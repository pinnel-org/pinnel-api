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

    /** Deletes a single detail by id + userId. Returns 1 if deleted, 0 if the id exists but belongs to another user. */
    @Modifying
    @Query("DELETE FROM TripDetailEntity td WHERE td.id = :id AND td.userId = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /** Returns all details for a given (tripId, userId) ordered by visitDate then cityOrder ascending. */
    List<TripDetailEntity> findByTrip_IdAndUserIdOrderByVisitDateAscCityOrderAsc(Long tripId, Long userId);

    /** Bulk-deletes all details for a given (tripId, userId, visitDate). Returns rows affected. DB cascade removes trip_detail_pins. */
    @Modifying
    @Query("DELETE FROM TripDetailEntity td WHERE td.trip.id = :tripId AND td.userId = :userId AND td.visitDate = :visitDate")
    int deleteByTripIdAndUserIdAndVisitDate(@Param("tripId") Long tripId, @Param("userId") Long userId, @Param("visitDate") LocalDate visitDate);
}
