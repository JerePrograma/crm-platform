package com.gestudio.crm.inbound;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class InboundMessageNormalizer {

  public String contact(String channel, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Inbound sender is required");
    }
    String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC);
    return switch (channel) {
      case "EMAIL" -> normalized.toLowerCase(Locale.ROOT);
      case "WHATSAPP" -> {
        String phone = normalized.replaceAll("[^0-9+]", "");
        if (phone.startsWith("00")) {
          phone = "+" + phone.substring(2);
        }
        yield phone;
      }
      default -> throw new IllegalArgumentException("Inbound channel is invalid");
    };
  }

  public String excerpt(String value) {
    if (value == null || value.isBlank()) {
      return "Respuesta recibida";
    }
    String sanitized = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
    return sanitized.substring(0, Math.min(sanitized.length(), 500));
  }
}
