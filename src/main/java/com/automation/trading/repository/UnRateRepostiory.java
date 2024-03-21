package com.automation.trading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.UnRate;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface UnRateRepostiory extends JpaRepository<UnRate, Date>{

	@Query(value = "SELECT EXISTS (SELECT 1 FROM un_rate)", nativeQuery = true)
    Integer findAny();

    @Modifying
    @Query(value = "UPDATE un_rate SET un_rate.roc_flag=1 where id=:id",nativeQuery = true)
    Integer updateRocFlag(Long id);

    Optional<UnRate>findTopByOrderByDateDesc();

    @Override
    @Modifying
    <S extends UnRate> List<S> saveAll(Iterable<S> iterable);

    Optional<List<UnRate>> findByRocFlagIsFalseOrderByDate();

    Optional<UnRate> findFirstByRocFlagIsTrueOrderByDateDesc();

    Optional<List<UnRate>> findByRollAverageFlagIsFalseOrderByDate();

    Optional<List<UnRate>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
}
