package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.PCEDGCalculation;

public interface PCEDGCalculationRepository extends JpaRepository<PCEDGCalculation, Date>{

	@Query(value = "SELECT EXISTS (SELECT 1 FROM pcedgcalculation)", nativeQuery = true)
	Integer findAny();

	Optional<List<PCEDGCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<PCEDGCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	List<PCEDGCalculation> findAllByRocIsNotNull();

	List<PCEDGCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	PCEDGCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
	
}
