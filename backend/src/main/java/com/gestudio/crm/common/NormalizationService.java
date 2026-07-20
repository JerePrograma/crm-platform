package com.gestudio.crm.common;

import com.gestudio.crm.contact.ContactChannelType;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class NormalizationService {

  private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
  private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");
  private static final Pattern NON_DIGIT = Pattern.compile("\\D+");
  private static final Pattern EMAIL =
      Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", Pattern.CASE_INSENSITIVE);

  public String normalizeName(String value) {
    String normalized = normalizeText(value);
    if (normalized == null) {
      throw new IllegalArgumentException("Name is required");
    }
    return normalized;
  }

  public String normalizeEmail(String value) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      return null;
    }
    String lowerCase = normalized.toLowerCase(Locale.ROOT);
    if (!EMAIL.matcher(lowerCase).matches()) {
      throw new IllegalArgumentException("Invalid email address");
    }
    return lowerCase;
  }

  public String normalizePhone(String value) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      return null;
    }
    String digits = NON_DIGIT.matcher(normalized).replaceAll("");
    return digits.isBlank() ? null : digits;
  }

  public String normalizeDomain(String value) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      return null;
    }
    String candidate = normalized.contains("://") ? normalized : "https://" + normalized;
    try {
      String host = new URI(candidate).getHost();
      if (host == null || host.isBlank()) {
        throw new IllegalArgumentException("Invalid website or domain");
      }
      host = host.toLowerCase(Locale.ROOT);
      return host.startsWith("www.") ? host.substring(4) : host;
    } catch (URISyntaxException exception) {
      throw new IllegalArgumentException("Invalid website or domain", exception);
    }
  }

  public String normalizeChannel(ContactChannelType type, String value) {
    if (type == null) {
      throw new IllegalArgumentException("Channel type is required");
    }
    return switch (type) {
      case EMAIL -> normalizeEmail(value);
      case PHONE, WHATSAPP -> normalizePhone(value);
      case WEBSITE -> normalizeDomain(value);
      case SOCIAL -> normalizeSocial(value);
    };
  }

  public String normalizeText(String value) {
    String trimmed = trimToNull(value);
    if (trimmed == null) {
      return null;
    }
    String decomposed = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
    String withoutDiacritics = DIACRITICS.matcher(decomposed).replaceAll("");
    String lowerCase = withoutDiacritics.toLowerCase(Locale.ROOT);
    String separated = NON_ALPHANUMERIC.matcher(lowerCase).replaceAll(" ");
    return WHITESPACE.matcher(separated).replaceAll(" ").trim();
  }

  private String normalizeSocial(String value) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      return null;
    }
    String lowerCase = normalized.toLowerCase(Locale.ROOT);
    while (lowerCase.endsWith("/")) {
      lowerCase = lowerCase.substring(0, lowerCase.length() - 1);
    }
    return lowerCase;
  }

  public String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
