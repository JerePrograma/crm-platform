package com.gestudio.crm.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class UnsubscribeTokenServiceTest {

  @Test
  void issuesOpaqueLookupTokenAndVerifiesOnlyItsBoundContext() throws Exception {
    UnsubscribeTokenService service =
        new UnsubscribeTokenService(GmailTestProperties.properties("https://example.test"));
    UUID tokenId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID messageId = UUID.randomUUID();

    var issued = service.issue(tokenId, organizationId, messageId);

    assertThat(issued.opaqueToken()).startsWith(tokenId + ".").doesNotContain("?");
    assertThat(issued.publicUri().getPath()).endsWith(issued.opaqueToken());
    assertThat(service.tokenId(issued.opaqueToken())).isEqualTo(tokenId);
    assertThat(
            service.verify(
                organizationId,
                messageId,
                issued.opaqueToken(),
                issued.tokenHash(),
                issued.keyId()))
        .isTrue();
    assertThat(
            service.verify(
                UUID.randomUUID(),
                messageId,
                issued.opaqueToken(),
                issued.tokenHash(),
                issued.keyId()))
        .isFalse();
    assertThat(service.tokenId("not-an-opaque-token")).isNull();
    assertThat(issued.toString()).doesNotContain(issued.opaqueToken()).contains("REDACTED");
    assertThat(new ObjectMapper().writeValueAsString(issued))
        .doesNotContain(issued.opaqueToken(), issued.tokenHash());
  }

  @Test
  void suppressionHashIsStableAndDomainSeparated() {
    UnsubscribeTokenService service =
        new UnsubscribeTokenService(GmailTestProperties.properties("https://example.test"));
    UUID tokenId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID messageId = UUID.randomUUID();
    var issued = service.issue(tokenId, organizationId, messageId);

    assertThat(service.suppressionHash("synthetic@example.test"))
        .hasSize(64)
        .isEqualTo(service.suppressionHash("synthetic@example.test"))
        .isNotEqualTo(issued.tokenHash());
  }
}
