import {defineConfig, devices} from "@playwright/test";

const deployedUrl = process.env.BASE_URL;
const backendPort = 8080;
const frontendPort = 5173;

export default defineConfig({
    testDir: "./tests",
    fullyParallel: false,
    workers: 1,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 2 : 0,
    reporter: process.env.CI ? [["html"], ["list"]] : [["list"]],
    globalTeardown: "./scripts/stop-local-servers.js",

    use: {
        baseURL: deployedUrl || `http://localhost:${frontendPort}`,
        trace: "on-first-retry",
        screenshot: "only-on-failure",
    },

    projects: [
        {
            name: "chromium",
            use: {...devices["Desktop Chrome"]},
        },
    ],

    webServer: deployedUrl
        ? undefined
        : [
            {
                command: "java -jar ../backend/target/task-app-0.0.1-SNAPSHOT.jar",
                url: `http://localhost:${backendPort}/api/tasks`,
                env: {...process.env, ALLOWED_ORIGINS: `http://localhost:${frontendPort}`},
                reuseExistingServer: false,
                timeout: 120_000,
            },
            {
                command: "node ../frontend/dev-server.mjs",
                url: `http://localhost:${frontendPort}`,
                env: {...process.env, PORT: String(frontendPort)},
                reuseExistingServer: false,
                timeout: 30_000,
            },
        ],
});