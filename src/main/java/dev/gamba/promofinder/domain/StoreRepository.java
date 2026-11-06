package dev.gamba.promofinder.domain;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface StoreRepository extends JpaRepository<Store, Long> {

	List<Store> findByDeletedAtIsNullOrderByName();

	List<Store> findByEnabledTrueAndDeletedAtIsNull();

	long countByDeletedAtIsNull();

	@Transactional
	void deleteByDeletedAtBefore(Instant cutoff);
}
