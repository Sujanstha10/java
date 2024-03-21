package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.M1VCalculation;

public interface M1VCalculationRepository extends JpaRepository<M1VCalculation, Date>{

	@Query(value = "SELECT EXISTS (SELECT 1 FROM m1vcalculation)", nativeQuery = true)
    Integer findAny();

	Optional<List<M1VCalculation>> findByRocAnnRollAvgFlagIsFalseOrderByToDate();

	Optional<List<M1VCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	Optional<List<M1VCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	List<M1VCalculation> findAllByRocIsNotNull();
	
	List<M1VCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	M1VCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
