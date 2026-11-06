package dev.gamba.promofinder.ui;

import java.time.Duration;
import java.time.Instant;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import dev.gamba.promofinder.domain.CheckRunRepository;
import dev.gamba.promofinder.domain.OfferRepository;
import dev.gamba.promofinder.domain.StoreRepository;
import dev.gamba.promofinder.domain.WatchRepository;
import dev.gamba.promofinder.notify.NotificationService;

/** Shared helpers for the UI controllers: messages, toasts, htmx triggers and the dashboard summary. */
@Component
public class Ui {

	static final String TOAST_VIEW = "fragments/common :: toast";
	static final String NOTHING_VIEW = "fragments/common :: nothing";

	public record Summary(long activeWatches, long totalWatches, long deals7d, Instant lastCheck, double cost7d,
			boolean hasStores, boolean hasChannel) {

		public boolean hasWatches() {
			return totalWatches > 0;
		}

		public boolean showChecklist() {
			return !(hasStores && hasWatches() && hasChannel);
		}

		public int stepsDone() {
			return (hasStores ? 1 : 0) + (hasWatches() ? 1 : 0) + (hasChannel ? 1 : 0);
		}
	}

	private final MessageSource messages;
	private final WatchRepository watches;
	private final StoreRepository stores;
	private final OfferRepository offers;
	private final CheckRunRepository runs;
	private final NotificationService notifications;

	Ui(MessageSource messages, WatchRepository watches, StoreRepository stores, OfferRepository offers,
			CheckRunRepository runs, NotificationService notifications) {
		this.messages = messages;
		this.watches = watches;
		this.stores = stores;
		this.offers = offers;
		this.runs = runs;
		this.notifications = notifications;
	}

	String msg(String key, Object... args) {
		return messages.getMessage(key, args, LocaleContextHolder.getLocale());
	}

	/** Renders only the toast (out-of-band), optionally firing client-side events like "watchesChanged". */
	String toast(Model model, HttpServletResponse response, Toast toast, String... events) {
		model.addAttribute("toast", toast);
		trigger(response, events);
		return TOAST_VIEW;
	}

	static void trigger(HttpServletResponse response, String... events) {
		if (events.length > 0) {
			response.setHeader("HX-Trigger", String.join(", ", events));
		}
	}

	Summary summary() {
		Instant weekAgo = Instant.now().minus(Duration.ofDays(7));
		return new Summary(watches.countByActiveTrueAndDeletedAtIsNull(), watches.countByDeletedAtIsNull(),
				offers.countByFoundAtAfter(weekAgo), runs.lastFinishedAt(), runs.sumCostSince(weekAgo),
				stores.countByDeletedAtIsNull() > 0, notifications.anyConfigured());
	}
}
