package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.DSPIC96;

public interface DSPIC96Repository extends JpaRepository<DSPIC96, Date> {

	Optional<List<DSPIC96>> findByRocFlagIsFalseOrderByDate();

	Optional<DSPIC96> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<DSPIC96>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<DSPIC96>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<DSPIC96> findTopByOrderByDateDesc();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM dspic96)", nativeQuery = true)
	Integer findAny();

}
