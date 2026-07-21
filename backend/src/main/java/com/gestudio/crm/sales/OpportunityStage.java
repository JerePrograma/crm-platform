package com.gestudio.crm.sales;

public enum OpportunityStage {
  QUALIFICATION,
  DISCOVERY,
  DEMO,
  PROPOSAL,
  NEGOTIATION,
  WON,
  LOST;

  public boolean terminal() {
    return this == WON || this == LOST;
  }
}
