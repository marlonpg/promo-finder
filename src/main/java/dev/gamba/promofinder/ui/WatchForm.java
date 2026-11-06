package dev.gamba.promofinder.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import dev.gamba.promofinder.domain.NotificationChannel;
import dev.gamba.promofinder.domain.Watch;

/** Backing object for the add/edit watch panel. */
public class WatchForm {

	static final int[] INTERVAL_PRESETS = { 30, 60, 180, 360, 720, 1440 };

	private String product;
	private String details;
	/** As typed, e.g. "R$ 4.500,00". */
	private String maxPrice;
	private Integer intervalMinutes = 60;
	private Set<NotificationChannel> channels = EnumSet.noneOf(NotificationChannel.class);
	private boolean checkNow;

	static WatchForm of(Watch watch, Fmt fmt) {
		var form = new WatchForm();
		form.product = watch.getProduct();
		form.details = watch.getDetails();
		form.maxPrice = fmt.brlInput(watch.getMaxPrice());
		form.intervalMinutes = watch.getIntervalMinutes();
		form.channels = watch.getChannels().isEmpty() ? EnumSet.noneOf(NotificationChannel.class)
				: EnumSet.copyOf(watch.getChannels());
		return form;
	}

	/** Field name to message key; empty when valid. */
	Map<String, String> validate() {
		var errors = new LinkedHashMap<String, String>();
		if (product == null || product.isBlank()) {
			errors.put("product", "form.error.product");
		}
		BigDecimal price = parsedPrice();
		if (price == null || price.signum() <= 0) {
			errors.put("maxPrice", "form.error.price");
		}
		if (intervalMinutes == null || intervalMinutes < 5) {
			errors.put("intervalMinutes", "form.error.interval");
		}
		return errors;
	}

	void applyTo(Watch watch) {
		watch.setProduct(product.trim());
		watch.setDetails(details == null || details.isBlank() ? null : details.trim());
		watch.setMaxPrice(parsedPrice());
		watch.setIntervalMinutes(intervalMinutes);
		watch.setChannels(channels.isEmpty() ? EnumSet.noneOf(NotificationChannel.class) : EnumSet.copyOf(channels));
	}

	/** Accepts "R$ 4.500,00", "4500,5", "4500.50" or "4500". */
	BigDecimal parsedPrice() {
		if (maxPrice == null) {
			return null;
		}
		String s = maxPrice.replaceAll("[^0-9.,]", "");
		if (s.isEmpty()) {
			return null;
		}
		if (s.contains(",")) {
			s = s.replace(".", "").replace(',', '.');
		}
		else if (s.indexOf('.') != s.lastIndexOf('.') || s.matches(".*\\.\\d{3}$")) {
			s = s.replace(".", "");
		}
		try {
			return new BigDecimal(s).setScale(2, RoundingMode.HALF_UP);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	/** Presets plus the current value if it is not one of them. */
	public int[] intervalOptions() {
		for (int preset : INTERVAL_PRESETS) {
			if (intervalMinutes != null && preset == intervalMinutes) {
				return INTERVAL_PRESETS;
			}
		}
		if (intervalMinutes == null) {
			return INTERVAL_PRESETS;
		}
		int[] options = Arrays.copyOf(INTERVAL_PRESETS, INTERVAL_PRESETS.length + 1);
		options[options.length - 1] = intervalMinutes;
		Arrays.sort(options);
		return options;
	}

	public String getProduct() { return product; }
	public void setProduct(String product) { this.product = product; }
	public String getDetails() { return details; }
	public void setDetails(String details) { this.details = details; }
	public String getMaxPrice() { return maxPrice; }
	public void setMaxPrice(String maxPrice) { this.maxPrice = maxPrice; }
	public Integer getIntervalMinutes() { return intervalMinutes; }
	public void setIntervalMinutes(Integer intervalMinutes) { this.intervalMinutes = intervalMinutes; }
	public Set<NotificationChannel> getChannels() { return channels; }
	public void setChannels(Set<NotificationChannel> channels) { this.channels = channels; }
	public boolean isCheckNow() { return checkNow; }
	public void setCheckNow(boolean checkNow) { this.checkNow = checkNow; }
}
