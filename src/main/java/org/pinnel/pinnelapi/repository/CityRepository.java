package org.pinnel.pinnelapi.repository;

import java.util.List;
import java.util.Optional;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CityRepository extends JpaRepository<CityEntity, Long> {

    Optional<CityEntity> findByNameAndCountry(String name, String country);

    boolean existsByNameAndCountry(String name, String country);

    @Query("""
            SELECT c FROM CityEntity c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT(:prefix, '%'))
            ORDER BY c.population DESC NULLS LAST, c.name ASC
            """)
    List<CityEntity> searchByNamePrefix(@Param("prefix") String prefix, Pageable pageable);
}
