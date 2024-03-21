package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.PCE;

public interface PCERepository extends JpaRepository<PCE, Date>{
	
	Optional<List<PCE>> findByRocFlagIsFalseOrderByDate();

	Optional<PCE> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<PCE>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<PCE>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<PCE> findTopByOrderByDateDesc();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM pce)", nativeQuery = true)
    Integer findAny();

}
