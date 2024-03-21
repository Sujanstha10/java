package com.automation.trading.repository;

import com.automation.trading.domain.calculation.CPIAUCSLCalculation;
import com.automation.trading.domain.calculation.DffCalculation;
import com.automation.trading.domain.calculation.GdpCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface DffCalculationRepository extends JpaRepository<DffCalculation, Date> {
    @Override
    <S extends DffCalculation> List<S> saveAll(Iterable<S> iterable);

    Optional<List<DffCalculation>> findByRocAnnRollAvgFlagIsFalseOrderByToDate();
    Optional<List<DffCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

    Optional<List<DffCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM dff_calculation)", nativeQuery = true)
    Integer findAny();


    List<DffCalculation> findAllByRocIsNotNull();
    
    List<DffCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();
    DffCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
}
