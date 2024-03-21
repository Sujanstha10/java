package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.CIVPARTCalculation;
import com.automation.trading.domain.calculation.ICSACalculation;

public interface ICSACalculationRepository extends JpaRepository<ICSACalculation, Date> {

	List<ICSACalculation> findAllByRocIsNotNull();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM icsacalculation)", nativeQuery = true)
    Integer findAny();

	Optional<List<ICSACalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<ICSACalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	List<ICSACalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();
	ICSACalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
