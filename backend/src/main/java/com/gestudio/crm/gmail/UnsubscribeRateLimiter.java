package com.gestudio.crm.gmail;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
final class UnsubscribeRateLimiter {

  private static final int REQUESTS_PER_MINUTE = 30;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  UnsubscribeRateLimiter(Clock clock) {
    this.clock = clock;
  }

  void requireAllowed(String key) {
    Instant minute = clock.instant().truncatedTo(ChronoUnit.MINUTES);
    if (windows.size() > 10_000) {
      windows.entrySet().removeIf(entry -> entry.getValue().minute().isBefore(minute));
    }
    Window window =
        windows.compute(
            key,
            (ignored, current) ->
                current == null || current.minute().isBefore(minute)
                    ? new Window(minute, 1)
                    : new Window(minute, current.count() + 1));
    if (window.count() > REQUESTS_PER_MINUTE) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Unsubscribe rate limit exceeded");
    }
  }

  private record Window(Instant minute, int count) {}
}
