package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.interestrates.T10YIE;

public interface T10YIERepository extends JpaRepository<T10YIE, Date>{

	Optional<List<T10YIE>> findByRocFlagIsFalseOrderByDate();

	Optional<T10YIE> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<T10YIE>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<T10YIE>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<T10YIE> findTopByOrderByDateDesc();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM t10yie)", nativeQuery = true)
    Integer findAny();

}
