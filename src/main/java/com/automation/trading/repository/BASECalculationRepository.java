package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.BASECalculation;

public interface BASECalculationRepository extends JpaRepository<BASECalculation, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM basecalculation)", nativeQuery = true)
	Integer findAny();

	@Override
	<S extends BASECalculation> List<S> saveAll(Iterable<S> iterable);

	Optional<List<BASECalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

	Optional<List<BASECalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();


	List<BASECalculation> findAllByRocIsNotNull();
	
	List<BASECalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();
    BASECalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
