package com.automation.trading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.interestrates.DGS30;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface DGS30Repository extends JpaRepository<DGS30, Date>{

	@Query(value = "SELECT EXISTS (SELECT 1 FROM dgs30)", nativeQuery = true)
	Integer findAny();

	@Override
	@Modifying
	<S extends DGS30> List<S> saveAll(Iterable<S> iterable);


	Optional<DGS30> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<DGS30>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<DGS30>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
	Optional<List<DGS30>> findByRocFlagIsFalseOrderByDate();

	Optional<DGS30>findTopByOrderByDateDesc();
}
