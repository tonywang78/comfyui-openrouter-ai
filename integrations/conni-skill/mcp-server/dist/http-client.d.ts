import type { ConniConfig } from "./config.js";
export declare class ConniHttpClient {
    private readonly config;
    constructor(config: ConniConfig);
    private headers;
    private parseResult;
    get<T>(path: string, params?: Record<string, string | number | undefined>): Promise<T>;
    post<T>(path: string, body?: unknown): Promise<T>;
    put<T>(path: string, body?: unknown): Promise<T>;
    postVoid(path: string, body?: unknown): Promise<void>;
    uploadMultipart<T>(path: string, filePath: string, fieldName?: string, extra?: Record<string, string>): Promise<T>;
    get apiKey(): string;
    get baseUrl(): string;
}
