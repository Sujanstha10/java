package com.automation.trading.repository;

import com.automation.trading.domain.fred.DFF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.interestrates.DGS10;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface DGS10Repository extends JpaRepository<DGS10, Date> {

    @Query(value = "SELECT EXISTS (SELECT 1 FROM dgs10)", nativeQuery = true)
    Integer findAny();
//
//    @Override
//    Optional<DGS10> findById(Long aLong);


    @Override
    @Modifying
    <S extends DGS10> List<S> saveAll(Iterable<S> iterable);


    Optional<DGS10> findFirstByRocFlagIsTrueOrderByDateDesc();

    Optional<List<DGS10>> findByRollAverageFlagIsFalseOrderByDate();

    Optional<List<DGS10>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
    Optional<List<DGS10>> findByRocFlagIsFalseOrderByDate();

    Optional<DGS10>findTopByOrderByDateDesc();
}
