package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.calculation.DGS5Calculation;

public interface DGS5CalculationRepository extends JpaRepository<DGS5Calculation, Date>{

    @Override
    <S extends DGS5Calculation> List<S> saveAll(Iterable<S> iterable);

    Optional<List<DGS5Calculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

    Optional<List<DGS5Calculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM dgs5calculation)", nativeQuery = true)
    Integer findAny();

    List<DGS5Calculation> findAllByRocIsNotNull();

    List<DGS5Calculation> findAllByRocIsNotNullAndRocChangeSignIsNull();
    DGS5Calculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
}
