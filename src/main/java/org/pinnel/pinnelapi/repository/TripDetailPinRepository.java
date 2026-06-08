package org.pinnel.pinnelapi.repository;

import java.util.List;
import org.pinnel.pinnelapi.entity.TripDetailPinEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripDetailPinRepository extends JpaRepository<TripDetailPinEntity, Long> {

    /** Returns the highest pinOrder for a given tripDetailId, or -1 if no rows exist yet. */
    @Query("SELECT COALESCE(MAX(p.pinOrder), -1) FROM TripDetailPinEntity p WHERE p.tripDetail.id = :tripDetailId")
    int findMaxPinOrder(@Param("tripDetailId") Long tripDetailId);

    /** Returns all pin entries for a given (tripDetailId, userId) ordered by pinOrder ascending. */
    List<TripDetailPinEntity> findByTripDetail_IdAndUserIdOrderByPinOrder(Long tripDetailId, Long userId);

    /** Deletes a single pin entry by id + userId. Returns 1 if deleted, 0 if the id exists but belongs to another user. */
    @Modifying
    @Query("DELETE FROM TripDetailPinEntity p WHERE p.id = :id AND p.userId = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
