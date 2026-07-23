import { describe, expect, it } from "vitest";
import {
  auditSummary,
  duplicateSourceSummary,
  humanizeError,
  labelFor,
  safeTechnicalJson,
  suggestedDuplicateName,
} from "./uiLabels";

describe("etiquetas visibles para el operador", () => {
  it("traduce estados técnicos frecuentes", () => {
    expect(labelFor("READY_TO_CONTACT")).toBe("Listo para contactar");
    expect(labelFor("REVIEW_REQUIRED")).toBe("Requiere revisión");
    expect(labelFor("DEAD")).toBe("Agotó los reintentos");
  });

  it("convierte conflictos técnicos en una acción comprensible", () => {
    expect(humanizeError("Duplicate review was already resolved")).toContain("ya fue resuelta");
  });

  it("resume evidencia importada sin mostrar el JSON como contenido principal", () => {
    const source = JSON.stringify({ institucion: "Flores", email: "hola@flores.test", localidad: "Palermo" });
    expect(suggestedDuplicateName(source)).toBe("Flores");
    expect(duplicateSourceSummary(source)).toEqual([
      ["Institución", "Flores"],
      ["Correo", "hola@flores.test"],
      ["Localidad", "Palermo"],
    ]);
  });

  it("oculta claves sensibles en el detalle técnico", () => {
    const technical = safeTechnicalJson(JSON.stringify({ token: "secret", status: "PENDING" }));
    expect(technical).not.toContain("secret");
    expect(auditSummary(JSON.stringify({ status: "PENDING" }))).toContain("Pendiente");
  });
});
