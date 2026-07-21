package com.gestudio.crm.prospect;

import com.gestudio.crm.common.UnprocessableEntityException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProspectLifecycle {

  private final Map<ProspectStatus, Set<ProspectStatus>> transitions =
      new EnumMap<>(ProspectStatus.class);

  public ProspectLifecycle() {
    allow(ProspectStatus.NEW, ProspectStatus.QUALIFYING, ProspectStatus.DO_NOT_CONTACT);
    allow(
        ProspectStatus.QUALIFYING,
        ProspectStatus.READY_TO_CONTACT,
        ProspectStatus.LOST,
        ProspectStatus.DO_NOT_CONTACT);
    allow(ProspectStatus.READY_TO_CONTACT, ProspectStatus.CONTACTED, ProspectStatus.DO_NOT_CONTACT);
    allow(
        ProspectStatus.CONTACTED,
        ProspectStatus.REPLIED,
        ProspectStatus.FOLLOW_UP,
        ProspectStatus.LOST,
        ProspectStatus.DO_NOT_CONTACT);
    allow(
        ProspectStatus.REPLIED,
        ProspectStatus.INTERESTED,
        ProspectStatus.FOLLOW_UP,
        ProspectStatus.LOST,
        ProspectStatus.DO_NOT_CONTACT);
    allow(
        ProspectStatus.INTERESTED,
        ProspectStatus.DEMO_PROPOSED,
        ProspectStatus.PROPOSAL,
        ProspectStatus.FOLLOW_UP,
        ProspectStatus.LOST,
        ProspectStatus.DO_NOT_CONTACT);
    allow(
        ProspectStatus.FOLLOW_UP,
        ProspectStatus.CONTACTED,
        ProspectStatus.INTERESTED,
        ProspectStatus.DEMO_PROPOSED,
        ProspectStatus.LOST,
        ProspectStatus.DO_NOT_CONTACT);
    allow(
        ProspectStatus.DEMO_PROPOSED,
        ProspectStatus.DEMO_SCHEDULED,
        ProspectStatus.FOLLOW_UP,
        ProspectStatus.LOST,
        ProspectStatus.DO_NOT_CONTACT);
    allow(
        ProspectStatus.DEMO_SCHEDULED,
        ProspectStatus.PROPOSAL,
        ProspectStatus.FOLLOW_UP,
        ProspectStatus.LOST,
        ProspectStatus.DO_NOT_CONTACT);
    allow(
        ProspectStatus.PROPOSAL,
        ProspectStatus.NEGOTIATION,
        ProspectStatus.LOST,
        ProspectStatus.DO_NOT_CONTACT);
    allow(
        ProspectStatus.NEGOTIATION,
        ProspectStatus.CUSTOMER,
        ProspectStatus.LOST,
        ProspectStatus.DO_NOT_CONTACT);
    allow(ProspectStatus.LOST, ProspectStatus.FOLLOW_UP, ProspectStatus.DO_NOT_CONTACT);
    allow(ProspectStatus.CUSTOMER);
    allow(ProspectStatus.DO_NOT_CONTACT, ProspectStatus.QUALIFYING);
  }

  public void requireAllowed(ProspectStatus from, ProspectStatus to) {
    if (from == null
        || to == null
        || from == to
        || !transitions.getOrDefault(from, Set.of()).contains(to)) {
      throw new UnprocessableEntityException(
          "Transition from " + from + " to " + to + " is not allowed");
    }
  }

  public Set<ProspectStatus> allowedFrom(ProspectStatus status) {
    return transitions.getOrDefault(status, Set.of());
  }

  private void allow(ProspectStatus from, ProspectStatus... to) {
    transitions.put(from, Set.of(to));
  }
}
