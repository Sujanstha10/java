package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.GDPDEFCalculation;
import com.automation.trading.domain.calculation.GdpCalculation;

public interface GDPDEFCalculationRepository extends JpaRepository<GDPDEFCalculation, Date>{

	Optional<List<GDPDEFCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	Optional<List<GDPDEFCalculation>> findByRocAnnRollAvgFlagIsFalseOrderByToDate();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM gdpdefcalculation)", nativeQuery = true)
    Integer findAny();

	Optional<List<GDPDEFCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	List<GDPDEFCalculation> findAllByRocIsNotNull();
	
	List<GDPDEFCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	GDPDEFCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
