package com.gestudio.crm.messaging;

import java.util.Map;
import java.util.Set;

public interface MessageRenderer {
  Set<String> variables(String... sources);

  RenderedMessage render(
      String subject, String textBody, String htmlBody, Map<String, String> values);

  record RenderedMessage(String subject, String textBody, String htmlBody) {}
}
