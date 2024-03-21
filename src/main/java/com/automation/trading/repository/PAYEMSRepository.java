package com.automation.trading.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.automation.trading.domain.fred.PAYEMS;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PAYEMSRepository extends JpaRepository<PAYEMS, Date>{

    @Query(value = "SELECT EXISTS (SELECT 1 FROM payems)", nativeQuery = true)
    Integer findAny();
    
    @Override
    @Modifying
    <S extends PAYEMS> List<S> saveAll(Iterable<S> iterable);


    Optional<PAYEMS> findFirstByRocFlagIsTrueOrderByDateDesc();

    Optional<List<PAYEMS>> findByRollAverageFlagIsFalseOrderByDate();

    Optional<List<PAYEMS>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
    Optional<List<PAYEMS>> findByRocFlagIsFalseOrderByDate();

    Optional<PAYEMS>findTopByOrderByDateDesc();
}
