package com.automation.trading.repository;

import com.automation.trading.domain.calculation.DGS10Calculation;
import com.automation.trading.domain.calculation.DGS5Calculation;
import com.automation.trading.domain.fred.DFF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.interestrates.DGS5;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface DGS5Repository extends JpaRepository<DGS5, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM dgs5)", nativeQuery = true)
	Integer findAny();

	@Override
	@Modifying
	<S extends DGS5> List<S> saveAll(Iterable<S> iterable);


	Optional<DGS5> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<DGS5>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<DGS5>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
	Optional<List<DGS5>> findByRocFlagIsFalseOrderByDate();

	Optional<DGS5>findTopByOrderByDateDesc();


}
