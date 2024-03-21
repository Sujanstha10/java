package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.CIVPARTCalculation;
import com.automation.trading.domain.calculation.DGS5Calculation;

public interface CIVPARTCalculationRepository extends JpaRepository<CIVPARTCalculation, Date>{

	List<CIVPARTCalculation> findAllByRocIsNotNull();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM civpartcalculation)", nativeQuery = true)
    Integer findAny();

	Optional<List<CIVPARTCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<CIVPARTCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	List<CIVPARTCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();
	CIVPARTCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
