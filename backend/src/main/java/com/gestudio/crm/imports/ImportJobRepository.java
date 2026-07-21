package com.gestudio.crm.imports;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {

  Optional<ImportJob> findByIdempotencyKey(String idempotencyKey);

  Optional<ImportJob> findByOrganizationIdAndIdempotencyKey(
      UUID organizationId, String idempotencyKey);

  Optional<ImportJob> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
