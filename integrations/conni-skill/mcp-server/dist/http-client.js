import { readFile } from "node:fs/promises";
import { basename } from "node:path";
export class ConniHttpClient {
    config;
    constructor(config) {
        this.config = config;
    }
    headers(json = true) {
        const h = {
            "X-Api-Key": this.config.apiKey,
        };
        if (json) {
            h["Content-Type"] = "application/json";
            h["Accept"] = "application/json";
        }
        return h;
    }
    async parseResult(res) {
        const body = (await res.json());
        if (!res.ok || body.code !== 200) {
            const msg = body.msg ?? res.statusText ?? "Request failed";
            throw new Error(msg);
        }
        return body.data;
    }
    async get(path, params) {
        const url = new URL(`${this.config.baseUrl}${path}`);
        if (params) {
            for (const [k, v] of Object.entries(params)) {
                if (v !== undefined && v !== null && v !== "") {
                    url.searchParams.set(k, String(v));
                }
            }
        }
        const res = await fetch(url, { headers: this.headers() });
        return this.parseResult(res);
    }
    async post(path, body) {
        const res = await fetch(`${this.config.baseUrl}${path}`, {
            method: "POST",
            headers: this.headers(),
            body: body !== undefined ? JSON.stringify(body) : undefined,
        });
        return this.parseResult(res);
    }
    async put(path, body) {
        const res = await fetch(`${this.config.baseUrl}${path}`, {
            method: "PUT",
            headers: this.headers(),
            body: body !== undefined ? JSON.stringify(body) : undefined,
        });
        return this.parseResult(res);
    }
    async postVoid(path, body) {
        await this.post(path, body);
    }
    async uploadMultipart(path, filePath, fieldName = "file", extra) {
        const buffer = await readFile(filePath);
        const form = new FormData();
        const name = basename(filePath);
        form.append(fieldName, new Blob([buffer]), name);
        if (extra) {
            for (const [k, v] of Object.entries(extra)) {
                form.append(k, v);
            }
        }
        const res = await fetch(`${this.config.baseUrl}${path}`, {
            method: "POST",
            headers: { "X-Api-Key": this.config.apiKey },
            body: form,
        });
        return this.parseResult(res);
    }
    get apiKey() {
        return this.config.apiKey;
    }
    get baseUrl() {
        return this.config.baseUrl;
    }
}
