package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.EMRATIOCalculation;
import com.automation.trading.domain.calculation.GDPC1Calculation;

public interface GDPC1CalculationRepository extends JpaRepository<GDPC1Calculation, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM gdpc1calculation)", nativeQuery = true)
	Integer findAny();

	Optional<List<GDPC1Calculation>> findByRocAnnRollAvgFlagIsFalseOrderByToDate();

	Optional<List<GDPC1Calculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	Optional<List<GDPC1Calculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	List<GDPC1Calculation> findAllByRocIsNotNull();

	List<GDPC1Calculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	GDPC1Calculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
