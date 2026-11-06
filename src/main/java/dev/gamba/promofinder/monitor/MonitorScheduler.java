package dev.gamba.promofinder.monitor;

import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.gamba.promofinder.domain.CheckRunRepository;
import dev.gamba.promofinder.domain.OfferRepository;
import dev.gamba.promofinder.domain.PricePointRepository;
import dev.gamba.promofinder.domain.StoreRepository;
import dev.gamba.promofinder.domain.Watch;
import dev.gamba.promofinder.domain.WatchRepository;

/** Every tick, runs the watches whose own interval has elapsed. Ticks never overlap. */
@Component
class MonitorScheduler {

	/** How long a deleted watch or store can still be restored with "Undo". */
	private static final Duration UNDO_WINDOW = Duration.ofMinutes(5);

	private final WatchRepository watches;
	private final StoreRepository stores;
	private final OfferRepository offers;
	private final CheckRunRepository runs;
	private final PricePointRepository pricePoints;
	private final WatchChecker checker;

	MonitorScheduler(WatchRepository watches, StoreRepository stores, OfferRepository offers,
			CheckRunRepository runs, PricePointRepository pricePoints, WatchChecker checker) {
		this.watches = watches;
		this.stores = stores;
		this.offers = offers;
		this.runs = runs;
		this.pricePoints = pricePoints;
		this.checker = checker;
	}

	@Scheduled(fixedDelayString = "${promo.scheduler.tick}", initialDelayString = "PT10S")
	void tick() {
		purgeDeleted();
		Instant now = Instant.now();
		watches.findByActiveTrueAndDeletedAtIsNull().stream()
				.filter(w -> w.isDue(now))
				.forEach(w -> checker.checkIfIdle(w.getId()));
	}

	private void purgeDeleted() {
		Instant cutoff = Instant.now().minus(UNDO_WINDOW);
		for (Watch watch : watches.findByDeletedAtBefore(cutoff)) {
			offers.deleteByWatchId(watch.getId());
			pricePoints.deleteByWatchId(watch.getId());
			runs.deleteByWatchId(watch.getId());
			watches.delete(watch);
		}
		stores.deleteByDeletedAtBefore(cutoff);
	}
}
