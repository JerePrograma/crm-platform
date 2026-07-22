package com.gestudio.crm.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InboundMessageNormalizerTest {

  private final InboundMessageNormalizer normalizer = new InboundMessageNormalizer();

  @Test
  void normalizesSupportedContactsAndLimitsStoredExcerpt() {
    assertThat(normalizer.contact("EMAIL", " Director@Example.TEST "))
        .isEqualTo("director@example.test");
    assertThat(normalizer.contact("WHATSAPP", "00 54 (341) 555-0101")).isEqualTo("+543415550101");
    assertThat(normalizer.excerpt("x".repeat(700))).hasSize(500);
  }

  @Test
  void rejectsUnsupportedChannel() {
    assertThatThrownBy(() -> normalizer.contact("SMS", "123"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
