package com.gestudio.crm.contact;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactChannelRepository extends JpaRepository<ContactChannel, UUID> {

  Optional<ContactChannel> findByTypeAndNormalizedValue(
      ContactChannelType type, String normalizedValue);

  Optional<ContactChannel> findByOrganizationIdAndTypeAndNormalizedValue(
      UUID organizationId, ContactChannelType type, String normalizedValue);

  boolean existsByTypeAndNormalizedValue(ContactChannelType type, String normalizedValue);

  boolean existsByOrganizationIdAndTypeAndNormalizedValue(
      UUID organizationId, ContactChannelType type, String normalizedValue);
}
