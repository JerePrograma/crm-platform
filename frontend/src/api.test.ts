import { afterEach, describe, expect, it, vi } from "vitest";
import { getProspectDashboardMetrics, listProspects } from "./api";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("listProspects", () => {
  it("sends pagination and filters without changing the response contract", async () => {
    const response = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 2,
      size: 50,
      first: false,
      last: true,
    };
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(
        new Response(JSON.stringify(response), {
          status: 200,
          headers: { "content-type": "application/json" },
        }),
      );

    await expect(listProspects("READY_TO_CONTACT", "correo@example.test", 2, 50))
      .resolves.toEqual(response);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0]!;
    expect(String(url)).toContain("page=2");
    expect(String(url)).toContain("size=50");
    expect(String(url)).toContain("status=READY_TO_CONTACT");
    expect(String(url)).toContain("query=correo%40example.test");
    expect(init).toMatchObject({ credentials: "same-origin" });
  });
});
describe("getProspectDashboardMetrics", () => {
  it("loads tenant-wide dashboard counts from the aggregate endpoint", async () => {
    const response = { interested: 5, blocked: 98 };
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(
        new Response(JSON.stringify(response), {
          status: 200,
          headers: { "content-type": "application/json" },
        }),
      );

    await expect(getProspectDashboardMetrics()).resolves.toEqual(response);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0]!;
    expect(String(url)).toBe("/api/v1/prospects/metrics");
    expect(init).toMatchObject({ credentials: "same-origin" });
  });
});
