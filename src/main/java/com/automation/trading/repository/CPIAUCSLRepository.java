package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.CPIAUCSL;

public interface CPIAUCSLRepository extends JpaRepository<CPIAUCSL, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM cpiaucsl)", nativeQuery = true)
	Integer findAny();

	Optional<List<CPIAUCSL>> findByRocFlagIsFalseOrderByDate();

	Optional<CPIAUCSL> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<CPIAUCSL>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<CPIAUCSL>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<CPIAUCSL> findTopByOrderByDateDesc();

}
