package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.IC4WSACalculation;

public interface IC4WSACalculationRepository extends JpaRepository<IC4WSACalculation, Date>{
	
	List<IC4WSACalculation> findAllByRocIsNotNull();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM ic4wsacalculation)", nativeQuery = true)
    Integer findAny();

	Optional<List<IC4WSACalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<IC4WSACalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	List<IC4WSACalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();
	IC4WSACalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
