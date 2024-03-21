package com.automation.trading.repository;

import com.automation.trading.domain.fred.MANEMP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface MANEMPRepository extends JpaRepository<MANEMP, Date>{

    @Query(value = "SELECT EXISTS (SELECT 1 FROM manemp)", nativeQuery = true)
    Integer findAny();

    @Override
    @Modifying
    <S extends MANEMP> List<S> saveAll(Iterable<S> iterable);


    Optional<MANEMP> findFirstByRocFlagIsTrueOrderByDateDesc();

    Optional<List<MANEMP>> findByRollAverageFlagIsFalseOrderByDate();

    Optional<List<MANEMP>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
    Optional<List<MANEMP>> findByRocFlagIsFalseOrderByDate();

    Optional<MANEMP>findTopByOrderByDateDesc();
}
