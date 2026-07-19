package com.gestudio.crm.imports;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportRowRepository extends JpaRepository<ImportRow, UUID> {

  List<ImportRow> findAllByImportJobIdOrderByRowNumber(UUID importJobId);
}
