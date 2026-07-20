package com.gestudio.crm.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.contact.ContactChannelType;
import org.junit.jupiter.api.Test;

class NormalizationServiceTest {

  private final NormalizationService service = new NormalizationService();

  @Test
  void normalizesNamesWithoutDiacriticsOrPunctuation() {
    assertThat(service.normalizeName("  Estúdio  de Danzas—Aurora "))
        .isEqualTo("estudio de danzas aurora");
  }

  @Test
  void normalizesEmailPhoneAndDomain() {
    assertThat(service.normalizeEmail(" Secretaria@Example.COM "))
        .isEqualTo("secretaria@example.com");
    assertThat(service.normalizePhone("+54 9 (11) 5555-1212"))
        .isEqualTo("5491155551212");
    assertThat(service.normalizeDomain("https://www.Example.com/contacto"))
        .isEqualTo("example.com");
  }

  @Test
  void rejectsMalformedEmailChannelsBeforePersistence() {
    assertThatThrownBy(() -> service.normalizeEmail("not-an-email"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid email address");
  }

  @Test
  void dispatchesChannelNormalizationByType() {
    assertThat(service.normalizeChannel(ContactChannelType.WHATSAPP, "+54 11 4444 5555"))
        .isEqualTo("541144445555");
  }
}
