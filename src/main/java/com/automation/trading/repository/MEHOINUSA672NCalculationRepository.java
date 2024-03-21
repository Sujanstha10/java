package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.MEHOINUSA672NCalculation;
import com.automation.trading.domain.calculation.NROUCalculation;

public interface MEHOINUSA672NCalculationRepository extends JpaRepository<MEHOINUSA672NCalculation, Date>{

	List<MEHOINUSA672NCalculation> findAllByRocIsNotNull();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM mehoinusa672ncalculation)", nativeQuery = true)
    Integer findAny();

	Optional<List<MEHOINUSA672NCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<MEHOINUSA672NCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
	List<MEHOINUSA672NCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	MEHOINUSA672NCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
}
