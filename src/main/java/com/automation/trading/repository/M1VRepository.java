package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.M1V;

public interface M1VRepository extends JpaRepository<M1V, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM m1v)", nativeQuery = true)
	Integer findAny();

	Optional<List<M1V>> findByRocFlagIsFalseOrderByDate();

	Optional<M1V> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<M1V>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<M1V>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<M1V> findTopByOrderByDateDesc();

}
