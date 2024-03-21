package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.T10YIECalculation;

public interface T10YIECalculationRepository extends JpaRepository<T10YIECalculation, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM t10yiecalculation)", nativeQuery = true)
	Integer findAny();

	Optional<List<T10YIECalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<T10YIECalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	List<T10YIECalculation> findAllByRocIsNotNull();

	List<T10YIECalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	T10YIECalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
