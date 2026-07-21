package com.gestudio.crm.exclusion;

import com.gestudio.crm.contact.ContactChannelType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExclusionRepository
    extends JpaRepository<Exclusion, UUID>, JpaSpecificationExecutor<Exclusion> {

  boolean existsByChannelTypeAndNormalizedValue(
      ContactChannelType channelType, String normalizedValue);

  boolean existsByOrganizationIdAndChannelTypeAndNormalizedValue(
      UUID organizationId, ContactChannelType channelType, String normalizedValue);

  Optional<Exclusion> findByChannelTypeAndNormalizedValue(
      ContactChannelType channelType, String normalizedValue);

  Optional<Exclusion> findByOrganizationIdAndChannelTypeAndNormalizedValue(
      UUID organizationId, ContactChannelType channelType, String normalizedValue);

  Optional<Exclusion> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
