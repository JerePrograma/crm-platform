package com.gestudio.crm.prospect;

public enum ProspectStatus {
  NEW,
  QUALIFYING,
  READY_TO_CONTACT,
  FOLLOW_UP,
  DEMO_PROPOSED,
  DEMO_SCHEDULED,
  PROPOSAL,
  CUSTOMER,
  // Legacy/data-quality states remain readable while V7 maps active lifecycle values.
  NEEDS_ENRICHMENT,
  READY_FOR_REVIEW,
  APPROVED,
  QUEUED,
  CONTACTED,
  REPLIED,
  INTERESTED,
  QUALIFIED,
  TRIAL_PROPOSED,
  TRIAL_ACTIVE,
  QUOTED,
  NEGOTIATION,
  WON,
  LOST,
  NO_RESPONSE,
  BOUNCED,
  UNSUBSCRIBED,
  DO_NOT_CONTACT,
  INVALID,
  DUPLICATE,
  ARCHIVED
}
