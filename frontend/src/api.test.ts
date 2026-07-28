import { afterEach, describe, expect, it, vi } from "vitest";
import { getImportRows, getProspectDashboardMetrics, listProspects } from "./api";

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

describe("getImportRows", () => {
  it("requests only one filtered import-result page", async () => {
    const response = {
      content: [],
      totalElements: 121,
      totalPages: 3,
      number: 1,
      size: 50,
      first: false,
      last: false,
      sourceSheets: ["Exclusiones", "Prospectos"],
    };
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(
        new Response(JSON.stringify(response), {
          status: 200,
          headers: { "content-type": "application/json" },
        }),
      );

    await expect(
      getImportRows("job-id", {
        status: "ACCEPTED",
        sourceSheet: "Prospectos",
        query: "contacto105@example.test",
        page: 1,
        size: 50,
      }),
    ).resolves.toEqual(response);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0]!;
    expect(String(url)).toContain("/api/v1/imports/prospects/job-id/rows?");
    expect(String(url)).toContain("page=1");
    expect(String(url)).toContain("size=50");
    expect(String(url)).toContain("status=ACCEPTED");
    expect(String(url)).toContain("sourceSheet=Prospectos");
    expect(String(url)).toContain("query=contacto105%40example.test");
    expect(init).toMatchObject({ credentials: "same-origin" });
  });

  it("caps the requested page size at one hundred rows", async () => {
    const response = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 100,
      first: true,
      last: true,
      sourceSheets: [],
    };
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(
        new Response(JSON.stringify(response), {
          status: 200,
          headers: { "content-type": "application/json" },
        }),
      );

    await getImportRows("job-id", { page: -2, size: 500 });

    const [url] = fetchMock.mock.calls[0]!;
    expect(String(url)).toContain("page=0");
    expect(String(url)).toContain("size=100");
  });
});