package com.gestudio.crm.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class OutboxWorkerServiceTest {

  @Test
  void computesDeterministicExponentialBackoffWithACap() {
    OutboxProperties properties =
        new OutboxProperties(
            false,
            10,
            Duration.ofMinutes(1),
            Duration.ofSeconds(5),
            Duration.ofSeconds(20),
            5,
            65536);
    OutboxWorkerService worker =
        new OutboxWorkerService(
            mock(JdbcTemplate.class),
            mock(TransactionTemplate.class),
            mock(OutboxEventProcessor.class),
            properties,
            Clock.systemUTC(),
            new SimpleMeterRegistry());
    UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000123");

    assertThat(worker.backoff(eventId, 1)).isEqualTo(worker.backoff(eventId, 1));
    assertThat(worker.backoff(eventId, 2)).isGreaterThan(worker.backoff(eventId, 1));
    assertThat(worker.backoff(eventId, 20)).isEqualTo(Duration.ofSeconds(20));
  }
}
