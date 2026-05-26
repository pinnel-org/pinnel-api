package org.pinnel.pinnelapi.repository;

import java.util.List;
import java.util.Set;
import org.pinnel.pinnelapi.entity.TripEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<TripEntity, Long> {

    List<TripEntity> findByUserId(Long userId);

    @Query("""
            SELECT DISTINCT c.country
            FROM TripEntity t
            JOIN t.cities c
            WHERE t.user.id = :userId
            """)
    Set<String> findDistinctCountriesByUserId(@Param("userId") Long userId);
}
