package dev.gamba.promofinder.ai;

import java.math.BigDecimal;

/** One offer as reported by Claude. */
public record FoundOffer(String store, String title, BigDecimal price, String currency, String url,
		Boolean inStock, String condition) {
}
