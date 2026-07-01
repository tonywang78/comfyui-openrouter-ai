import { readFile } from "node:fs/promises";
import { basename } from "node:path";
import type { ConniConfig } from "./config.js";
import type { ApiResult } from "./types.js";

export class ConniHttpClient {
  constructor(private readonly config: ConniConfig) {}

  private headers(json = true): Record<string, string> {
    const h: Record<string, string> = {
      "X-Api-Key": this.config.apiKey,
    };
    if (json) {
      h["Content-Type"] = "application/json";
      h["Accept"] = "application/json";
    }
    return h;
  }

  private async parseResult<T>(res: Response): Promise<T> {
    const body = (await res.json()) as ApiResult<T>;
    if (!res.ok || body.code !== 200) {
      const msg = body.msg ?? res.statusText ?? "Request failed";
      throw new Error(msg);
    }
    return body.data;
  }

  async get<T>(path: string, params?: Record<string, string | number | undefined>): Promise<T> {
    const url = new URL(`${this.config.baseUrl}${path}`);
    if (params) {
      for (const [k, v] of Object.entries(params)) {
        if (v !== undefined && v !== null && v !== "") {
          url.searchParams.set(k, String(v));
        }
      }
    }
    const res = await fetch(url, { headers: this.headers() });
    return this.parseResult<T>(res);
  }

  async post<T>(path: string, body?: unknown): Promise<T> {
    const res = await fetch(`${this.config.baseUrl}${path}`, {
      method: "POST",
      headers: this.headers(),
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    return this.parseResult<T>(res);
  }

  async put<T>(path: string, body?: unknown): Promise<T> {
    const res = await fetch(`${this.config.baseUrl}${path}`, {
      method: "PUT",
      headers: this.headers(),
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    return this.parseResult<T>(res);
  }

  async postVoid(path: string, body?: unknown): Promise<void> {
    await this.post<null>(path, body);
  }

  async uploadMultipart<T>(path: string, filePath: string, fieldName = "file", extra?: Record<string, string>): Promise<T> {
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
    return this.parseResult<T>(res);
  }

  get apiKey(): string {
    return this.config.apiKey;
  }

  get baseUrl(): string {
    return this.config.baseUrl;
  }
}
