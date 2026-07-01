#!/usr/bin/env node
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { loadConfig } from "./config.js";
import { ConniHttpClient } from "./http-client.js";
import { registerAdminTools } from "./tools/admin.js";
import { registerAccountTools } from "./tools/account.js";
import { registerComfyuiTools } from "./tools/comfyui.js";
import { registerGenerationTools } from "./tools/generation.js";
import { registerMediaTools } from "./tools/media.js";
import { registerWorksTools } from "./tools/works.js";
async function main() {
    const config = loadConfig();
    const client = new ConniHttpClient(config);
    const server = new McpServer({
        name: "conni-x-pro",
        version: "1.0.0",
    });
    const register = (name, description, schema, handler) => {
        server.tool(name, description, schema, async (args) => {
            try {
                const data = await handler(args);
                return {
                    content: [{ type: "text", text: JSON.stringify(data, null, 2) }],
                };
            }
            catch (err) {
                const message = err instanceof Error ? err.message : String(err);
                return {
                    content: [{ type: "text", text: JSON.stringify({ error: message }) }],
                    isError: true,
                };
            }
        });
    };
    registerAccountTools(client, register);
    registerComfyuiTools(client, register);
    registerWorksTools(client, register);
    registerGenerationTools(client, register);
    registerMediaTools(client, register);
    registerAdminTools(client, register);
    const transport = new StdioServerTransport();
    await server.connect(transport);
}
main().catch((err) => {
    console.error(err);
    process.exit(1);
});
