import { z } from "zod";
import type { ConniHttpClient } from "../http-client.js";

export function registerAccountTools(
  client: ConniHttpClient,
  register: (name: string, description: string, schema: z.ZodRawShape, handler: (args: Record<string, unknown>) => Promise<unknown>) => void
) {
  register(
    "conni_health",
    "Check Conni-X-Pro API connectivity and version.",
    {},
    async () => {
      const health = await client.get<{ status: string }>("/client/health");
      const version = await client.get<Record<string, unknown>>("/client/version");
      return { health, version };
    }
  );

  register(
    "conni_get_credits",
    "Get current user credit balance before submitting generation tasks.",
    {},
    async () => client.get<number>("/user/get/credits")
  );

  register(
    "conni_get_user_info",
    "Get profile info for the API key bound user.",
    {},
    async () => client.get<Record<string, unknown>>("/user/get/user-info")
  );

  register(
    "conni_get_credit_transactions",
    "Get paginated credit transaction history for current user.",
    {
      page: z.number().int().min(1).optional(),
      size: z.number().int().min(1).optional(),
      transactionType: z.string().optional(),
    },
    async (args) =>
      client.get("/user/get/credit-transactions", {
        page: (args.page as number) ?? 1,
        size: (args.size as number) ?? 10,
        transactionType: args.transactionType as string | undefined,
      })
  );

  register(
    "conni_get_notice",
    "Get current site notice (public).",
    {},
    async () => client.get("/notice/get")
  );

  register(
    "conni_redeem_code",
    "Redeem a redemption code for credits/VIP.",
    { code: z.string() },
    async (args) => {
      await client.postVoid("/redemption-code/redeem", { code: args.code });
      return { ok: true };
    }
  );
}
