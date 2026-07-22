package com.gestudio.crm.inbound;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class InboundRateLimiter {

  private final Clock clock;
  private final InboundProperties properties;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public InboundRateLimiter(Clock clock, InboundProperties properties) {
    this.clock = clock;
    this.properties = properties;
  }

  public void requireAllowed(String key) {
    Instant minute = clock.instant().truncatedTo(ChronoUnit.MINUTES);
    Window window =
        windows.compute(
            key,
            (ignored, current) -> {
              if (current == null || !current.minute().equals(minute)) {
                return new Window(minute, 1);
              }
              return new Window(minute, current.count() + 1);
            });
    if (window.count() > properties.requestsPerMinute()) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Webhook rate limit exceeded");
    }
  }

  private record Window(Instant minute, int count) {}
}
