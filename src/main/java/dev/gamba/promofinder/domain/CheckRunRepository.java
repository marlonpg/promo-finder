package dev.gamba.promofinder.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface CheckRunRepository extends JpaRepository<CheckRun, Long> {

	List<CheckRun> findTop20ByWatchIdOrderByStartedAtDesc(Long watchId);

	/** Successful runs with a price, oldest first: the price history. */
	List<CheckRun> findByWatchIdAndStartedAtAfterAndErrorCodeIsNullAndBestPriceIsNotNullOrderByStartedAt(
			Long watchId, Instant since);

	Optional<CheckRun> findFirstByWatchIdAndErrorCodeIsNullOrderByStartedAtDesc(Long watchId);

	@Query("select coalesce(sum(r.costUsd), 0) from CheckRun r where r.startedAt > ?1")
	double sumCostSince(Instant since);

	@Query("select max(r.finishedAt) from CheckRun r")
	Instant lastFinishedAt();

	@Transactional
	void deleteByWatchId(Long watchId);
}
