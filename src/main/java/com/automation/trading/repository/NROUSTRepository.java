package com.automation.trading.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.NROUST;

public interface NROUSTRepository extends JpaRepository<NROUST, Long> {

	Optional<List<NROUST>> findByRocFlagIsFalseOrderByDate();

	Optional<NROUST> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<NROUST>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<NROUST>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<NROUST> findTopByOrderByDateDesc();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM nroust)", nativeQuery = true)
	Integer findAny();

}
