package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.ICSA;

public interface ICSARepository extends JpaRepository<ICSA, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM icsa)", nativeQuery = true)
	Integer findAny();

	Optional<List<ICSA>> findByRocFlagIsFalseOrderByDate();

	Optional<ICSA> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<ICSA>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<ICSA>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<ICSA> findTopByOrderByDateDesc();

}
