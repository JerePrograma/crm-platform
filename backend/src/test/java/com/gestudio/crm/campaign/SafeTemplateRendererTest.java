package com.gestudio.crm.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.common.UnprocessableEntityException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SafeTemplateRendererTest {

  private final SafeTemplateRenderer renderer = new SafeTemplateRenderer();

  @Test
  void escapesVariablesInHtmlAndDoesNotInterpretText() {
    var rendered =
        renderer.render(
            "Hola {{contact.firstName}}",
            "{{contact.firstName}}",
            "<p>{{contact.firstName}}</p>",
            Map.of("contact.firstName", "<img src=x onerror=alert(1)>"));
    assertThat(rendered.subject()).contains("<img");
    assertThat(rendered.textBody()).contains("<img");
    assertThat(rendered.htmlBody())
        .isEqualTo("<p>&lt;img src=x onerror=alert(1)&gt;</p>")
        .doesNotContain("<img");
  }

  @Test
  void rejectsActiveHtmlAndArbitraryExpressions() {
    assertThatThrownBy(
            () ->
                renderer.render(
                    "Subject", "Text", "<a href=\"javascript:alert(1)\">x</a>", Map.of()))
        .isInstanceOf(UnprocessableEntityException.class);
    assertThatThrownBy(() -> renderer.variables("{{ prospect.displayName }} and {{bad value}}"))
        .isInstanceOf(UnprocessableEntityException.class);
  }
}
