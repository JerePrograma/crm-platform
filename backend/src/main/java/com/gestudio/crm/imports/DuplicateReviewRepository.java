package com.gestudio.crm.imports;

import com.gestudio.crm.imports.DuplicateReview.Status;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DuplicateReviewRepository extends JpaRepository<DuplicateReview, UUID> {

  List<DuplicateReview> findAllByStatusOrderByCreatedAtAsc(Status status);

  List<DuplicateReview> findAllByOrganizationIdAndStatusOrderByCreatedAtAsc(
      UUID organizationId, Status status);
}
