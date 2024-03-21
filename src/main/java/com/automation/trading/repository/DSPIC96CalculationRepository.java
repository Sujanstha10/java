package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.DSPIC96Calculation;

public interface DSPIC96CalculationRepository extends JpaRepository<DSPIC96Calculation, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM dspic96calculation)", nativeQuery = true)
	Integer findAny();

	Optional<List<DSPIC96Calculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<DSPIC96Calculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	List<DSPIC96Calculation> findAllByRocIsNotNull();

	List<DSPIC96Calculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	DSPIC96Calculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
