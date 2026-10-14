package com.gestudio.crm.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.campaign.SafeTemplateRenderer;
import com.gestudio.crm.common.TenantIds;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BusinessProfilePropertiesTest {

  @Test
  void preservesLegacyByDefaultAndSelectsAnIndependentScalarisTenant() {
    var legacy = new BusinessProfileProperties(null, null, null, null);
    assertThat(legacy.organizationId()).isEqualTo(TenantIds.BOOTSTRAP_ORGANIZATION_ID);
    assertThat(legacy.marketingConfigured()).isFalse();
    var scalaris =
        new BusinessProfileProperties(" scalaris ", "marketing", "synthetic-secret-1", null);
    assertThat(scalaris.organizationId())
        .isEqualTo(BusinessProfileProperties.SCALARIS_ORGANIZATION_ID)
        .isNotEqualTo(TenantIds.BOOTSTRAP_ORGANIZATION_ID);
    assertThat(scalaris.marketingConfigured()).isTrue();
    assertThat(scalaris.effectiveMarketingDisplayName()).isEqualTo("Marketing Scalaris");
  }

  @Test
  void rejectsUnknownProfilesAndPartialCredentials() {
    assertThatThrownBy(() -> new BusinessProfileProperties("unknown", null, null, null).code())
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new BusinessProfileProperties("SCALARIS", "operator", "", null)
                    .marketingConfigured())
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new BusinessProfileProperties("SCALARIS", "", "synthetic-secret-1", null)
                    .marketingConfigured())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void marketingPermissionsExcludeTechnicalAdministrationAndRealDelivery() {
    assertThat(ScalarisCatalog.MARKETING_PERMISSIONS)
        .contains(
            "IMPORT_EXECUTE",
            "CAMPAIGN_WRITE",
            "CAMPAIGN_APPROVE",
            "MESSAGE_DRAFT",
            "MESSAGE_SIMULATE",
            "REPORT_READ")
        .doesNotContain(
            "USER_MANAGE",
            "SETTINGS_MANAGE",
            "MESSAGE_SEND",
            "AUDIT_READ",
            "INTEGRATION_MANAGE",
            "OUTBOX_MANAGE");
  }

  @Test
  void allStarterTemplatesRenderWithoutMissingContactVariables() {
    SafeTemplateRenderer renderer = new SafeTemplateRenderer();
    for (var template : ScalarisCatalog.TEMPLATES) {
      assertThat(renderer.render(template.subject(), template.body(), "", Map.of()).textBody())
          .isNotBlank();
    }
    assertThat(ScalarisCatalog.TEMPLATES.stream().filter(t -> t.channel().equals("EMAIL")))
        .allSatisfy(t -> assertThat(t.body()).contains(ScalarisCatalog.WHATSAPP_DISPLAY));
  }
}
