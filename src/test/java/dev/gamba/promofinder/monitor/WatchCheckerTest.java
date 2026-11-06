package dev.gamba.promofinder.monitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.gamba.promofinder.ai.FoundOffer;
import dev.gamba.promofinder.domain.Store;

class WatchCheckerTest {

	private static final BigDecimal MAX = new BigDecimal("4500.00");

	private static Store store(String domain) {
		var store = new Store();
		store.setName(domain);
		store.setDomain(domain);
		return store;
	}

	private static FoundOffer offer(String price, String url) {
		return new FoundOffer("Shop", "Galaxy S26 Ultra", new BigDecimal(price), "BRL", url, true, "new");
	}

	@Test
	void acceptsOfferAtOrBelowTargetOnAllowedDomain() {
		var stores = List.of(store("amazon.com.br"));
		assertThat(WatchChecker.isMatch(offer("4500.00", "https://www.amazon.com.br/dp/X"), MAX, stores)).isTrue();
		assertThat(WatchChecker.isMatch(offer("3999.90", "https://amazon.com.br/dp/X"), MAX, stores)).isTrue();
	}

	@Test
	void rejectsOfferAboveTarget() {
		assertThat(WatchChecker.isMatch(offer("4500.01", "https://amazon.com.br/x"), MAX, List.of())).isFalse();
	}

	@Test
	void rejectsOfferFromOtherDomainOrLookalike() {
		var stores = List.of(store("amazon.com.br"));
		assertThat(WatchChecker.isMatch(offer("100", "https://mercadolivre.com.br/x"), MAX, stores)).isFalse();
		assertThat(WatchChecker.isMatch(offer("100", "https://amazon.com.br.evil.com/x"), MAX, stores)).isFalse();
		assertThat(WatchChecker.isMatch(offer("100", "javascript:alert(1)"), MAX, stores)).isFalse();
	}

	@Test
	void rejectsOutOfStockAndForeignCurrency() {
		var outOfStock = new FoundOffer("Shop", "t", new BigDecimal("100"), "BRL", "https://a.com", false, "new");
		var usd = new FoundOffer("Shop", "t", new BigDecimal("100"), "USD", "https://a.com", true, "new");
		assertThat(WatchChecker.isMatch(outOfStock, MAX, List.of())).isFalse();
		assertThat(WatchChecker.isMatch(usd, MAX, List.of())).isFalse();
	}

	@Test
	void normalizesDomains() {
		assertThat(WatchChecker.normalizeDomain(" https://www.Kabum.com.br/celulares ")).isEqualTo("kabum.com.br");
	}
}
