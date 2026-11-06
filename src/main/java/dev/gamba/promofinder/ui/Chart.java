package dev.gamba.promofinder.ui;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.gamba.promofinder.domain.CheckRun;

/**
 * Price-over-time chart, laid out in Java and drawn as inline SVG by the templates.
 * Coordinates are in a {@code width x height} viewBox; the Y range always includes the target.
 */
public record Chart(double width, double height, String line, String area, double targetY, List<Dot> dots,
		BigDecimal min, BigDecimal max, Instant from, Instant to) {

	public record Dot(double x, double y, BigDecimal price, Instant at, String store) {
	}

	/** Null when there are fewer than two points to draw. */
	public static Chart of(List<CheckRun> runs, BigDecimal target, double width, double height) {
		if (runs.size() < 2) {
			return null;
		}
		double pad = height * 0.12;
		BigDecimal min = Stream.concat(runs.stream().map(CheckRun::getBestPrice), Stream.of(target))
				.min(BigDecimal::compareTo).orElseThrow();
		BigDecimal max = Stream.concat(runs.stream().map(CheckRun::getBestPrice), Stream.of(target))
				.max(BigDecimal::compareTo).orElseThrow();
		double lo = min.doubleValue();
		double span = Math.max(max.doubleValue() - lo, 1);
		Instant from = runs.getFirst().getStartedAt();
		Instant to = runs.getLast().getStartedAt();
		double timeSpan = Math.max(to.toEpochMilli() - from.toEpochMilli(), 1);

		List<Dot> dots = runs.stream().map(r -> new Dot(
				round((r.getStartedAt().toEpochMilli() - from.toEpochMilli()) / timeSpan * width),
				round(pad + (1 - (r.getBestPrice().doubleValue() - lo) / span) * (height - 2 * pad)),
				r.getBestPrice(), r.getStartedAt(), r.getBestStore())).toList();
		String line = dots.stream().map(d -> d.x() + "," + d.y()).collect(Collectors.joining(" "));
		String area = "0," + height + " " + line + " " + width + "," + height;
		double targetY = round(pad + (1 - (target.doubleValue() - lo) / span) * (height - 2 * pad));
		return new Chart(width, height, line, area, targetY, dots, min, max, from, to);
	}

	private static double round(double v) {
		return Math.round(v * 10) / 10.0;
	}
}
