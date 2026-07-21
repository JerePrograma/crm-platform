package com.gestudio.crm.prospect;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProspectRepository
    extends JpaRepository<Prospect, UUID>, JpaSpecificationExecutor<Prospect> {

  Optional<Prospect> findByExternalSourceId(String externalSourceId);

  Optional<Prospect> findByOrganizationIdAndExternalSourceId(
      UUID organizationId, String externalSourceId);

  Optional<Prospect> findFirstByInstitutionId(UUID institutionId);

  Optional<Prospect> findFirstByOrganizationIdAndInstitutionId(
      UUID organizationId, UUID institutionId);

  Optional<Prospect> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Page<Prospect> findAllByStatus(ProspectStatus status, Pageable pageable);

  Page<Prospect> findAllByOrganizationId(UUID organizationId, Pageable pageable);

  Page<Prospect> findAllByOrganizationIdAndStatus(
      UUID organizationId, ProspectStatus status, Pageable pageable);
}
