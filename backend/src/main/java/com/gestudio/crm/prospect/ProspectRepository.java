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

  Optional<Prospect> findFirstByInstitutionId(UUID institutionId);

  Page<Prospect> findAllByStatus(ProspectStatus status, Pageable pageable);
}
