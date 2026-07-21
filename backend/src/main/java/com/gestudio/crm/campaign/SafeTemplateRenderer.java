package com.gestudio.crm.campaign;

import com.gestudio.crm.common.UnprocessableEntityException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SafeTemplateRenderer {

  public static final Set<String> ALLOWED_VARIABLES =
      Set.of(
          "prospect.displayName",
          "prospect.city",
          "contact.firstName",
          "contact.lastName",
          "owner.name",
          "campaign.name");

  private static final Pattern VARIABLE =
      Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9.]*)\\s*}}");
  private static final Pattern UNSAFE_HTML =
      Pattern.compile(
          "(?is)<\\s*(script|iframe|object|embed|link|meta|style)\\b|\\son[a-z]+\\s*=|javascript\\s*:");

  public Set<String> variables(String... sources) {
    Set<String> variables = new LinkedHashSet<>();
    for (String source : sources) {
      if (source == null) {
        continue;
      }
      Matcher matcher = VARIABLE.matcher(source);
      while (matcher.find()) {
        String variable = matcher.group(1);
        if (!ALLOWED_VARIABLES.contains(variable)) {
          throw new UnprocessableEntityException("Unsupported template variable: " + variable);
        }
        variables.add(variable);
      }
      String withoutValidVariables = VARIABLE.matcher(source).replaceAll("");
      if (withoutValidVariables.contains("{{") || withoutValidVariables.contains("}}")) {
        throw new UnprocessableEntityException("Template contains an invalid variable expression");
      }
    }
    return variables;
  }

  public RenderedTemplate render(
      String subject, String textBody, String htmlBody, Map<String, String> values) {
    Set<String> required = variables(subject, textBody, htmlBody);
    for (String variable : required) {
      if (values.get(variable) == null || values.get(variable).isBlank()) {
        throw new UnprocessableEntityException("Missing template variable: " + variable);
      }
    }
    if (htmlBody != null && UNSAFE_HTML.matcher(htmlBody).find()) {
      throw new UnprocessableEntityException("Template HTML contains unsafe content");
    }
    return new RenderedTemplate(
        replace(subject, values, false),
        replace(textBody, values, false),
        replace(htmlBody, values, true));
  }

  private String replace(String source, Map<String, String> values, boolean html) {
    if (source == null) {
      return "";
    }
    Matcher matcher = VARIABLE.matcher(source);
    StringBuilder output = new StringBuilder();
    while (matcher.find()) {
      String value = values.get(matcher.group(1));
      matcher.appendReplacement(output, Matcher.quoteReplacement(html ? escapeHtml(value) : value));
    }
    matcher.appendTail(output);
    return output.toString();
  }

  private String escapeHtml(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  public record RenderedTemplate(String subject, String textBody, String htmlBody) {}
}
