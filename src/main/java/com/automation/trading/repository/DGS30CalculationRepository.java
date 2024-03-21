package com.automation.trading.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.automation.trading.domain.calculation.DGS10Calculation;
import com.automation.trading.domain.calculation.DGS30Calculation;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface DGS30CalculationRepository extends JpaRepository<DGS30Calculation, Date>{

    @Override
    <S extends DGS30Calculation> List<S> saveAll(Iterable<S> iterable);

    Optional<List<DGS30Calculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

    Optional<List<DGS30Calculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM dgs30calculation)", nativeQuery = true)
    Integer findAny();

    List<DGS30Calculation> findAllByRocIsNotNull();
    
    List<DGS30Calculation> findAllByRocIsNotNullAndRocChangeSignIsNull();
    DGS30Calculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
}
