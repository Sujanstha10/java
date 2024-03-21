package com.automation.trading.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.DFF;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface DFFRepository extends JpaRepository<DFF, Date> {

	@Query("select min(d.id) from DFF d where d.rocFlag =false ")
	Long findMinId();

	@Query(value = "SELECT EXISTS (SELECT 1 FROM dff)", nativeQuery = true)
	Integer findAny();


	@Modifying
	@Query(value = "UPDATE dff SET dff.roc_flag=1 where id=:id",nativeQuery = true)
	Integer updateRocFlag(Long id);

	Optional<DFF>findTopByOrderByDateDesc();

	@Override
	@Modifying
	<S extends DFF> List<S> saveAll(Iterable<S> iterable);

	Optional<List<DFF>> findByRocFlagIsFalseOrderByDate();

	Optional<DFF> findFirstByRocFlagIsTrueOrderByDateDesc();

	Optional<List<DFF>> findByRollAverageFlagIsFalseOrderByDate();

	Optional<List<DFF>> findTop2ByRollAverageFlagIsTrueOrderByDateDesc();



}
