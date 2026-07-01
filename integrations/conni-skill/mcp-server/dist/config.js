export function loadConfig() {
    const apiKey = process.env.CONNI_API_KEY?.trim();
    if (!apiKey) {
        throw new Error("CONNI_API_KEY is required (format: ak_...)");
    }
    if (!apiKey.startsWith("ak_")) {
        throw new Error("CONNI_API_KEY must start with ak_");
    }
    const baseUrl = (process.env.CONNI_API_BASE_URL ?? "http://localhost:9898/api").replace(/\/$/, "");
    return { baseUrl, apiKey };
}
