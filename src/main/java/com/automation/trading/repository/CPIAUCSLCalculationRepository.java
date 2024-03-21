package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.CIVPARTCalculation;
import com.automation.trading.domain.calculation.CPIAUCSLCalculation;

public interface CPIAUCSLCalculationRepository extends JpaRepository<CPIAUCSLCalculation, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM cpiaucslcalculation)", nativeQuery = true)
    Integer findAny();

	Optional<List<CPIAUCSLCalculation>> findByRocAnnRollAvgFlagIsFalseOrderByToDate();

	Optional<List<CPIAUCSLCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	Optional<List<CPIAUCSLCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	@Override
	<S extends CPIAUCSLCalculation> List<S> saveAll(Iterable<S> iterable);

	List<CPIAUCSLCalculation> findAllByRocIsNotNull();
	
	List<CPIAUCSLCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();
	CPIAUCSLCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();	

}
