import { z } from "zod";
import type { ConniHttpClient } from "../http-client.js";

type RegisterFn = (
  name: string,
  description: string,
  schema: z.ZodRawShape,
  handler: (args: Record<string, unknown>) => Promise<unknown>
) => void;

const jsonBody = z
  .record(z.unknown())
  .describe("Request body object matching backend DTO (see references/admin-operations.md)");

export function registerAdminTools(client: ConniHttpClient, register: RegisterFn) {
  register(
    "conni_admin_overview",
    "Admin: system overview statistics dashboard.",
    {},
    async () => client.get("/system/overview/get")
  );

  // --- Workflow ---
  register(
    "conni_admin_list_workflows",
    "Admin: paginated workflow list with category info.",
    {
      page: z.number().int().min(1).optional(),
      size: z.number().int().min(1).optional(),
      keyword: z.string().optional(),
      categoryId: z.number().int().optional(),
    },
    async (args) =>
      client.get("/system/workflow/page", {
        page: (args.page as number) ?? 1,
        size: (args.size as number) ?? 10,
        keyword: args.keyword as string | undefined,
        categoryId: args.categoryId as number | undefined,
      })
  );

  register(
    "conni_admin_get_workflow_detail",
    "Admin: full workflow config for editing (form nodes, output nodes, JSON).",
    { workflowId: z.number().int() },
    async (args) =>
      client.get("/system/workflow/detail", { workflowId: args.workflowId as number })
  );

  register(
    "conni_admin_parse_workflow_json",
    "Admin: parse a ComfyUI workflow JSON file before save. Returns parsed nodes for form/output config.",
    { filePath: z.string().describe("Local path to .json workflow file") },
    async (args) => client.uploadMultipart("/system/workflow/parsing", args.filePath as string)
  );

  register(
    "conni_admin_save_workflow",
    "Admin: create new workflow from SaveWorkflowConfigDto (after parsing JSON).",
    { config: jsonBody },
    async (args) => {
      await client.postVoid("/system/workflow/save", args.config);
      return { ok: true };
    }
  );

  register(
    "conni_admin_update_workflow_config",
    "Admin: full workflow config update (UpdateWorkflowConfigDto, includes workflowId).",
    { config: jsonBody },
    async (args) => {
      await client.postVoid("/system/workflow/update-config", args.config);
      return { ok: true };
    }
  );

  register(
    "conni_admin_update_workflow",
    "Admin: update workflow name, category, published flag only.",
    {
      workflowId: z.number().int(),
      name: z.string(),
      workflowCategoryId: z.number().int(),
      published: z.boolean().optional(),
    },
    async (args) => {
      await client.postVoid("/system/workflow/update", {
        workflowId: args.workflowId,
        name: args.name,
        workflowCategoryId: args.workflowCategoryId,
        published: args.published,
      });
      return { ok: true };
    }
  );

  register(
    "conni_admin_delete_workflow",
    "Admin: delete a workflow.",
    { workflowId: z.number().int() },
    async (args) => {
      await client.postVoid("/system/workflow/delete", { workflowId: args.workflowId });
      return { ok: true };
    }
  );

  register(
    "conni_admin_list_workflow_categories",
    "Admin: list all workflow categories.",
    {},
    async () => client.get("/system/workflow/category/list")
  );

  register(
    "conni_admin_create_workflow_category",
    "Admin: create workflow category.",
    { name: z.string() },
    async (args) => client.post<number>("/system/workflow/category/create", { name: args.name })
  );

  register(
    "conni_admin_update_workflow_category",
    "Admin: rename workflow category.",
    { categoryId: z.number().int(), name: z.string() },
    async (args) => {
      await client.postVoid("/system/workflow/category/update", {
        categoryId: args.categoryId,
        name: args.name,
      });
      return { ok: true };
    }
  );

  register(
    "conni_admin_delete_workflow_category",
    "Admin: delete workflow category.",
    { categoryId: z.number().int() },
    async (args) => {
      await client.postVoid("/system/workflow/category/delete", { categoryId: args.categoryId });
      return { ok: true };
    }
  );

  // --- Users ---
  register(
    "conni_admin_list_users",
    "Admin: paginated user list.",
    {
      page: z.number().int().min(1).optional(),
      size: z.number().int().min(1).optional(),
      keyword: z.string().optional(),
      role: z.string().optional().describe("USER / VIP / ADMIN"),
    },
    async (args) =>
      client.get("/system/user/page", {
        page: (args.page as number) ?? 1,
        size: (args.size as number) ?? 10,
        keyword: args.keyword as string | undefined,
        role: args.role as string | undefined,
      })
  );

  register(
    "conni_admin_create_user",
    "Admin: create user account.",
    {
      email: z.string().optional(),
      phone: z.string().optional(),
      password: z.string(),
      nickname: z.string().optional(),
      avatar: z.string().optional(),
      role: z.string().optional(),
    },
    async (args) => client.post<number>("/system/user/create", args)
  );

  register(
    "conni_admin_update_user",
    "Admin: update user profile/role.",
    {
      id: z.number().int(),
      email: z.string().optional(),
      phone: z.string().optional(),
      nickname: z.string().optional(),
      avatar: z.string().optional(),
      role: z.string().optional(),
    },
    async (args) => {
      await client.postVoid("/system/user/update", args);
      return { ok: true };
    }
  );

  register(
    "conni_admin_delete_user",
    "Admin: delete user.",
    { id: z.number().int() },
    async (args) => {
      await client.postVoid("/system/user/delete", { id: args.id });
      return { ok: true };
    }
  );

  // --- API Keys ---
  register(
    "conni_admin_list_api_keys",
    "Admin: paginated API key list.",
    {
      page: z.number().int().min(1).optional(),
      size: z.number().int().min(1).optional(),
      keyword: z.string().optional(),
      status: z.number().int().optional().describe("1=enabled 0=disabled"),
      userId: z.number().int().optional(),
    },
    async (args) =>
      client.get("/system/api-key/page", {
        page: (args.page as number) ?? 1,
        size: (args.size as number) ?? 10,
        keyword: args.keyword as string | undefined,
        status: args.status as number | undefined,
        userId: args.userId as number | undefined,
      })
  );

  register(
    "conni_admin_create_api_key",
    "Admin: create API key for a user. Returns plainKey once.",
    {
      userId: z.number().int(),
      name: z.string(),
      expiresAt: z.string().optional().describe("ISO datetime or null"),
    },
    async (args) =>
      client.post("/system/api-key/create", {
        userId: args.userId,
        name: args.name,
        expiresAt: args.expiresAt,
      })
  );

  register(
    "conni_admin_update_api_key",
    "Admin: update API key name/status/expiry.",
    {
      id: z.number().int(),
      name: z.string().optional(),
      status: z.number().int().optional(),
      expiresAt: z.string().optional(),
    },
    async (args) => {
      await client.postVoid("/system/api-key/update", args);
      return { ok: true };
    }
  );

  register(
    "conni_admin_delete_api_key",
    "Admin: delete API key.",
    { id: z.number().int() },
    async (args) => {
      await client.postVoid("/system/api-key/delete", { id: args.id });
      return { ok: true };
    }
  );

  register(
    "conni_admin_rotate_api_key",
    "Admin: rotate API key (new plainKey returned, old key invalidated).",
    { id: z.number().int() },
    async (args) => client.post("/system/api-key/rotate", { id: args.id })
  );

  // --- Redemption codes ---
  register(
    "conni_admin_list_redemption_codes",
    "Admin: paginated redemption code list.",
    {
      page: z.number().int().min(1).optional(),
      size: z.number().int().min(1).optional(),
      keyword: z.string().optional(),
      status: z.number().int().optional(),
    },
    async (args) =>
      client.get("/system/redemption-code/page", {
        page: (args.page as number) ?? 1,
        size: (args.size as number) ?? 10,
        keyword: args.keyword as string | undefined,
        status: args.status as number | undefined,
      })
  );

  register(
    "conni_admin_create_redemption_code",
    "Admin: create redemption code.",
    {
      creditsAmount: z.number().int().min(0),
      codeType: z.string().optional().describe("CREDITS / VIP / CREDITS_VIP"),
      prefix: z.string().optional(),
      length: z.number().int().optional(),
      expireTime: z.string().optional(),
      description: z.string().optional(),
    },
    async (args) => client.post<number>("/system/redemption-code/create", args)
  );

  register(
    "conni_admin_update_redemption_code",
    "Admin: update redemption code metadata.",
    { config: jsonBody },
    async (args) => {
      await client.postVoid("/system/redemption-code/update", args.config);
      return { ok: true };
    }
  );

  register(
    "conni_admin_update_redemption_code_credits",
    "Admin: update redemption code credit amount.",
    { id: z.number().int(), creditsAmount: z.number().int().min(0) },
    async (args) => {
      await client.postVoid("/system/redemption-code/update-credits", {
        id: args.id,
        creditsAmount: args.creditsAmount,
      });
      return { ok: true };
    }
  );

  register(
    "conni_admin_delete_redemption_code",
    "Admin: delete redemption code.",
    { id: z.number().int() },
    async (args) => {
      await client.postVoid("/system/redemption-code/delete", { id: args.id });
      return { ok: true };
    }
  );

  // --- Notice ---
  register(
    "conni_admin_set_notice",
    "Admin: set site-wide notice popup.",
    {
      title: z.string(),
      publisher: z.string(),
      content: z.string(),
      createTime: z.string().optional(),
    },
    async (args) => {
      await client.postVoid("/system/notice/set", args);
      return { ok: true };
    }
  );

  register(
    "conni_admin_clear_notice",
    "Admin: clear site-wide notice.",
    {},
    async () => {
      await client.postVoid("/system/notice/clear");
      return { ok: true };
    }
  );

  // --- Media admin ---
  register(
    "conni_admin_list_media_standardization_configs",
    "Admin: list media standardization workflow configs.",
    {},
    async () => client.get("/media/admin/standardization-configs")
  );

  register(
    "conni_admin_save_media_standardization_config",
    "Admin: create or update media standardization config.",
    {
      id: z.number().int().optional(),
      variantType: z.string(),
      workflowId: z.number().int(),
      displayName: z.string(),
      enabled: z.boolean().optional(),
    },
    async (args) => {
      await client.postVoid("/media/admin/standardization-config", args);
      return { ok: true };
    }
  );
}
