package com.automation.trading.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.UNEMPLOY;

public interface UNEMPLOYRepository extends JpaRepository<UNEMPLOY, Long>{

	Optional<List<UNEMPLOY>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<UNEMPLOY>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<UNEMPLOY> findTopByOrderByDateDesc();

	Optional<List<UNEMPLOY>> findByRocFlagIsFalseOrderByDate();

	Optional<UNEMPLOY> findFirstByRocFlagIsTrueOrderByDateDesc();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM unemploy)", nativeQuery = true)
	Integer findAny();

}
