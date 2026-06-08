package org.pinnel.pinnelapi.repository;

import java.util.List;
import org.pinnel.pinnelapi.entity.TripDayCityEntity;
import org.pinnel.pinnelapi.entity.TripDayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripDayRepository extends JpaRepository<TripDayEntity, Long> {

    @Query("""
            SELECT d FROM TripDayEntity d
            LEFT JOIN FETCH d.cities
            WHERE d.trip.id = :tripId
            ORDER BY d.visitDate ASC
            """)
    List<TripDayEntity> findByTripIdWithCities(@Param("tripId") Long tripId);

    // Hibernate 7 forbids JOIN FETCH on two List (bag) associations in one query.
    // This second query initialises the pins collections on TripDayCityEntity objects
    // already tracked by the session, relying on first-level cache merging.
    @Query("""
            SELECT c FROM TripDayCityEntity c
            LEFT JOIN FETCH c.pins
            WHERE c.tripDay.trip.id = :tripId
            """)
    List<TripDayCityEntity> initPinsForTrip(@Param("tripId") Long tripId);

    /** Loads all days for a trip with their cities and pins fully initialised. */
    default List<TripDayEntity> findByTripIdWithDetails(Long tripId) {
        List<TripDayEntity> days = findByTripIdWithCities(tripId);
        initPinsForTrip(tripId);
        return days;
    }

    void deleteByTripId(Long tripId);
}
