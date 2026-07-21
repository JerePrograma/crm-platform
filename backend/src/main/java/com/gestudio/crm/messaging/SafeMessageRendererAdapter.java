package com.gestudio.crm.messaging;

import com.gestudio.crm.campaign.SafeTemplateRenderer;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SafeMessageRendererAdapter implements MessageRenderer {

  private final SafeTemplateRenderer renderer;

  public SafeMessageRendererAdapter(SafeTemplateRenderer renderer) {
    this.renderer = renderer;
  }

  @Override
  public Set<String> variables(String... sources) {
    return renderer.variables(sources);
  }

  @Override
  public RenderedMessage render(
      String subject, String textBody, String htmlBody, Map<String, String> values) {
    var rendered = renderer.render(subject, textBody, htmlBody, values);
    return new RenderedMessage(rendered.subject(), rendered.textBody(), rendered.htmlBody());
  }
}
