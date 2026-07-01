import { z } from "zod";
import type { ConniHttpClient } from "../http-client.js";
type RegisterFn = (name: string, description: string, schema: z.ZodRawShape, handler: (args: Record<string, unknown>) => Promise<unknown>) => void;
export declare function registerAdminTools(client: ConniHttpClient, register: RegisterFn): void;
export {};
