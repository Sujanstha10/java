package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.M2Calculation;

public interface M2CalculationRepository extends JpaRepository<M2Calculation, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM m2calculation)", nativeQuery = true)
	Integer findAny();

	Optional<List<M2Calculation>> findByRocAnnRollAvgFlagIsFalseOrderByToDate();

	Optional<List<M2Calculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	Optional<List<M2Calculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	List<M2Calculation> findAllByRocIsNotNull();
	
	List<M2Calculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	M2Calculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
