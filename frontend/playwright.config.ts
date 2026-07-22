import { defineConfig } from "@playwright/test";
import path from "node:path";

export default defineConfig({
  testDir: "./tests",
  timeout: 60_000,
  expect: { timeout: 10_000 },
  retries: 0,
  workers: 1,
  reporter: [["line"], ["json", { outputFile: path.resolve("../validation-output/complete-crm-e2e-latest.json") }]],
  outputDir: path.resolve("../validation-output/playwright"),
  use: {
    baseURL: process.env.CRM_E2E_BASE_URL ?? "http://127.0.0.1:5173",
    ...(process.env.CRM_E2E_BROWSER_CHANNEL === "bundled"
      ? {}
      : { channel: process.env.CRM_E2E_BROWSER_CHANNEL ?? "chrome" }),
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
});
