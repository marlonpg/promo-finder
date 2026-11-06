package dev.gamba.promofinder.domain;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchRepository extends JpaRepository<Watch, Long> {

	List<Watch> findByDeletedAtIsNullOrderByCreatedAtDesc();

	List<Watch> findByActiveTrueAndDeletedAtIsNull();

	List<Watch> findByDeletedAtBefore(Instant cutoff);

	long countByDeletedAtIsNull();

	long countByActiveTrueAndDeletedAtIsNull();
}
