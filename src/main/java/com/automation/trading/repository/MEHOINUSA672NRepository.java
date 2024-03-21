package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.MEHOINUSA672N;
import com.automation.trading.domain.fred.NROU;

public interface MEHOINUSA672NRepository extends JpaRepository<MEHOINUSA672N, Date>{

	Optional<List<MEHOINUSA672N>> findByRocFlagIsFalseOrderByDate();

	Optional<MEHOINUSA672N> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<MEHOINUSA672N> findTopByOrderByDateDesc();

	Optional<List<MEHOINUSA672N>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<MEHOINUSA672N>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM mehoinusa672n)", nativeQuery = true)
	Integer findAny();
	
}
