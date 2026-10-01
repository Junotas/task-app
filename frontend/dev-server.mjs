import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, join, normalize, sep } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL(".", import.meta.url));
const port = Number(process.env.PORT || 5173);

const contentTypes = {
    ".html": "text/html; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".js": "text/javascript; charset=utf-8",
};

createServer(async (request, response) => {
    const path = new URL(request.url, "http://localhost").pathname;
    const file = normalize(join(root, path === "/" ? "index.html" : path));

    if (!file.startsWith(root.endsWith(sep) ? root : root + sep)) {
        response.writeHead(403).end();
        return;
    }

    try {
        const body = await readFile(file);
        response.writeHead(200, {
            "Content-Type": contentTypes[extname(file)] || "application/octet-stream",
            "Cache-Control": "no-store",
        });
        response.end(body);
    } catch {
        response.writeHead(404).end();
    }
}).listen(port, () => {
    console.log(`Frontend served on http://localhost:${port}`);
});