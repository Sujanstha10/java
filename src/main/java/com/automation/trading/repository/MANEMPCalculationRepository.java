package com.automation.trading.repository;

import com.automation.trading.domain.calculation.MANEMPCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface MANEMPCalculationRepository extends JpaRepository<MANEMPCalculation, Date> {

    @Override
    <S extends MANEMPCalculation> List<S> saveAll(Iterable<S> iterable);

    Optional<List<MANEMPCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

    Optional<List<MANEMPCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM manempcalculation)", nativeQuery = true)
    Integer findAny();

    List<MANEMPCalculation> findAllByRocIsNotNull();

    List<MANEMPCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();
    MANEMPCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();
}
