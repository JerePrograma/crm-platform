package com.gestudio.crm.imports;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImportRowRepository
    extends JpaRepository<ImportRow, UUID>, JpaSpecificationExecutor<ImportRow> {

  List<ImportRow> findAllByImportJobIdOrderBySourceSheetAscRowNumberAsc(UUID importJobId);

  List<ImportRow> findAllByOrganizationIdAndImportJobIdOrderBySourceSheetAscRowNumberAsc(
      UUID organizationId, UUID importJobId);

  @Query(
      """
      SELECT DISTINCT row.sourceSheet
      FROM ImportRow row
      WHERE row.organizationId = :organizationId
        AND row.importJob.id = :importJobId
      ORDER BY row.sourceSheet
      """)
  List<String> findDistinctSourceSheets(
      @Param("organizationId") UUID organizationId, @Param("importJobId") UUID importJobId);
}
