package org.pinnel.pinnelapi.repository;

import java.time.LocalDate;
import org.pinnel.pinnelapi.entity.TripDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripDetailRepository extends JpaRepository<TripDetailEntity, Long> {

    /** Returns the highest cityOrder for a given (tripId, visitDate), or -1 if no rows exist yet. */
    @Query("SELECT COALESCE(MAX(td.cityOrder), -1) FROM TripDetailEntity td WHERE td.trip.id = :tripId AND td.visitDate = :visitDate")
    int findMaxCityOrder(@Param("tripId") Long tripId, @Param("visitDate") LocalDate visitDate);
}
