package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.Gdp;

public interface GdpRepository extends JpaRepository<Gdp, Date> {

    @Override
    @Modifying
    <S extends Gdp> List<S> saveAll(Iterable<S> iterable);

    List<Gdp> findAll();


    @Modifying
    @Query(value = "UPDATE gdp SET gdp.roc_flag=1 where id=:id",nativeQuery = true)
    Integer updateRocFlag(Long id);

    Optional<Gdp>findTopByOrderByDateDesc();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM gdp)", nativeQuery = true)
    Integer findAny();

    Optional<List<Gdp>> findByRocFlagIsFalseOrderByDate();

    Optional<Gdp> findFirstByRocFlagIsTrueOrderByDateDesc();

    Optional<List<Gdp>> findByRollAverageFlagIsFalseOrderByDate();

    Optional<List<Gdp>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();



}
