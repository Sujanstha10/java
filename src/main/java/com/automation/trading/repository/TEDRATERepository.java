package com.automation.trading.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.interestrates.TEDRATE;

public interface TEDRATERepository extends JpaRepository<TEDRATE, Long>{

	Optional<List<TEDRATE>> findByRocFlagIsFalseOrderByDate();

	Optional<TEDRATE> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<TEDRATE>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<TEDRATE>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<TEDRATE> findTopByOrderByDateDesc();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM tedrate)", nativeQuery = true)
    Integer findAny();

}
