package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.M1Calculation;

public interface M1CalculationRepository extends JpaRepository<M1Calculation, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM m1calculation)", nativeQuery = true)
    Integer findAny();

	Optional<List<M1Calculation>> findByRocAnnRollAvgFlagIsFalseOrderByToDate();

	Optional<List<M1Calculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	Optional<List<M1Calculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	List<M1Calculation> findAllByRocIsNotNull();
	
	List<M1Calculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	M1Calculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
}
