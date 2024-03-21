package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.GDPPOTCalculation;

public interface GDPPOTCalculationRepository extends JpaRepository<GDPPOTCalculation, Date>{

	@Query(value = "SELECT EXISTS (SELECT 1 FROM gdppotcalculation)", nativeQuery = true)
    Integer findAny();

	Optional<List<GDPPOTCalculation>> findByRocAnnRollAvgFlagIsFalseOrderByToDate();

	Optional<List<GDPPOTCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	Optional<List<GDPPOTCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	List<GDPPOTCalculation> findAllByRocIsNotNull();
	
	List<GDPPOTCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	GDPPOTCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
}
