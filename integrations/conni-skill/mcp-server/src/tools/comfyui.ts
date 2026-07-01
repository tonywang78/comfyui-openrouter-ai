import { z } from "zod";
import type { ConniHttpClient } from "../http-client.js";
import type { TaskNodeContainer, TaskProgress } from "../types.js";

const TERMINAL_STATUSES = new Set(["SUCCEED", "FAILED", "CANCELED"]);

const nodeContainerSchema = z.object({
  nodeKey: z.string(),
  inputs: z.string().optional(),
  nodeValue: z.string().optional(),
  isUpload: z.boolean().optional(),
  tips: z.string().optional(),
  type: z.string().optional(),
  options: z.string().optional(),
});

async function getTaskDetail(client: ConniHttpClient, taskId: string): Promise<TaskProgress> {
  try {
    return await client.get<TaskProgress>("/comfyui/task/get/task/detail", { taskId });
  } catch {
    // Fallback when detail endpoint unavailable: scan first pages
    for (let page = 1; page <= 5; page++) {
      const result = await client.get<{ total: number; items: TaskProgress[] }>(
        "/comfyui/task/get/task/progress-page",
        { page }
      );
      const found = result.items?.find((t) => t.taskId === taskId);
      if (found) return found;
      if (!result.items?.length || result.items.length < 10) break;
    }
    throw new Error(`Task not found: ${taskId}`);
  }
}

export function registerComfyuiTools(
  client: ConniHttpClient,
  register: (name: string, description: string, schema: z.ZodRawShape, handler: (args: Record<string, unknown>) => Promise<unknown>) => void
) {
  register(
    "conni_list_workflow_categories",
    "List ComfyUI workflow categories for filtering.",
    {},
    async () => client.get<unknown[]>("/comfyui/task/get/workflow-category/list")
  );

  register(
    "conni_list_workflows",
    "List available ComfyUI workflows (paginated, searchable).",
    {
      page: z.number().int().min(1).optional().describe("Page number, default 1"),
      prompt: z.string().optional().describe("Search keyword"),
      categoryId: z.number().int().optional().describe("Filter by category ID"),
    },
    async (args) =>
      client.get("/comfyui/task/get/workflow/page", {
        page: (args.page as number) ?? 1,
        prompt: args.prompt as string | undefined,
        categoryId: args.categoryId as number | undefined,
      })
  );

  register(
    "conni_get_workflow_schema",
    "Get workflow form schema (formContainer) and credit cost before submit.",
    {
      workflowId: z.number().int().describe("Workflow ID"),
    },
    async (args) =>
      client.get("/comfyui/task/get/workflow/interface", {
        workflowId: args.workflowId as number,
      })
  );

  register(
    "conni_submit_task",
    "Submit a ComfyUI generation task with workflowId and nodeContainer.",
    {
      workflowId: z.number().int(),
      nodeContainer: z.array(nodeContainerSchema).min(1),
      mediaVariantId: z.number().int().optional(),
    },
    async (args) => {
      const taskId = await client.post<string>("/comfyui/task/submit/task", {
        workflowId: args.workflowId,
        nodeContainer: args.nodeContainer as TaskNodeContainer[],
        mediaVariantId: args.mediaVariantId,
      });
      return { taskId };
    }
  );

  register(
    "conni_cancel_task",
    "Cancel a waiting ComfyUI task.",
    {
      taskId: z.string(),
    },
    async (args) => {
      await client.postVoid("/comfyui/task/cancel/task", { taskId: args.taskId });
      return { ok: true };
    }
  );

  register(
    "conni_remake_task",
    "Remake a completed/failed/canceled task (re-queue with same taskId).",
    { taskId: z.string() },
    async (args) => {
      const taskId = await client.post<string>("/comfyui/task/remake/task", { taskId: args.taskId });
      return { taskId };
    }
  );

  register(
    "conni_get_task_progress",
    "Get paginated task progress list for current user.",
    {
      page: z.number().int().min(1).optional(),
      status: z.enum(["WAIT", "BUILD", "SUCCEED", "CANCELED", "FAILED"]).optional(),
    },
    async (args) =>
      client.get("/comfyui/task/get/task/progress-page", {
        page: (args.page as number) ?? 1,
        status: args.status as string | undefined,
      })
  );

  register(
    "conni_wait_for_task",
    "Poll task until SUCCEED, FAILED, or CANCELED (or timeout).",
    {
      taskId: z.string(),
      timeoutSeconds: z.number().int().min(10).max(3600).optional().describe("Max wait, default 600"),
      pollIntervalSeconds: z.number().int().min(2).max(60).optional().describe("Poll interval, default 5"),
    },
    async (args) => {
      const taskId = args.taskId as string;
      const timeoutMs = ((args.timeoutSeconds as number) ?? 600) * 1000;
      const intervalMs = ((args.pollIntervalSeconds as number) ?? 5) * 1000;
      const deadline = Date.now() + timeoutMs;

      while (Date.now() < deadline) {
        const task = await getTaskDetail(client, taskId);
        if (TERMINAL_STATUSES.has(task.status)) {
          return task;
        }
        await new Promise((r) => setTimeout(r, intervalMs));
      }
      throw new Error(`Task ${taskId} did not finish within timeout`);
    }
  );
}
