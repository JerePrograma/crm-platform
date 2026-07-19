package com.gestudio.crm.imports;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {

  Optional<ImportJob> findByIdempotencyKey(String idempotencyKey);
}
