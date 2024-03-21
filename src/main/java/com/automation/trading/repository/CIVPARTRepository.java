package com.automation.trading.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.CIVPART;

public interface CIVPARTRepository extends JpaRepository<CIVPART, Long> {

	Optional<List<CIVPART>> findByRocFlagIsFalseOrderByDate();

	Optional<CIVPART> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<CIVPART> findTopByOrderByDateDesc();

	Optional<List<CIVPART>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<CIVPART>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM civpart)", nativeQuery = true)
	Integer findAny();

}
