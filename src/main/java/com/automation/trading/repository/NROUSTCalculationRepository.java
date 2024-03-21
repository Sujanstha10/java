package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.NROUSTCalculation;

public interface NROUSTCalculationRepository extends JpaRepository<NROUSTCalculation, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM nroustcalculation)", nativeQuery = true)
	Integer findAny();

	Optional<List<NROUSTCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<NROUSTCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
	List<NROUSTCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	NROUSTCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
