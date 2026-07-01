import { z } from "zod";
import type { ConniHttpClient } from "../http-client.js";
import type { TaskNodeContainer } from "../types.js";

const nodeContainerSchema = z.object({
  nodeKey: z.string(),
  inputs: z.string().optional(),
  nodeValue: z.string().optional(),
  isUpload: z.boolean().optional(),
});

export function registerMediaTools(
  client: ConniHttpClient,
  register: (name: string, description: string, schema: z.ZodRawShape, handler: (args: Record<string, unknown>) => Promise<unknown>) => void
) {
  register(
    "conni_upload_file",
    "Upload a local file to OSS (for workflow form fields with isUpload=true). Returns public URL.",
    {
      filePath: z.string().describe("Absolute or relative path to local file"),
    },
    async (args) => {
      const url = await client.uploadMultipart<string>("/oss/upload/file", args.filePath as string);
      return { url };
    }
  );

  register(
    "conni_media_upload",
    "Upload a file to the user media library.",
    {
      filePath: z.string(),
      name: z.string().optional().describe("Display name for the media item"),
    },
    async (args) => {
      const extra = args.name ? { name: args.name as string } : undefined;
      return client.uploadMultipart<Record<string, unknown>>("/media/upload", args.filePath as string, "file", extra);
    }
  );

  register(
    "conni_list_media",
    "List media library items (paginated).",
    {
      page: z.number().int().min(1).optional(),
      mediaType: z.string().optional().describe("Filter: image, video, audio, etc."),
      keyword: z.string().optional(),
    },
    async (args) =>
      client.get("/media/page", {
        page: (args.page as number) ?? 1,
        mediaType: args.mediaType as string | undefined,
        keyword: args.keyword as string | undefined,
      })
  );

  register(
    "conni_create_media_task",
    "Create a ComfyUI task from a media library item.",
    {
      mediaId: z.number().int(),
      workflowId: z.number().int(),
      variantId: z.number().int().optional(),
      extraNodes: z.array(nodeContainerSchema).optional(),
    },
    async (args) => {
      const taskId = await client.post<string>("/media/create-task", {
        mediaId: args.mediaId,
        workflowId: args.workflowId,
        variantId: args.variantId,
        extraNodes: args.extraNodes as TaskNodeContainer[] | undefined,
      });
      return { taskId };
    }
  );

  register(
    "conni_media_detail",
    "Get media library item detail.",
    { mediaId: z.number().int() },
    async (args) => client.get("/media/detail", { mediaId: args.mediaId as number })
  );

  register(
    "conni_media_update",
    "Update media name or tags.",
    {
      mediaId: z.number().int(),
      name: z.string().optional(),
      tags: z.string().optional(),
    },
    async (args) => {
      await client.put("/media/update", args);
      return { ok: true };
    }
  );

  register(
    "conni_media_delete",
    "Delete a media library item.",
    { mediaId: z.number().int() },
    async (args) => {
      await client.postVoid("/media/delete", { mediaId: args.mediaId });
      return { ok: true };
    }
  );

  register(
    "conni_media_import_from_work",
    "Import a generated work into media library.",
    {
      workflowResultId: z.number().int(),
      name: z.string().optional(),
    },
    async (args) => client.post("/media/import-from-work", args)
  );
}
