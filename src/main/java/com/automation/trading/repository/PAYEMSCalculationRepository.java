package com.automation.trading.repository;

import com.automation.trading.domain.calculation.PAYEMSCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PAYEMSCalculationRepository extends JpaRepository<PAYEMSCalculation, Date> {
    @Override
    <S extends PAYEMSCalculation> List<S> saveAll(Iterable<S> iterable);

    Optional<List<PAYEMSCalculation>> findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

    Optional<List<PAYEMSCalculation>> findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM payemscalculation)", nativeQuery = true)
    Integer findAny();

    List<PAYEMSCalculation> findAllByRocIsNotNull();

    List<PAYEMSCalculation> findAllByRocIsNotNullAndRocChangeSignIsNull();
    PAYEMSCalculation findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

}
