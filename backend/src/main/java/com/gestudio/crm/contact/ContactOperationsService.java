package com.gestudio.crm.contact;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.DuplicateResourceException;
import com.gestudio.crm.common.NormalizationService;
import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.exclusion.ContactEligibilityService;
import com.gestudio.crm.exclusion.ContactEligibilityService.ChannelCandidate;
import com.gestudio.crm.security.CurrentActor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactOperationsService {

  private static final Set<String> CONSENT = Set.of("UNKNOWN", "GRANTED", "DENIED");

  private final JdbcTemplate jdbcTemplate;
  private final NormalizationService normalizationService;
  private final ContactEligibilityService eligibilityService;
  private final CurrentActor currentActor;
  private final AuditEventWriter auditEventWriter;

  public ContactOperationsService(
      JdbcTemplate jdbcTemplate,
      NormalizationService normalizationService,
      ContactEligibilityService eligibilityService,
      CurrentActor currentActor,
      AuditEventWriter auditEventWriter) {
    this.jdbcTemplate = jdbcTemplate;
    this.normalizationService = normalizationService;
    this.eligibilityService = eligibilityService;
    this.currentActor = currentActor;
    this.auditEventWriter = auditEventWriter;
  }

  @Transactional(readOnly = true)
  public List<ContactView> listForProspect(UUID prospectId) {
    requireInstitution(prospectId);
    Map<UUID, ContactAccumulator> contacts = new LinkedHashMap<>();
    jdbcTemplate.query(
        """
        SELECT c.id, c.version, c.first_name, c.last_name, c.name, c.role,
               c.primary_contact, c.verified, c.preferred_channel, c.consent, c.source,
               c.last_validated_at, c.created_at, c.updated_at,
               cc.id AS channel_id, cc.version AS channel_version, cc.type, cc.value,
               cc.normalized_value, cc.primary_channel, cc.valid, cc.verified AS channel_verified,
               cc.consent AS channel_consent, cc.preferred, cc.last_validated_at AS channel_validated_at
        FROM prospect p
        JOIN contact c ON c.institution_id = p.institution_id
          AND c.organization_id = p.organization_id AND c.deleted_at IS NULL
        LEFT JOIN contact_channel cc ON cc.contact_id = c.id AND cc.organization_id = c.organization_id
        WHERE p.id = ? AND p.organization_id = ?
        ORDER BY c.primary_contact DESC, c.created_at, cc.primary_channel DESC, cc.created_at
        """,
        (org.springframework.jdbc.core.RowCallbackHandler)
            resultSet -> accumulate(contacts, resultSet),
        prospectId,
        currentActor.organizationId());
    return contacts.values().stream().map(ContactAccumulator::view).toList();
  }

  @Transactional
  public ContactView create(UUID prospectId, CreateContactCommand command) {
    UUID institutionId = requireInstitution(prospectId);
    validateContact(command.firstName(), command.lastName(), command.channels());
    UUID contactId = UUID.randomUUID();
    if (command.primary()) {
      clearPrimary(institutionId);
    }
    String consent = consent(command.consent());
    jdbcTemplate.update(
        """
        INSERT INTO contact (
          id, version, created_at, updated_at, organization_id, institution_id, name, role,
          first_name, last_name, primary_contact, verified, preferred_channel, consent,
          source, last_validated_at
        ) VALUES (?, 0, now(), now(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        contactId,
        currentActor.organizationId(),
        institutionId,
        displayName(command.firstName(), command.lastName()),
        trim(command.role()),
        trim(command.firstName()),
        trim(command.lastName()),
        command.primary(),
        command.verified(),
        command.preferredChannel() == null ? null : command.preferredChannel().name(),
        consent,
        trim(command.source()),
        timestamp(command.lastValidatedAt()));
    boolean first = true;
    for (ChannelCommand channel :
        command.channels() == null ? List.<ChannelCommand>of() : command.channels()) {
      createChannel(prospectId, contactId, channel, first);
      first = false;
    }
    refreshContactability(prospectId);
    auditEventWriter.record(
        "CONTACT_CREATED", "Contact", contactId, Map.of("prospectId", prospectId));
    return find(contactId);
  }

  @Transactional
  public ContactView update(UUID contactId, UpdateContactCommand command) {
    ContactView before = find(contactId);
    if (command.primary() && !before.primary()) {
      clearPrimary(institutionForContact(contactId));
    }
    int updated =
        jdbcTemplate.update(
            """
            UPDATE contact SET name = ?, role = ?, first_name = ?, last_name = ?,
              primary_contact = ?, verified = ?, preferred_channel = ?, consent = ?, source = ?,
              last_validated_at = ?, updated_at = now(), version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ? AND deleted_at IS NULL
            """,
            displayName(command.firstName(), command.lastName()),
            trim(command.role()),
            trim(command.firstName()),
            trim(command.lastName()),
            command.primary(),
            command.verified(),
            command.preferredChannel() == null ? null : command.preferredChannel().name(),
            consent(command.consent()),
            trim(command.source()),
            timestamp(command.lastValidatedAt()),
            contactId,
            currentActor.organizationId(),
            command.version());
    if (updated == 0) {
      conflict(contactId, command.version());
    }
    auditEventWriter.record(
        "CONTACT_UPDATED", "Contact", contactId, Map.of("previousVersion", before.version()));
    return find(contactId);
  }

  @Transactional
  public void delete(UUID contactId, long version) {
    find(contactId);
    UUID prospectId = prospectForContact(contactId);
    int updated =
        jdbcTemplate.update(
            """
            UPDATE contact SET deleted_at = now(), primary_contact = FALSE, updated_at = now(),
              version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ? AND deleted_at IS NULL
            """,
            contactId,
            currentActor.organizationId(),
            version);
    if (updated == 0) {
      conflict(contactId, version);
    }
    refreshContactability(prospectId);
    auditEventWriter.record("CONTACT_REMOVED", "Contact", contactId, Map.of());
  }

  @Transactional
  public ContactView makePrimary(UUID contactId, long version) {
    ContactView contact = find(contactId);
    if (contact.primary()) {
      return contact;
    }
    UUID institutionId = institutionForContact(contactId);
    clearPrimary(institutionId);
    int updated =
        jdbcTemplate.update(
            """
            UPDATE contact SET primary_contact = TRUE, updated_at = now(), version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ? AND deleted_at IS NULL
            """,
            contactId,
            currentActor.organizationId(),
            version);
    if (updated == 0) {
      conflict(contactId, version);
    }
    auditEventWriter.record("CONTACT_PRIMARY_CHANGED", "Contact", contactId, Map.of());
    return find(contact.id());
  }

  @Transactional
  public ContactView addChannel(UUID contactId, ChannelCommand command) {
    find(contactId);
    UUID prospectId = prospectForContact(contactId);
    createChannel(prospectId, contactId, command, false);
    refreshContactability(prospectId);
    auditEventWriter.record(
        "CONTACT_CHANNEL_CREATED", "Contact", contactId, Map.of("type", command.type().name()));
    return find(contactId);
  }

  @Transactional
  public ContactView updateChannel(UUID channelId, UpdateChannelCommand command) {
    ChannelReference reference = channel(channelId);
    String normalized = normalize(command.type(), command.value());
    rejectDuplicate(command.type(), normalized, channelId);
    if (command.primary()) {
      jdbcTemplate.update(
          """
          UPDATE contact_channel SET primary_channel = FALSE, updated_at = now(), version = version + 1
          WHERE contact_id = ? AND organization_id = ? AND id <> ? AND primary_channel = TRUE
          """,
          reference.contactId(),
          currentActor.organizationId(),
          channelId);
    }
    int updated =
        jdbcTemplate.update(
            """
            UPDATE contact_channel SET type = ?, value = ?, normalized_value = ?, primary_channel = ?,
              valid = ?, verified = ?, consent = ?, preferred = ?, last_validated_at = ?,
              updated_at = now(), version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ?
            """,
            command.type().name(),
            command.value().trim(),
            normalized,
            command.primary(),
            command.valid(),
            command.verified(),
            consent(command.consent()),
            command.preferred(),
            timestamp(command.lastValidatedAt()),
            channelId,
            currentActor.organizationId(),
            command.version());
    if (updated == 0) {
      throw new OptimisticConflictException("Contact channel was modified by another user");
    }
    refreshContactability(reference.prospectId());
    auditEventWriter.record(
        "CONTACT_CHANNEL_UPDATED",
        "ContactChannel",
        channelId,
        Map.of("type", command.type().name()));
    return find(reference.contactId());
  }

  @Transactional
  public void deleteChannel(UUID channelId, long version) {
    ChannelReference reference = channel(channelId);
    int deleted =
        jdbcTemplate.update(
            "DELETE FROM contact_channel WHERE id = ? AND organization_id = ? AND version = ?",
            channelId,
            currentActor.organizationId(),
            version);
    if (deleted == 0) {
      throw new OptimisticConflictException("Contact channel was modified by another user");
    }
    refreshContactability(reference.prospectId());
    auditEventWriter.record(
        "CONTACT_CHANNEL_REMOVED",
        "ContactChannel",
        channelId,
        Map.of("contactId", reference.contactId()));
  }

  private void createChannel(
      UUID prospectId, UUID contactId, ChannelCommand command, boolean first) {
    String normalized = normalize(command.type(), command.value());
    rejectDuplicate(command.type(), normalized, null);
    jdbcTemplate.update(
        """
        INSERT INTO contact_channel (
          id, version, created_at, updated_at, organization_id, contact_id, type, value,
          normalized_value, primary_channel, valid, verified, consent, preferred, last_validated_at
        ) VALUES (?, 0, now(), now(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        UUID.randomUUID(),
        currentActor.organizationId(),
        contactId,
        command.type().name(),
        command.value().trim(),
        normalized,
        first || command.primary(),
        command.valid(),
        command.verified(),
        consent(command.consent()),
        command.preferred(),
        timestamp(command.lastValidatedAt()));
  }

  private void refreshContactability(UUID prospectId) {
    List<ChannelCandidate> candidates =
        jdbcTemplate.query(
            """
            SELECT cc.type, cc.normalized_value
            FROM prospect p
            JOIN contact c ON c.institution_id = p.institution_id
              AND c.organization_id = p.organization_id
            JOIN contact_channel cc ON cc.contact_id = c.id
              AND cc.organization_id = c.organization_id
            WHERE p.id = ? AND p.organization_id = ?
              AND c.deleted_at IS NULL
              AND cc.valid = TRUE
              AND cc.consent <> 'DENIED'
            UNION ALL
            SELECT 'WEBSITE', i.website_domain
            FROM prospect p
            JOIN institution i ON i.id = p.institution_id
              AND i.organization_id = p.organization_id
            WHERE p.id = ? AND p.organization_id = ?
              AND i.website_domain IS NOT NULL
              AND i.website_domain <> ''
            """,
            (resultSet, rowNumber) ->
                new ChannelCandidate(
                    ContactChannelType.valueOf(resultSet.getString(1)), resultSet.getString(2)),
            prospectId,
            currentActor.organizationId(),
            prospectId,
            currentActor.organizationId());

    boolean hasDirectContactChannel =
        candidates.stream()
            .anyMatch(
                candidate ->
                    candidate.type() == ContactChannelType.EMAIL
                        || candidate.type() == ContactChannelType.PHONE
                        || candidate.type() == ContactChannelType.WHATSAPP);

    if (!eligibilityService.evaluate(candidates).eligible()) {
      jdbcTemplate.update(
          """
          UPDATE prospect
          SET contact_eligible = FALSE,
              eligibility = CASE WHEN status = 'CUSTOMER' THEN 'CUSTOMER' ELSE 'EXCLUDED' END,
              status = CASE WHEN status = 'CUSTOMER' THEN status ELSE 'DO_NOT_CONTACT' END,
              updated_at = now(), updated_by = ?, version = version + 1
          WHERE id = ? AND organization_id = ?
          """,
          currentActor.userIdOrNull(),
          prospectId,
          currentActor.organizationId());
      return;
    }

    if (!hasDirectContactChannel) {
      jdbcTemplate.update(
          """
          UPDATE prospect
          SET contact_eligible = FALSE,
              eligibility = CASE
                WHEN status = 'CUSTOMER' THEN 'CUSTOMER'
                WHEN status = 'DO_NOT_CONTACT' THEN 'EXCLUDED'
                ELSE 'INVALID'
              END,
              status = CASE
                WHEN status IN ('NEW', 'READY_TO_CONTACT') THEN 'NEEDS_ENRICHMENT'
                ELSE status
              END,
              updated_at = now(), updated_by = ?, version = version + 1
          WHERE id = ? AND organization_id = ?
          """,
          currentActor.userIdOrNull(),
          prospectId,
          currentActor.organizationId());
      return;
    }

    jdbcTemplate.update(
        """
        UPDATE prospect
        SET status = CASE WHEN status = 'NEEDS_ENRICHMENT' THEN 'NEW' ELSE status END,
            eligibility = CASE
              WHEN status = 'CUSTOMER' THEN 'CUSTOMER'
              WHEN status = 'DO_NOT_CONTACT' THEN 'EXCLUDED'
              ELSE 'ELIGIBLE'
            END,
            contact_eligible = CASE
              WHEN status IN ('CUSTOMER', 'DO_NOT_CONTACT') THEN FALSE
              ELSE TRUE
            END,
            updated_at = now(), updated_by = ?, version = version + 1
        WHERE id = ? AND organization_id = ?
        """,
        currentActor.userIdOrNull(),
        prospectId,
        currentActor.organizationId());
  }

  private ContactView find(UUID contactId) {
    List<ContactView> matches =
        jdbcTemplate
            .query(
                """
            SELECT p.id AS prospect_id FROM contact c
            JOIN prospect p ON p.institution_id = c.institution_id AND p.organization_id = c.organization_id
            WHERE c.id = ? AND c.organization_id = ? AND c.deleted_at IS NULL
            """,
                (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class),
                contactId,
                currentActor.organizationId())
            .stream()
            .findFirst()
            .map(this::listForProspect)
            .orElseThrow(() -> new ResourceNotFoundException("Contact not found: " + contactId));
    return matches.stream()
        .filter(contact -> contact.id().equals(contactId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Contact not found: " + contactId));
  }

  private UUID requireInstitution(UUID prospectId) {
    return jdbcTemplate
        .query(
            "SELECT institution_id FROM prospect WHERE id = ? AND organization_id = ?",
            (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class),
            prospectId,
            currentActor.organizationId())
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + prospectId));
  }

  private UUID institutionForContact(UUID contactId) {
    return jdbcTemplate
        .query(
            "SELECT institution_id FROM contact WHERE id = ? AND organization_id = ? AND deleted_at IS NULL",
            (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class),
            contactId,
            currentActor.organizationId())
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Contact not found: " + contactId));
  }

  private UUID prospectForContact(UUID contactId) {
    return jdbcTemplate
        .query(
            """
            SELECT p.id FROM contact c JOIN prospect p ON p.institution_id = c.institution_id
              AND p.organization_id = c.organization_id
            WHERE c.id = ? AND c.organization_id = ? AND c.deleted_at IS NULL
            """,
            (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class),
            contactId,
            currentActor.organizationId())
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Contact not found: " + contactId));
  }

  private ChannelReference channel(UUID channelId) {
    return jdbcTemplate
        .query(
            """
            SELECT cc.contact_id, p.id, cc.version
            FROM contact_channel cc
            JOIN contact c ON c.id = cc.contact_id AND c.organization_id = cc.organization_id
            JOIN prospect p ON p.institution_id = c.institution_id AND p.organization_id = c.organization_id
            WHERE cc.id = ? AND cc.organization_id = ? AND c.deleted_at IS NULL
            """,
            (resultSet, rowNumber) ->
                new ChannelReference(
                    resultSet.getObject(1, UUID.class),
                    resultSet.getObject(2, UUID.class),
                    resultSet.getLong(3)),
            channelId,
            currentActor.organizationId())
        .stream()
        .findFirst()
        .orElseThrow(
            () -> new ResourceNotFoundException("Contact channel not found: " + channelId));
  }

  private void accumulate(Map<UUID, ContactAccumulator> contacts, ResultSet resultSet)
      throws SQLException {
    UUID contactId = resultSet.getObject("id", UUID.class);
    ContactAccumulator contact =
        contacts.computeIfAbsent(contactId, ignored -> accumulator(resultSet));
    UUID channelId = resultSet.getObject("channel_id", UUID.class);
    if (channelId != null) {
      contact.channels.add(
          new ChannelView(
              channelId,
              resultSet.getLong("channel_version"),
              ContactChannelType.valueOf(resultSet.getString("type")),
              resultSet.getString("value"),
              resultSet.getString("normalized_value"),
              resultSet.getBoolean("primary_channel"),
              resultSet.getBoolean("valid"),
              resultSet.getBoolean("channel_verified"),
              resultSet.getString("channel_consent"),
              resultSet.getBoolean("preferred"),
              instant(resultSet, "channel_validated_at")));
    }
  }

  private ContactAccumulator accumulator(ResultSet resultSet) {
    try {
      return new ContactAccumulator(
          resultSet.getObject("id", UUID.class),
          resultSet.getLong("version"),
          resultSet.getString("first_name"),
          resultSet.getString("last_name"),
          resultSet.getString("name"),
          resultSet.getString("role"),
          resultSet.getBoolean("primary_contact"),
          resultSet.getBoolean("verified"),
          nullableChannel(resultSet.getString("preferred_channel")),
          resultSet.getString("consent"),
          resultSet.getString("source"),
          instant(resultSet, "last_validated_at"),
          instant(resultSet, "created_at"),
          instant(resultSet, "updated_at"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Unable to map contact", exception);
    }
  }

  private void validateContact(String firstName, String lastName, List<ChannelCommand> channels) {
    if (trim(firstName) == null
        && trim(lastName) == null
        && (channels == null || channels.isEmpty())) {
      throw new IllegalArgumentException("A contact requires a name or channel");
    }
  }

  private String normalize(ContactChannelType type, String value) {
    String normalized = normalizationService.normalizeChannel(type, value);
    if (normalized == null) {
      throw new IllegalArgumentException("Channel value is required");
    }
    return normalized;
  }

  private void rejectDuplicate(ContactChannelType type, String normalized, UUID currentId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM contact_channel
            WHERE organization_id = ? AND type = ? AND normalized_value = ?
              AND (?::uuid IS NULL OR id <> ?::uuid)
            """,
            Integer.class,
            currentActor.organizationId(),
            type.name(),
            normalized,
            currentId,
            currentId);
    if (count != null && count > 0) {
      throw new DuplicateResourceException("Contact channel already exists: " + type);
    }
  }

  private void clearPrimary(UUID institutionId) {
    jdbcTemplate.update(
        """
        UPDATE contact SET primary_contact = FALSE, updated_at = now(), version = version + 1
        WHERE organization_id = ? AND institution_id = ? AND primary_contact = TRUE
        """,
        currentActor.organizationId(),
        institutionId);
  }

  private void conflict(UUID contactId, long expected) {
    Long actual =
        jdbcTemplate
            .query(
                "SELECT version FROM contact WHERE id = ? AND organization_id = ? AND deleted_at IS NULL",
                (resultSet, rowNumber) -> resultSet.getLong(1),
                contactId,
                currentActor.organizationId())
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Contact not found: " + contactId));
    throw new OptimisticConflictException(
        "Contact was modified by another user (expected " + expected + ", actual " + actual + ")");
  }

  private String consent(String value) {
    String result = value == null ? "UNKNOWN" : value.trim().toUpperCase(Locale.ROOT);
    if (!CONSENT.contains(result)) {
      throw new IllegalArgumentException("Consent must be UNKNOWN, GRANTED or DENIED");
    }
    return result;
  }

  private String displayName(String firstName, String lastName) {
    String result =
        String.join(
                " ",
                trim(firstName) == null ? "" : firstName.trim(),
                trim(lastName) == null ? "" : lastName.trim())
            .trim();
    return result.isEmpty() ? null : result;
  }

  private String trim(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private java.sql.Timestamp timestamp(Instant value) {
    return value == null ? null : java.sql.Timestamp.from(value);
  }

  private Instant instant(ResultSet resultSet, String column) throws SQLException {
    OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  private ContactChannelType nullableChannel(String value) {
    return value == null ? null : ContactChannelType.valueOf(value);
  }

  private static final class ContactAccumulator {
    private final UUID id;
    private final long version;
    private final String firstName;
    private final String lastName;
    private final String displayName;
    private final String role;
    private final boolean primary;
    private final boolean verified;
    private final ContactChannelType preferredChannel;
    private final String consent;
    private final String source;
    private final Instant lastValidatedAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<ChannelView> channels = new ArrayList<>();

    private ContactAccumulator(
        UUID id,
        long version,
        String firstName,
        String lastName,
        String displayName,
        String role,
        boolean primary,
        boolean verified,
        ContactChannelType preferredChannel,
        String consent,
        String source,
        Instant lastValidatedAt,
        Instant createdAt,
        Instant updatedAt) {
      this.id = id;
      this.version = version;
      this.firstName = firstName;
      this.lastName = lastName;
      this.displayName = displayName;
      this.role = role;
      this.primary = primary;
      this.verified = verified;
      this.preferredChannel = preferredChannel;
      this.consent = consent;
      this.source = source;
      this.lastValidatedAt = lastValidatedAt;
      this.createdAt = createdAt;
      this.updatedAt = updatedAt;
    }

    private ContactView view() {
      return new ContactView(
          id,
          version,
          firstName,
          lastName,
          displayName,
          role,
          primary,
          verified,
          preferredChannel,
          consent,
          source,
          lastValidatedAt,
          createdAt,
          updatedAt,
          List.copyOf(channels));
    }
  }

  private record ChannelReference(UUID contactId, UUID prospectId, long version) {}

  public record ChannelCommand(
      ContactChannelType type,
      String value,
      boolean primary,
      boolean valid,
      boolean verified,
      String consent,
      boolean preferred,
      Instant lastValidatedAt) {}

  public record CreateContactCommand(
      String firstName,
      String lastName,
      String role,
      boolean primary,
      boolean verified,
      ContactChannelType preferredChannel,
      String consent,
      String source,
      Instant lastValidatedAt,
      List<ChannelCommand> channels) {}

  public record UpdateContactCommand(
      long version,
      String firstName,
      String lastName,
      String role,
      boolean primary,
      boolean verified,
      ContactChannelType preferredChannel,
      String consent,
      String source,
      Instant lastValidatedAt) {}

  public record UpdateChannelCommand(
      long version,
      ContactChannelType type,
      String value,
      boolean primary,
      boolean valid,
      boolean verified,
      String consent,
      boolean preferred,
      Instant lastValidatedAt) {}

  public record ContactView(
      UUID id,
      long version,
      String firstName,
      String lastName,
      String displayName,
      String role,
      boolean primary,
      boolean verified,
      ContactChannelType preferredChannel,
      String consent,
      String source,
      Instant lastValidatedAt,
      Instant createdAt,
      Instant updatedAt,
      List<ChannelView> channels) {}

  public record ChannelView(
      UUID id,
      long version,
      ContactChannelType type,
      String value,
      String normalizedValue,
      boolean primary,
      boolean valid,
      boolean verified,
      String consent,
      boolean preferred,
      Instant lastValidatedAt) {}
}
