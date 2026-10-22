import { afterEach, describe, expect, it, vi } from "vitest";
import { getBusinessProfile, getImportRows, getProspectDashboardMetrics, getTaskPage, listProspects, listTasks } from "./api";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("getTaskPage", () => {
  it("requests pending tasks with server-side ownership and date filters and preserves all page totals", async () => {
    const response = { content: [], totalElements: 205, totalPages: 9, number: 8, size: 25, first: false, last: true };
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify(response), {
      status: 200, headers: { "content-type": "application/json" },
    }));
    await expect(getTaskPage({
      statuses: ["OPEN", "IN_PROGRESS"], ownerUserId: "marketing", page: 8, size: 25,
      dueFrom: "2026-10-22T03:00:00.000Z", dueUntil: "2026-10-23T03:00:00.000Z",
    })).resolves.toEqual(response);
    const [url, init] = fetchMock.mock.calls[0]!;
    const parameters = new URL(String(url), "http://localhost").searchParams;
    expect(String(url)).toContain("/api/v1/tasks/page?");
    expect(parameters.getAll("statuses")).toEqual(["OPEN", "IN_PROGRESS"]);
    expect(parameters.get("ownerUserId")).toBe("marketing");
    expect(parameters.get("page")).toBe("8");
    expect(parameters.get("dueFrom")).toBe("2026-10-22T03:00:00.000Z");
    expect(parameters.get("dueUntil")).toBe("2026-10-23T03:00:00.000Z");
    expect(init).toMatchObject({ credentials: "same-origin" });
  });

  it("requests only the selected prospect's tasks without a global task batch or a client-side filter", async () => {
    const response = { content: [], totalElements: 230, totalPages: 10, number: 9, size: 25, first: false, last: true };
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify(response), { status: 200 }));
    await expect(getTaskPage({ prospectId: "selected-prospect", page: 9 })).resolves.toEqual(response);
    const parameters = new URL(String(fetchMock.mock.calls[0]![0]), "http://localhost").searchParams;
    expect(parameters.get("prospectId")).toBe("selected-prospect");
    expect(parameters.get("page")).toBe("9");
    expect(parameters.has("statuses")).toBe(false);
  });

  it("bounds page sizes and preserves the legacy array endpoint", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockImplementation(async () => new Response("[]", { status: 200 }));
    await getTaskPage({ page: -3, size: 500 });
    expect(String(fetchMock.mock.calls[0]![0])).toContain("page=0&size=100");
    await listTasks("OPEN");
    expect(fetchMock.mock.calls[1]).toEqual(["/api/v1/tasks?status=OPEN", expect.objectContaining({ credentials: "same-origin" })]);
  });
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

describe("getBusinessProfile", () => {
  it("lee el perfil de la organización autenticada con la sesión existente", async () => {
    const response = { code: "SCALARIS", serviceLines: ["Servicio técnico", "Desarrollo", "Venta a pedido"], manualCommunicationsOnly: true };
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify(response), {
      status: 200, headers: { "content-type": "application/json" },
    }));
    await expect(getBusinessProfile()).resolves.toEqual(response);
    expect(fetchMock.mock.calls[0]).toEqual(["/api/v1/business-profile", expect.objectContaining({ credentials: "same-origin" })]);
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
