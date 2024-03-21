package com.automation.trading.repository;

import com.automation.trading.domain.calculation.DGS10Calculation;
import com.automation.trading.domain.fred.interestrates.DGS10;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface DGS10CalculationRepository extends JpaRepository<DGS10Calculation, Date>{

    @Override
    <S extends DGS10Calculation> List<S> saveAll(Iterable<S> iterable);

    Optional<List<DGS10Calculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

    Optional<List<DGS10Calculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM dgs10calculation)", nativeQuery = true)
    Integer findAny();

    List<DGS10Calculation> findAllByRocIsNotNull();

    List<DGS10Calculation> findAllByRocIsNotNullAndRocChangeSignIsNull();
    DGS10Calculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
}
