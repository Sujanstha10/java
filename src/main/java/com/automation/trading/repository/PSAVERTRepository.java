package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.PSAVERT;

public interface PSAVERTRepository extends JpaRepository<PSAVERT, Date> {
	
	Optional<List<PSAVERT>> findByRocFlagIsFalseOrderByDate();

	Optional<PSAVERT> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<PSAVERT>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<PSAVERT>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<PSAVERT> findTopByOrderByDateDesc();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM psavert)", nativeQuery = true)
    Integer findAny();

}
