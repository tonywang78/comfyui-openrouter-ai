import { z } from "zod";
import type { ConniHttpClient } from "../http-client.js";
export declare function registerComfyuiTools(client: ConniHttpClient, register: (name: string, description: string, schema: z.ZodRawShape, handler: (args: Record<string, unknown>) => Promise<unknown>) => void): void;
