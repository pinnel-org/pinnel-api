package org.pinnel.pinnelapi.repository;

import java.util.Optional;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<CityEntity, Long> {

    Optional<CityEntity> findByNameAndCountry(String name, String country);

    boolean existsByNameAndCountry(String name, String country);
}
