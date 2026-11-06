package dev.gamba.promofinder.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import dev.gamba.promofinder.domain.CheckRun;
import dev.gamba.promofinder.domain.CheckRunRepository;
import dev.gamba.promofinder.domain.Watch;
import dev.gamba.promofinder.monitor.CheckTracker;
import dev.gamba.promofinder.ui.WatchView.Status;

@Component
class WatchViews {

	private final CheckRunRepository runs;
	private final CheckTracker tracker;

	WatchViews(CheckRunRepository runs, CheckTracker tracker) {
		this.runs = runs;
		this.tracker = tracker;
	}

	List<WatchView> of(List<Watch> watches) {
		return watches.stream().map(this::of).toList();
	}

	WatchView of(Watch watch) {
		Instant since = tracker.runningSince(watch.getId());
		BigDecimal best = watch.getLastBestPrice();
		BigDecimal gap = best == null ? null : best.subtract(watch.getMaxPrice());
		int progress = best == null ? 0
				: best.compareTo(watch.getMaxPrice()) <= 0 ? 100
				: watch.getMaxPrice().multiply(BigDecimal.valueOf(100)).divide(best, 0, RoundingMode.DOWN).intValue();
		Chart spark = Chart.of(history(watch, Duration.ofDays(7)), watch.getMaxPrice(), 120, 32);
		return new WatchView(watch, status(watch, since), since, gap, progress, spark);
	}

	List<CheckRun> history(Watch watch, Duration period) {
		return runs.findByWatchIdAndStartedAtAfterAndErrorCodeIsNullAndBestPriceIsNotNullOrderByStartedAt(
				watch.getId(), Instant.now().minus(period));
	}

	private Status status(Watch watch, Instant runningSince) {
		if (runningSince != null) {
			return Status.SEARCHING;
		}
		if (tracker.isQueued(watch.getId())) {
			return Status.QUEUED;
		}
		if (!watch.isActive()) {
			return Status.PAUSED;
		}
		if (watch.getLastErrorCode() != null) {
			return Status.ERROR;
		}
		if (watch.isDeal()) {
			return Status.DEAL;
		}
		return watch.getLastCheckedAt() == null ? Status.NEW : Status.WATCHING;
	}
}
