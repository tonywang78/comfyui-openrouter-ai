import { z } from "zod";
import type { ConniHttpClient } from "../http-client.js";

export function registerWorksTools(
  client: ConniHttpClient,
  register: (name: string, description: string, schema: z.ZodRawShape, handler: (args: Record<string, unknown>) => Promise<unknown>) => void
) {
  register(
    "conni_list_works",
    "List user's generated works (paginated).",
    {
      page: z.number().int().min(1).optional(),
    },
    async (args) =>
      client.get("/comfyui/result/get/workflow-result/page", {
        page: (args.page as number) ?? 1,
      })
  );

  register(
    "conni_get_work_detail",
    "Get detail of a generated work by workflowResultId.",
    {
      workflowResultId: z.number().int(),
    },
    async (args) =>
      client.get("/comfyui/result/get/workflow-result/detail", {
        workflowResultId: args.workflowResultId as number,
      })
  );

  register(
    "conni_delete_work",
    "Delete a generated work.",
    {
      workflowResultId: z.number().int(),
    },
    async (args) => {
      await client.postVoid("/comfyui/result/delete/workflow-result", {
        workflowResultId: args.workflowResultId,
      });
      return { ok: true };
    }
  );

  register(
    "conni_batch_delete_works",
    "Batch delete generated works.",
    { workflowResultIds: z.array(z.number().int()).min(1) },
    async (args) => {
      await client.postVoid("/comfyui/result/batch-delete/workflow-result", {
        workflowResultIds: args.workflowResultIds,
      });
      return { ok: true };
    }
  );
}
