package com.gestudio.crm.settings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SendingPropertiesTest {

  @Test
  void defaultsUsedByTheApplicationBlockRealSending() {
    SendingProperties properties = new SendingProperties(false, true, 0, true);

    assertThat(properties.blocksRealSending()).isTrue();
  }

  @Test
  void everyGuardMustBeOpenBeforeRealSendingCouldBeConsidered() {
    SendingProperties properties = new SendingProperties(true, false, 1, false);

    assertThat(properties.blocksRealSending()).isFalse();
  }
}
