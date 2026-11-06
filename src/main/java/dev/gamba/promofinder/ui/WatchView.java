package dev.gamba.promofinder.ui;

import java.math.BigDecimal;
import java.time.Instant;

import dev.gamba.promofinder.domain.Watch;

/** Everything a watch card needs, computed once in Java so the template stays simple. */
public record WatchView(Watch watch, Status status, Instant searchingSince, BigDecimal gap, int progress,
		Chart spark) {

	public enum Status {
		NEW, QUEUED, SEARCHING, WATCHING, DEAL, ERROR, PAUSED;

		public String css() {
			return name().toLowerCase();
		}
	}

	public boolean isBusy() {
		return status == Status.SEARCHING || status == Status.QUEUED;
	}

	/** Gap is best price minus target: positive means still above the target. */
	public boolean isAbove() {
		return gap != null && gap.signum() > 0;
	}

	public BigDecimal absGap() {
		return gap == null ? null : gap.abs();
	}
}
