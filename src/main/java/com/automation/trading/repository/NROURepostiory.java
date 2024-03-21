package com.automation.trading.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.NROU;

public interface NROURepostiory extends JpaRepository<NROU, Long> {

	Optional<List<NROU>> findByRocFlagIsFalseOrderByDate();

	Optional<NROU> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<NROU> findTopByOrderByDateDesc();

	Optional<List<NROU>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<NROU>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM nrou)", nativeQuery = true)
	Integer findAny();

}
