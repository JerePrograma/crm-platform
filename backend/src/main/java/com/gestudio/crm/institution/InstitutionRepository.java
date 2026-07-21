package com.gestudio.crm.institution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {

  Optional<Institution> findByNormalizedNameAndNormalizedLocality(
      String normalizedName, String normalizedLocality);

  Optional<Institution> findByOrganizationIdAndNormalizedNameAndNormalizedLocality(
      UUID organizationId, String normalizedName, String normalizedLocality);

  Optional<Institution> findFirstByWebsiteDomain(String websiteDomain);

  Optional<Institution> findFirstByOrganizationIdAndWebsiteDomain(
      UUID organizationId, String websiteDomain);

  List<Institution> findAllByNormalizedLocality(String normalizedLocality);

  List<Institution> findAllByOrganizationIdAndNormalizedLocality(
      UUID organizationId, String normalizedLocality);
}
