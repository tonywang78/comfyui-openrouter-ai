import { randomUUID } from "node:crypto";
import { z } from "zod";
import { consumeGenerationStream } from "../sse-consumer.js";
export function registerGenerationTools(client, register) {
    register("conni_generation_chat", "Send a message to the generation assistant agent. Returns assistant reply and any task drafts to confirm.", {
        sessionId: z.string().optional().describe("Reuse session for multi-turn; auto-generated if omitted"),
        text: z.string().optional(),
        imageUrls: z.array(z.string()).optional(),
        pinnedWorkflowId: z.number().int().optional(),
        pinnedWorkflowIds: z.array(z.number().int()).optional(),
        enableWebSearch: z.boolean().optional(),
        timeoutSeconds: z.number().int().min(30).max(600).optional(),
    }, async (args) => {
        const sessionId = args.sessionId || randomUUID();
        await client.postVoid("/llm/generation/submit", {
            sessionId,
            text: args.text,
            imageUrls: args.imageUrls,
            pinnedWorkflowId: args.pinnedWorkflowId,
            pinnedWorkflowIds: args.pinnedWorkflowIds,
        });
        const params = new URLSearchParams({
            sessionId,
            token: client.apiKey,
        });
        if (args.enableWebSearch) {
            params.set("enableWebSearch", "true");
        }
        if (args.pinnedWorkflowIds && args.pinnedWorkflowIds.length) {
            params.set("pinnedWorkflowIds", args.pinnedWorkflowIds.join(","));
        }
        else if (args.pinnedWorkflowId) {
            params.set("pinnedWorkflowId", String(args.pinnedWorkflowId));
        }
        const streamUrl = `${client.baseUrl}/llm/generation/stream?${params}`;
        const timeoutMs = (args.timeoutSeconds ?? 180) * 1000;
        const streamResult = await consumeGenerationStream(streamUrl, timeoutMs);
        const result = {
            sessionId,
            ...streamResult,
        };
        return result;
    });
    register("conni_generation_confirm", "Confirm a generation assistant draft and submit ComfyUI task.", {
        sessionId: z.string(),
        draftId: z.string(),
    }, async (args) => client.post("/llm/generation/confirm", {
        sessionId: args.sessionId,
        draftId: args.draftId,
    }));
    register("conni_generation_list_workflows", "List workflows available to the generation assistant.", {
        keyword: z.string().optional(),
        categoryId: z.number().int().optional(),
        limit: z.number().int().min(1).max(50).optional(),
    }, async (args) => client.get("/llm/generation/workflows", {
        keyword: args.keyword,
        categoryId: args.categoryId,
        limit: args.limit ?? 20,
    }));
    register("conni_generation_delete_session", "Delete a generation assistant session.", {
        sessionId: z.string(),
    }, async (args) => {
        await client.postVoid("/llm/generation/session/delete", {
            sessionId: args.sessionId,
        });
        return { ok: true };
    });
}
