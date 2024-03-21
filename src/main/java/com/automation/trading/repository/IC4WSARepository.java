package com.automation.trading.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.IC4WSA;

public interface IC4WSARepository extends JpaRepository<IC4WSA, Date>{

	@Query(value = "SELECT EXISTS (SELECT 1 FROM ic4wsa)", nativeQuery = true)
	Integer findAny();

	Optional<List<IC4WSA>> findByRocFlagIsFalseOrderByDate();

	Optional<IC4WSA> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<IC4WSA>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<IC4WSA>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();

	Optional<IC4WSA> findTopByOrderByDateDesc();
}
