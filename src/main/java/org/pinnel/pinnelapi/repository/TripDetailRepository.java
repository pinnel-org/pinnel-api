package org.pinnel.pinnelapi.repository;

import java.time.LocalDate;
import java.util.List;
import org.pinnel.pinnelapi.entity.TripDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripDetailRepository extends JpaRepository<TripDetailEntity, Long> {

    /** Returns the highest cityOrder for a given (tripId, visitDate), or -1 if no rows exist yet. */
    @Query("SELECT COALESCE(MAX(td.cityOrder), -1) FROM TripDetailEntity td WHERE td.trip.id = :tripId AND td.visitDate = :visitDate")
    int findMaxCityOrder(@Param("tripId") Long tripId, @Param("visitDate") LocalDate visitDate);

    /** Returns all details for a given (tripId, userId, visitDate) ordered by cityOrder ascending. */
    List<TripDetailEntity> findByTrip_IdAndUserIdAndVisitDateOrderByCityOrder(Long tripId, Long userId, LocalDate visitDate);

    /** Bulk-deletes all details for a given (tripId, userId, visitDate). DB cascade removes trip_detail_pins. */
    @Modifying
    @Query("DELETE FROM TripDetailEntity td WHERE td.trip.id = :tripId AND td.userId = :userId AND td.visitDate = :date")
    void deleteByTripAndUserAndDate(@Param("tripId") Long tripId, @Param("userId") Long userId, @Param("date") LocalDate date);
}
