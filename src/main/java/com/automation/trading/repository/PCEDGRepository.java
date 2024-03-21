package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.PCE;
import com.automation.trading.domain.fred.PCEDG;

public interface PCEDGRepository extends JpaRepository<PCEDG, Date> {

	Optional<List<PCEDG>> findByRocFlagIsFalseOrderByDate();

	Optional<PCEDG> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<PCEDG>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<PCEDG>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<PCEDG> findTopByOrderByDateDesc();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM pcedg)", nativeQuery = true)
    Integer findAny();
}
