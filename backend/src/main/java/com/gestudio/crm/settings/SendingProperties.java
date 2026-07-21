package com.gestudio.crm.settings;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sending")
public record SendingProperties(
    boolean enabled, boolean dryRun, @Min(0) int dailyLimit, boolean environmentKillSwitch) {

  public boolean blocksRealSending() {
    return !enabled || dryRun || dailyLimit <= 0 || environmentKillSwitch;
  }
}
