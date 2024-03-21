package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.GDPC1;

public interface GDPC1Repository extends JpaRepository<GDPC1, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM gdpc1)", nativeQuery = true)
	Integer findAny();

	Optional<List<GDPC1>> findByRocFlagIsFalseOrderByDate();

	Optional<GDPC1> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<GDPC1>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<GDPC1>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<GDPC1> findTopByOrderByDateDesc();

}
