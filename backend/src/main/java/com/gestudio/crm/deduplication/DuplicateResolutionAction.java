package com.gestudio.crm.deduplication;

public enum DuplicateResolutionAction {
  MARK_NOT_DUPLICATE,
  LINK_TO_EXISTING,
  MERGE,
  CREATE_SEPARATE,
  REJECT_ROW,
  DEFER
}
