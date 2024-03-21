package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.BASE;

public interface BASERepository extends JpaRepository<BASE, Date>{

	@Query(value = "SELECT EXISTS (SELECT 1 FROM base)", nativeQuery = true)
	Integer findAny();

	Optional<List<BASE>> findByRocFlagIsFalseOrderByDate();

	Optional<BASE> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<BASE>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<BASE>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<BASE> findTopByOrderByDateDesc();

}
