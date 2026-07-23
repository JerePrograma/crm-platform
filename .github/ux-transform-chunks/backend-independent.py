from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file_path = Path(path)
    source = file_path.read_text(encoding="utf-8")
    count = source.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one match in {path}, found {count}: {old[:120]!r}")
    file_path.write_text(source.replace(old, new, 1), encoding="utf-8")


replace_once(
    "backend/src/main/java/com/gestudio/crm/prospect/ProspectApplicationService.java",
    '''  @Transactional
  public ProspectView create(CreateProspectCommand command) {
    Objects.requireNonNull(command, "Create prospect command is required");''',
    '''  @Transactional
  public ProspectView create(CreateProspectCommand command) {
    return create(command, null);
  }

  @Transactional
  public ProspectView createIndependent(CreateProspectCommand command, UUID distinctionId) {
    Objects.requireNonNull(distinctionId, "Distinct institution id is required");
    return create(command, distinctionId);
  }

  private ProspectView create(CreateProspectCommand command, UUID distinctionId) {
    Objects.requireNonNull(command, "Create prospect command is required");''',
)

replace_once(
    "backend/src/main/java/com/gestudio/crm/prospect/ProspectApplicationService.java",
    '''    Institution institution =
        resolveInstitution(command, normalizedName, normalizedLocality, websiteDomain);''',
    '''    Institution institution =
        distinctionId == null
            ? resolveInstitution(command, normalizedName, normalizedLocality, websiteDomain)
            : createInstitution(
                command,
                normalizedName + " duplicate review " + distinctionId,
                normalizedLocality,
                websiteDomain);''',
)

replace_once(
    "backend/src/main/java/com/gestudio/crm/prospect/ProspectApplicationService.java",
    '''    return byNameAndLocation
        .or(() -> byDomain)
        .orElseGet(
            () -> {
              Institution institution =
                  Institution.create(
                      command.institutionName(),
                      normalizedName,
                      normalizationService.trimToNull(command.category()),
                      normalizationService.trimToNull(command.locality()),
                      normalizedLocality,
                      normalizationService.trimToNull(command.province()),
                      normalizationService.trimToNull(command.country()),
                      normalizationService.trimToNull(command.website()),
                      websiteDomain);
              institution.assignOrganization(currentActor.organizationId());
              return institutionRepository.save(institution);
            });
  }

  private List<PreparedChannel> prepareContactChannels(CreateProspectCommand command) {''',
    '''    return byNameAndLocation
        .or(() -> byDomain)
        .orElseGet(
            () -> createInstitution(command, normalizedName, normalizedLocality, websiteDomain));
  }

  private Institution createInstitution(
      CreateProspectCommand command,
      String normalizedName,
      String normalizedLocality,
      String websiteDomain) {
    Institution institution =
        Institution.create(
            command.institutionName(),
            normalizedName,
            normalizationService.trimToNull(command.category()),
            normalizationService.trimToNull(command.locality()),
            normalizedLocality,
            normalizationService.trimToNull(command.province()),
            normalizationService.trimToNull(command.country()),
            normalizationService.trimToNull(command.website()),
            websiteDomain);
    institution.assignOrganization(currentActor.organizationId());
    return institutionRepository.save(institution);
  }

  private List<PreparedChannel> prepareContactChannels(CreateProspectCommand command) {''',
)

replace_once(
    "backend/src/main/java/com/gestudio/crm/deduplication/DuplicateResolutionService.java",
    '''    UUID prospectId = prospectApplicationService.create(imported.toCommand(review.id())).id();''',
    '''    UUID prospectId =
        prospectApplicationService.createIndependent(imported.toCommand(review.id()), review.id()).id();''',
)

replace_once(
    "backend/src/test/java/com/gestudio/crm/deduplication/DuplicateResolutionIntegrationTest.java",
    '''    assertThat(separateResult.survivorProspectId()).isNotNull().isNotEqualTo(existing);
    assertRow(separate, "ACCEPTED", separateResult.survivorProspectId());''',
    '''    assertThat(separateResult.survivorProspectId()).isNotNull().isNotEqualTo(existing);
    assertRow(separate, "ACCEPTED", separateResult.survivorProspectId());
    assertImportedEmailPreserved(separate);''',
)

replace_once(
    "backend/src/test/java/com/gestudio/crm/deduplication/DuplicateResolutionIntegrationTest.java",
    '''    assertThat(notDuplicateResult.action()).isEqualTo("MARK_NOT_DUPLICATE");
    assertRow(notDuplicate, "ACCEPTED", notDuplicateResult.survivorProspectId());''',
    '''    assertThat(notDuplicateResult.action()).isEqualTo("MARK_NOT_DUPLICATE");
    assertRow(notDuplicate, "ACCEPTED", notDuplicateResult.survivorProspectId());
    assertImportedEmailPreserved(notDuplicate);''',
)

replace_once(
    "backend/src/test/java/com/gestudio/crm/deduplication/DuplicateResolutionIntegrationTest.java",
    '''  private int count(String table, UUID prospectId) {''',
    '''  private void assertImportedEmailPreserved(UUID reviewId) {
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM duplicate_review dr
                JOIN import_row ir
                  ON ir.id = dr.import_row_id AND ir.organization_id = dr.organization_id
                JOIN prospect p
                  ON p.id = ir.prospect_id AND p.organization_id = dr.organization_id
                JOIN institution i
                  ON i.id = p.institution_id AND i.organization_id = dr.organization_id
                JOIN contact c
                  ON c.institution_id = p.institution_id
                  AND c.organization_id = dr.organization_id
                  AND c.deleted_at IS NULL
                JOIN contact_channel cc
                  ON cc.contact_id = c.id AND cc.organization_id = dr.organization_id
                WHERE dr.id = ? AND dr.organization_id = ?
                  AND cc.type = 'EMAIL'
                  AND cc.normalized_value = ir.normalized_email
                  AND i.name = 'Synthetic candidate'
                """,
                Integer.class,
                reviewId,
                principal.organizationId()))
        .isEqualTo(1);
  }

  private int count(String table, UUID prospectId) {''',
)

print("Independent duplicate prospect creation patch applied.")
