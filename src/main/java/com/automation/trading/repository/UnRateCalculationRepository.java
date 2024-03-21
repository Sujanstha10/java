package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.UnRateCalculation;

public interface UnRateCalculationRepository extends JpaRepository<UnRateCalculation, Date> {
	@Override
	<S extends UnRateCalculation> List<S> saveAll(Iterable<S> iterable);

	Optional<List<UnRateCalculation>> findByRocAnnRollAvgFlagIsFalseOrderByToDate();

	Optional<List<UnRateCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<UnRateCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM un_rate_calculation)", nativeQuery = true)
	Integer findAny();

	List<UnRateCalculation> findAllByRocIsNotNull();

	List<UnRateCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

	UnRateCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
}
