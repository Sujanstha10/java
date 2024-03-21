package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.M2VCalculation;
import com.automation.trading.domain.calculation.NROUCalculation;

public interface NROUCalculationRepository extends JpaRepository<NROUCalculation, Date> {

	List<NROUCalculation> findAllByRocIsNotNull();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM nroucalculation)", nativeQuery = true)
    Integer findAny();

	Optional<List<NROUCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<NROUCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
	List<NROUCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	NROUCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
	
	
	
}
