package dev.gamba.promofinder.monitor;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import dev.gamba.promofinder.ai.CheckError;
import dev.gamba.promofinder.ai.ClaudeCliClient;
import dev.gamba.promofinder.ai.ClaudeException;
import dev.gamba.promofinder.ai.FoundOffer;
import dev.gamba.promofinder.ai.SearchResult;
import dev.gamba.promofinder.domain.CheckRun;
import dev.gamba.promofinder.domain.CheckRunRepository;
import dev.gamba.promofinder.domain.Offer;
import dev.gamba.promofinder.domain.OfferRepository;
import dev.gamba.promofinder.domain.PricePoint;
import dev.gamba.promofinder.domain.PricePointRepository;
import dev.gamba.promofinder.domain.Store;
import dev.gamba.promofinder.domain.StoreRepository;
import dev.gamba.promofinder.domain.Watch;
import dev.gamba.promofinder.domain.WatchRepository;
import dev.gamba.promofinder.notify.NotificationService;

/** Runs one search for a watch, records prices and offers, and notifies the user about new deals. */
@Service
public class WatchChecker {

	private static final Logger log = LoggerFactory.getLogger(WatchChecker.class);

	/** Only one Claude process at a time, whether started by the scheduler or "check now". */
	private final Object lock = new Object();

	private final ClaudeCliClient claude;
	private final StoreRepository stores;
	private final WatchRepository watches;
	private final OfferRepository offers;
	private final CheckRunRepository runs;
	private final PricePointRepository pricePoints;
	private final NotificationService notifications;
	private final CheckTracker tracker;
	private final ApplicationEventPublisher events;

	public WatchChecker(ClaudeCliClient claude, StoreRepository stores, WatchRepository watches,
			OfferRepository offers, CheckRunRepository runs, PricePointRepository pricePoints,
			NotificationService notifications, CheckTracker tracker, ApplicationEventPublisher events) {
		this.claude = claude;
		this.stores = stores;
		this.watches = watches;
		this.offers = offers;
		this.runs = runs;
		this.pricePoints = pricePoints;
		this.notifications = notifications;
		this.tracker = tracker;
		this.events = events;
	}

	/** Queues a check in the background. Returns false if one is already queued or running. */
	public boolean requestCheck(Long watchId) {
		if (!tracker.queue(watchId)) {
			return false;
		}
		events.publishEvent(new WatchUpdated(watchId));
		Thread.ofVirtual().name("check-" + watchId).start(() -> runQueued(watchId));
		return true;
	}

	/** Checks now on the calling thread, unless a check for this watch is already queued or running. */
	void checkIfIdle(Long watchId) {
		if (tracker.queue(watchId)) {
			runQueued(watchId);
		}
	}

	private void runQueued(Long watchId) {
		try {
			synchronized (lock) {
				watches.findById(watchId).filter(w -> w.getDeletedAt() == null).ifPresent(this::check);
			}
		}
		catch (RuntimeException e) {
			log.error("Check crashed for watch {}", watchId, e);
		}
		finally {
			tracker.finish(watchId);
			events.publishEvent(new WatchUpdated(watchId));
		}
	}

	private void check(Watch watch) {
		tracker.start(watch.getId());
		events.publishEvent(new WatchUpdated(watch.getId()));
		log.info("Checking '{}' (max R$ {})", watch.getProduct(), watch.getMaxPrice());

		CheckRun run = runs.save(new CheckRun(watch.getId(), Instant.now()));
		List<Store> enabledStores = stores.findByEnabledTrueAndDeletedAtIsNull();
		FoundOffer best = null;
		try {
			SearchResult result = claude.search(watch, enabledStores);
			List<FoundOffer> valid = result.offers().stream().filter(o -> isValid(o, enabledStores)).toList();
			List<FoundOffer> matches = valid.stream().filter(o -> o.price().compareTo(watch.getMaxPrice()) <= 0).toList();
			best = valid.stream().min(Comparator.comparing(FoundOffer::price)).orElse(null);
			savePricePoints(watch, run, valid);
			int alerts = record(watch, matches);
			run.setMatches(matches.size());
			run.setCostUsd(result.costUsd());
			run.setNote(truncate(result.notes()));
			if (best != null) {
				run.setBestPrice(best.price());
				run.setBestStore(best.store());
			}
			log.info("'{}': best {}, {} match(es), {} alert(s), cost ${}", watch.getProduct(),
					best == null ? "none" : best.price(), matches.size(), alerts, result.costUsd());
		}
		catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			CheckError error = e instanceof ClaudeException ce ? ce.error() : CheckError.UNKNOWN;
			log.error("Check failed for '{}' ({})", watch.getProduct(), error, e);
			run.setErrorCode(error.name());
			// A failed search still used Claude; keep its cost so the usage totals stay honest.
			if (e instanceof ClaudeException ce) {
				run.setCostUsd(ce.costUsd());
			}
			run.setNote(truncate(e.getMessage()));
		}
		run.setFinishedAt(Instant.now());
		runs.save(run);

		// Reload: the user may have edited the watch while Claude was searching.
		FoundOffer bestOffer = best;
		watches.findById(watch.getId()).ifPresent(fresh -> {
			fresh.setLastCheckedAt(run.getFinishedAt());
			fresh.setLastErrorCode(run.getErrorCode());
			fresh.setLastStatus(run.getNote());
			if (run.isSuccess()) {
				fresh.setLastBestPrice(bestOffer == null ? null : bestOffer.price());
				fresh.setLastBestStore(bestOffer == null ? null : bestOffer.store());
				fresh.setLastBestUrl(bestOffer == null ? null : bestOffer.url());
			}
			watches.save(fresh);
		});
	}

	/** Keeps the cheapest offer per store. */
	private void savePricePoints(Watch watch, CheckRun run, List<FoundOffer> valid) {
		Map<String, FoundOffer> bestPerStore = valid.stream()
				.sorted(Comparator.comparing(FoundOffer::price))
				.collect(Collectors.toMap(o -> o.store() == null ? hostOf(o.url()) : o.store(), o -> o,
						(a, b) -> a, LinkedHashMap::new));
		pricePoints.saveAll(bestPerStore.entrySet().stream()
				.map(e -> new PricePoint(watch.getId(), run.getId(), e.getKey(), e.getValue().title(),
						e.getValue().price(), e.getValue().url(), run.getStartedAt()))
				.toList());
	}

	private int record(Watch watch, List<FoundOffer> matches) {
		List<Offer> fresh = matches.stream()
				.filter(o -> !offers.existsByWatchIdAndUrlAndNotifiedTrueAndPriceLessThanEqual(
						watch.getId(), o.url(), o.price()))
				.map(o -> offers.save(new Offer(watch.getId(), o.store(), o.title(), o.price(), o.url(), Instant.now())))
				.toList();
		if (fresh.isEmpty()) {
			return 0;
		}
		BigDecimal best = fresh.stream().map(Offer::getPrice).min(BigDecimal::compareTo).orElseThrow();
		String subject = "Promo found: %s for R$ %s".formatted(watch.getProduct(), best.toPlainString());
		String body = fresh.stream()
				.map(o -> "%s - R$ %s%n%s%n%s".formatted(o.getStore(), o.getPrice().toPlainString(), o.getTitle(), o.getUrl()))
				.collect(Collectors.joining("\n\n"));
		// Only mark as notified when it reached the user, so a failed send is retried next check.
		if (watch.getChannels().isEmpty() || !notifications.send(watch.getChannels(), subject, body).isEmpty()) {
			fresh.forEach(o -> o.setNotified(true));
			offers.saveAll(fresh);
		}
		return fresh.size();
	}

	private static String truncate(String text) {
		return text == null || text.length() <= 2000 ? text : text.substring(0, 2000);
	}

	/** An offer at or below the target that passes {@link #isValid}. */
	static boolean isMatch(FoundOffer offer, BigDecimal maxPrice, List<Store> allowedStores) {
		return isValid(offer, allowedStores) && offer.price().compareTo(maxPrice) <= 0;
	}

	/** Don't trust the model blindly: re-check price, stock, currency and store domain in code. */
	static boolean isValid(FoundOffer offer, List<Store> allowedStores) {
		if (offer.price() == null || offer.url() == null || offer.price().signum() <= 0
				|| Boolean.FALSE.equals(offer.inStock())) {
			return false;
		}
		if (offer.currency() != null && !offer.currency().isBlank() && !"BRL".equalsIgnoreCase(offer.currency())) {
			return false;
		}
		String host = hostOf(offer.url());
		if (host == null) {
			return false;
		}
		return allowedStores.isEmpty() || allowedStores.stream()
				.map(s -> normalizeDomain(s.getDomain()))
				.anyMatch(d -> host.equals(d) || host.endsWith("." + d));
	}

	private static String hostOf(String url) {
		try {
			URI uri = URI.create(url.trim());
			if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
				return null;
			}
			return uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static String normalizeDomain(String domain) {
		return domain.trim().toLowerCase(Locale.ROOT)
				.replaceFirst("^https?://", "")
				.replaceFirst("^www\\.", "")
				.replaceFirst("/.*$", "");
	}
}
