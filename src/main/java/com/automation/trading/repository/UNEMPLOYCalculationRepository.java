package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.UNEMPLOYCalculation;

public interface UNEMPLOYCalculationRepository extends JpaRepository<UNEMPLOYCalculation, Date> {

	List<UNEMPLOYCalculation> findAllByRocIsNotNull();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM unemploycalculation)", nativeQuery = true)
	Integer findAny();

	Optional<List<UNEMPLOYCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<UNEMPLOYCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
	
	List<UNEMPLOYCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	UNEMPLOYCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
