package com.gestudio.crm.settings;

import com.gestudio.crm.settings.OrganizationSettingsService.SettingsView;
import com.gestudio.crm.settings.OrganizationSettingsService.UpdateCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class OrganizationSettingsController {

  private final OrganizationSettingsService service;

  public OrganizationSettingsController(OrganizationSettingsService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('REPORT_READ')")
  public SettingsView get() {
    return service.get();
  }

  @PutMapping
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public SettingsView update(@Valid @RequestBody UpdateRequest request) {
    return service.update(request.command());
  }

  public record UpdateRequest(
      @PositiveOrZero long version,
      @NotBlank @Size(max = 200) String name,
      @NotBlank String timezone,
      @NotBlank @Size(min = 3, max = 3) String currency,
      @NotBlank String locale,
      @NotBlank String brandingPrimaryColor,
      @Min(1) @Max(365) int followUpDays,
      @NotBlank String operatingWindowStart,
      @NotBlank String operatingWindowEnd,
      @NotEmpty List<@NotNull @Min(1) @Max(7) Integer> businessDays,
      Boolean sendingEnabled,
      Boolean sendingDryRun,
      @Min(0) Integer sendingDailyLimit,
      Boolean sendingKillSwitch) {
    UpdateCommand command() {
      return new UpdateCommand(
          version,
          name,
          timezone,
          currency,
          locale,
          brandingPrimaryColor,
          followUpDays,
          operatingWindowStart,
          operatingWindowEnd,
          businessDays,
          sendingEnabled,
          sendingDryRun,
          sendingDailyLimit,
          sendingKillSwitch);
    }
  }
}
