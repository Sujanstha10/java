package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.T5YIECalculation;

public interface T5YIECalculationRepository extends JpaRepository<T5YIECalculation, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM t5yiecalculation)", nativeQuery = true)
	Integer findAny();

	Optional<List<T5YIECalculation>> findByRocAnnRollAvgFlagIsFalseOrderByToDate();

	Optional<List<T5YIECalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	Optional<List<T5YIECalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	List<T5YIECalculation> findAllByRocIsNotNull();

	List<T5YIECalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	T5YIECalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
