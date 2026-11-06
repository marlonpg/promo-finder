package dev.gamba.promofinder.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/** Formatting helpers for templates: {@code ${@fmt.brl(price)}}. */
@Component("fmt")
public class Fmt {

	private static final Locale BRAZIL = Locale.of("pt", "BR");

	/** Reais are always shown the Brazilian way, whatever the UI language: R$ 4.500,00. */
	public String brl(BigDecimal value) {
		return value == null ? "—" : NumberFormat.getCurrencyInstance(BRAZIL).format(value).replace(' ', ' ');
	}

	/** Plain value for the price input, e.g. "4.500,00". */
	public String brlInput(BigDecimal value) {
		if (value == null) {
			return "";
		}
		var format = NumberFormat.getNumberInstance(BRAZIL);
		format.setMinimumFractionDigits(2);
		format.setMaximumFractionDigits(2);
		return format.format(value);
	}

	public String usd(Double value) {
		return value == null ? "—" : String.format(LocaleContextHolder.getLocale(), "US$ %.2f", value);
	}

	public String iso(Instant instant) {
		return instant == null ? "" : instant.toString();
	}

	public String dateTime(Instant instant) {
		return instant == null ? "—" : DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
				.withLocale(LocaleContextHolder.getLocale())
				.withZone(ZoneId.systemDefault())
				.format(instant);
	}

	public String duration(Instant from, Instant to) {
		if (from == null || to == null) {
			return "—";
		}
		long seconds = Duration.between(from, to).toSeconds();
		return seconds < 60 ? seconds + "s" : "%dm %02ds".formatted(seconds / 60, seconds % 60);
	}

	/** Interval in minutes as "30 min", "6 h", "1 d". */
	public String interval(int minutes) {
		if (minutes % 1440 == 0) {
			return minutes / 1440 + " d";
		}
		return minutes % 60 == 0 ? minutes / 60 + " h" : minutes + " min";
	}
}
