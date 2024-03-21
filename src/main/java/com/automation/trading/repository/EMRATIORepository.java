package com.automation.trading.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.EMRATIO;
import com.automation.trading.domain.fred.GDPC1;

public interface EMRATIORepository extends JpaRepository<EMRATIO, Long>{

	Optional<List<EMRATIO>> findByRocFlagIsFalseOrderByDate();

	Optional<EMRATIO> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<EMRATIO>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<EMRATIO>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM emratio)", nativeQuery = true)
	Integer findAny();

	Optional<EMRATIO> findTopByOrderByDateDesc();

}
