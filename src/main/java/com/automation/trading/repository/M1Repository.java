package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.M1;

public interface M1Repository extends JpaRepository<M1, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM m1)", nativeQuery = true)
	Integer findAny();

	Optional<List<M1>> findByRocFlagIsFalseOrderByDate();

	Optional<M1> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<M1>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<M1>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<M1> findTopByOrderByDateDesc();

}
