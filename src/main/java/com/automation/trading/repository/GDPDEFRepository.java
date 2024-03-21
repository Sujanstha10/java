package com.automation.trading.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.GDPDEF;

public interface GDPDEFRepository extends JpaRepository<GDPDEF, Long> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM gdpdef)", nativeQuery = true)
	Integer findAny();

	Optional<List<GDPDEF>> findByRocFlagIsFalseOrderByDate();

	Optional<GDPDEF> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<GDPDEF> findTopByOrderByDateDesc();

	Optional<List<GDPDEF>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<GDPDEF>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

}
