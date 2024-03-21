package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.interestrates.T5YIE;

public interface T5YIERepository extends JpaRepository<T5YIE, Date>{
	
	@Query(value = "SELECT EXISTS (SELECT 1 FROM t5yie)", nativeQuery = true)
    Integer findAny();

	Optional<List<T5YIE>> findByRocFlagIsFalseOrderByDate();

	Optional<T5YIE> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<T5YIE>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<T5YIE>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<T5YIE> findTopByOrderByDateDesc();

}
