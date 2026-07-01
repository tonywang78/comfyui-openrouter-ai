import type { GenerationChatResult } from "./types.js";
/**
 * Consume generation agent SSE stream until `done` or `error`.
 */
export declare function consumeGenerationStream(streamUrl: string, timeoutMs?: number): Promise<Omit<GenerationChatResult, "sessionId">>;
