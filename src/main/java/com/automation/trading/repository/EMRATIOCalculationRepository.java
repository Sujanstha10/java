package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.DPRIMECalculation;
import com.automation.trading.domain.calculation.EMRATIOCalculation;

public interface EMRATIOCalculationRepository extends JpaRepository<EMRATIOCalculation, Date>{

	List<EMRATIOCalculation> findAllByRocIsNotNull();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM emratiocalculation)", nativeQuery = true)
	Integer findAny();

	Optional<List<EMRATIOCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<EMRATIOCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	List<EMRATIOCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();
    EMRATIOCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
}
