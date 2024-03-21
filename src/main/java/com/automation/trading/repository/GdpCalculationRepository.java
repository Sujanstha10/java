package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.GDPC1Calculation;
import com.automation.trading.domain.calculation.GdpCalculation;

public interface GdpCalculationRepository extends JpaRepository<GdpCalculation, Date> {

    @Override
    <S extends GdpCalculation> List<S> saveAll(Iterable<S> iterable);

    Optional<List<GdpCalculation>> findByRocAnnRollAvgFlagIsFalseOrderByToDate();
    Optional<List<GdpCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

    Optional<List<GdpCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM gdp_calculation)", nativeQuery = true)
    Integer findAny();

    List<GdpCalculation> findAllByRocIsNotNull();
    
    List<GdpCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();

    GdpCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
}
