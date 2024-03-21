package com.automation.trading.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.DPRIME;

public interface DPRIMERepository extends JpaRepository<DPRIME, Long>{

	Optional<List<DPRIME>> findByRocFlagIsFalseOrderByDate();

	Optional<DPRIME> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<DPRIME>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<DPRIME>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<DPRIME> findTopByOrderByDateDesc();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM dprime)", nativeQuery = true)
    Integer findAny();

}
