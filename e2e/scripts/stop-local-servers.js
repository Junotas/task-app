import { execSync } from "node:child_process";

const PORTS = [8080, 5173];

function listeningProcessIds(port) {
    let output;

    try {
        output = execSync("netstat -ano", { encoding: "utf8" });
    } catch {
        return [];
    }

    const ids = output
        .split(/\r?\n/)
        .map((line) => line.trim().split(/\s+/))
        .filter((columns) => columns.length >= 5)
        .filter((columns) => columns[1].endsWith(`:${port}`))
        .filter((columns) => /^(0\.0\.0\.0|\[::\]):0$/.test(columns[2]))
        .map((columns) => columns[columns.length - 1]);

    return [...new Set(ids)];
}

export default function stopLocalServers() {
    if (process.env.BASE_URL || process.platform !== "win32") {
        return;
    }

    for (const port of PORTS) {
        for (const id of listeningProcessIds(port)) {
            try {
                execSync(`taskkill /PID ${id} /T /F`, { stdio: "ignore" });
            } catch {
            }
        }
    }
}