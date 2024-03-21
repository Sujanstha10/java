package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.M2VCalculation;

public interface M2VCalculationRepository extends JpaRepository<M2VCalculation, Date>{

	@Query(value = "SELECT EXISTS (SELECT 1 FROM m2vcalculation)", nativeQuery = true)
    Integer findAny();

	Optional<List<M2VCalculation>> findByRocAnnRollAvgFlagIsFalseOrderByToDate();

	Optional<List<M2VCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	Optional<List<M2VCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	List<M2VCalculation> findAllByRocIsNotNull();
	
	List<M2VCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	M2VCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
