package com.gestudio.crm.exclusion;

import com.gestudio.crm.contact.ContactChannelType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExclusionRepository extends JpaRepository<Exclusion, UUID> {

  boolean existsByChannelTypeAndNormalizedValue(
      ContactChannelType channelType, String normalizedValue);

  Optional<Exclusion> findByChannelTypeAndNormalizedValue(
      ContactChannelType channelType, String normalizedValue);
}
