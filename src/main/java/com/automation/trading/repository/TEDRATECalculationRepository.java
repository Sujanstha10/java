package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.TEDRATECalculation;

public interface TEDRATECalculationRepository extends JpaRepository<TEDRATECalculation, Date> {

	List<TEDRATECalculation> findAllByRocIsNotNull();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM tedratecalculation)", nativeQuery = true)
	Integer findAny();

	Optional<List<TEDRATECalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<TEDRATECalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	List<TEDRATECalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	TEDRATECalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
	

}
