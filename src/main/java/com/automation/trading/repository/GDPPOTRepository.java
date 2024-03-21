package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.GDPPOT;

public interface GDPPOTRepository extends JpaRepository<GDPPOT, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM gdppot)", nativeQuery = true)
    Integer findAny();
	Optional<List<GDPPOT>> findByRocFlagIsFalseOrderByDate();

	Optional<GDPPOT> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<GDPPOT> findTopByOrderByDateDesc();

	Optional<List<GDPPOT>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<GDPPOT>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

}
