package dev.gamba.promofinder.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface PricePointRepository extends JpaRepository<PricePoint, Long> {

	List<PricePoint> findByCheckRunIdOrderByPrice(Long checkRunId);

	@Transactional
	void deleteByWatchId(Long watchId);
}
