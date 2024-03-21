package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.M2;

public interface M2Repository extends JpaRepository<M2, Date>{

	@Query(value = "SELECT EXISTS (SELECT 1 FROM m2)", nativeQuery = true)
	Integer findAny();

	Optional<List<M2>> findByRocFlagIsFalseOrderByDate();

	Optional<M2> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<M2>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<M2>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<M2> findTopByOrderByDateDesc();

}
