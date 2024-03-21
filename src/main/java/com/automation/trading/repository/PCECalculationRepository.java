package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.PCECalculation;

public interface PCECalculationRepository extends JpaRepository<PCECalculation, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM pcecalculation)", nativeQuery = true)
	Integer findAny();

	Optional<List<PCECalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<PCECalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	List<PCECalculation> findAllByRocIsNotNull();

	List<PCECalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	PCECalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
}
