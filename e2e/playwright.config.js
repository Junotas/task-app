import { defineConfig, devices } from "@playwright/test";

const baseURL = process.env.BASE_URL || "http://localhost:8080";
const startLocalServer = !process.env.BASE_URL;

export default defineConfig({
    testDir: "./tests",
    fullyParallel: false,
    workers: 1,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 2 : 0,
    reporter: process.env.CI ? [["html"], ["list"]] : [["list"]],

    use: {
        baseURL,
        trace: "on-first-retry",
        screenshot: "only-on-failure",
    },

    projects: [
        {
            name: "chromium",
            use: { ...devices["Desktop Chrome"] },
        },
    ],

    webServer: startLocalServer
        ? {
              command: "java -jar ../backend/target/task-app-0.0.1-SNAPSHOT.jar",
              url: "http://localhost:8080/api/tasks",
              reuseExistingServer: true,
              timeout: 120_000,
          }
        : undefined,
});