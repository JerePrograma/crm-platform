package com.gestudio.crm.prospect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.common.UnprocessableEntityException;
import org.junit.jupiter.api.Test;

class ProspectLifecycleTest {

  private final ProspectLifecycle lifecycle = new ProspectLifecycle();

  @Test
  void exposesOnlyExplicitForwardTransitions() {
    assertThat(lifecycle.allowedFrom(ProspectStatus.NEW))
        .containsExactlyInAnyOrder(ProspectStatus.QUALIFYING, ProspectStatus.DO_NOT_CONTACT);
    assertThat(lifecycle.allowedFrom(ProspectStatus.CUSTOMER)).isEmpty();

    lifecycle.requireAllowed(ProspectStatus.NEW, ProspectStatus.QUALIFYING);
    lifecycle.requireAllowed(ProspectStatus.NEGOTIATION, ProspectStatus.CUSTOMER);
  }

  @Test
  void rejectsSelfTransitionsAndArbitraryJumps() {
    assertThatThrownBy(() -> lifecycle.requireAllowed(ProspectStatus.NEW, ProspectStatus.CUSTOMER))
        .isInstanceOf(UnprocessableEntityException.class)
        .hasMessageContaining("is not allowed");
    assertThatThrownBy(
            () -> lifecycle.requireAllowed(ProspectStatus.CONTACTED, ProspectStatus.CONTACTED))
        .isInstanceOf(UnprocessableEntityException.class);
  }
}
