package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.PSAVERTCalculation;

public interface PSAVERTCalculationRepository extends JpaRepository<PSAVERTCalculation, Date>{

	@Query(value = "SELECT EXISTS (SELECT 1 FROM psavertcalculation)", nativeQuery = true)
	Integer findAny();

	Optional<List<PSAVERTCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<PSAVERTCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	List<PSAVERTCalculation> findAllByRocIsNotNull();

	List<PSAVERTCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	PSAVERTCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
}
