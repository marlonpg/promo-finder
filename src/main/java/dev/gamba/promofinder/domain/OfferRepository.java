package dev.gamba.promofinder.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

	List<Offer> findTop50ByWatchIdOrderByFoundAtDesc(Long watchId);

	/** Already told the user about this URL at this price or cheaper? */
	boolean existsByWatchIdAndUrlAndNotifiedTrueAndPriceLessThanEqual(Long watchId, String url, BigDecimal price);

	long countByFoundAtAfter(Instant since);

	@Transactional
	void deleteByWatchId(Long watchId);
}
