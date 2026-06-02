package org.pinnel.pinnelapi.repository;

import java.util.List;
import org.pinnel.pinnelapi.entity.TripDayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripDayRepository extends JpaRepository<TripDayEntity, Long> {

    @Query("""
            SELECT d FROM TripDayEntity d
            LEFT JOIN FETCH d.cities c
            LEFT JOIN FETCH c.pins
            WHERE d.trip.id = :tripId
            ORDER BY d.visitDate ASC
            """)
    List<TripDayEntity> findByTripIdWithDetails(@Param("tripId") Long tripId);

    void deleteByTripId(Long tripId);
}
