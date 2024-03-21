package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.DPRIMECalculation;

public interface DPRIMECalculationRepository extends JpaRepository<DPRIMECalculation, Date> {

	List<DPRIMECalculation> findAllByRocIsNotNull();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM dprimecalculation)", nativeQuery = true)
	Integer findAny();

	Optional<List<DPRIMECalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<DPRIMECalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	List<DPRIMECalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();
    DPRIMECalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
}
