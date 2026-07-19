package com.gestudio.crm.institution;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {

  Optional<Institution> findByNormalizedNameAndLocality(String normalizedName, String locality);
}
