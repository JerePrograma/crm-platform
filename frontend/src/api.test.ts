import { describe, expect, it } from "vitest";
import { ApiError, dashboardCsvUrl, isConflict } from "./api";

describe("API safety helpers", () => {
  it("encodes report dates without accepting arbitrary query fragments", () => {
    expect(dashboardCsvUrl("2026-07-01", "2026-07-22")).toBe(
      "/api/v1/reports/dashboard.csv?from=2026-07-01&to=2026-07-22",
    );
  });

  it("recognizes only typed HTTP conflicts", () => {
    expect(isConflict(new ApiError(409, "stale"))).toBe(true);
    expect(isConflict(new ApiError(400, "invalid"))).toBe(false);
    expect(isConflict(new Error("409"))).toBe(false);
  });
});
