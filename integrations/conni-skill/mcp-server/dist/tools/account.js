import { z } from "zod";
export function registerAccountTools(client, register) {
    register("conni_health", "Check Conni-X-Pro API connectivity and version.", {}, async () => {
        const health = await client.get("/client/health");
        const version = await client.get("/client/version");
        return { health, version };
    });
    register("conni_get_credits", "Get current user credit balance before submitting generation tasks.", {}, async () => client.get("/user/get/credits"));
    register("conni_get_user_info", "Get profile info for the API key bound user.", {}, async () => client.get("/user/get/user-info"));
    register("conni_get_credit_transactions", "Get paginated credit transaction history for current user.", {
        page: z.number().int().min(1).optional(),
        size: z.number().int().min(1).optional(),
        transactionType: z.string().optional(),
    }, async (args) => client.get("/user/get/credit-transactions", {
        page: args.page ?? 1,
        size: args.size ?? 10,
        transactionType: args.transactionType,
    }));
    register("conni_get_notice", "Get current site notice (public).", {}, async () => client.get("/notice/get"));
    register("conni_redeem_code", "Redeem a redemption code for credits/VIP.", { code: z.string() }, async (args) => {
        await client.postVoid("/redemption-code/redeem", { code: args.code });
        return { ok: true };
    });
}
