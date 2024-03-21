package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.M2V;

public interface M2VRepository extends JpaRepository<M2V, Date>{

	@Query(value = "SELECT EXISTS (SELECT 1 FROM m2v)", nativeQuery = true)
	Integer findAny();

	Optional<List<M2V>> findByRocFlagIsFalseOrderByDate();

	Optional<M2V> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<M2V> findTopByOrderByDateDesc();

	Optional<List<M2V>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<M2V>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

}
