function parseSseChunk(buffer) {
    const events = [];
    const parts = buffer.split("\n\n");
    const rest = parts.pop() ?? "";
    for (const block of parts) {
        if (!block.trim())
            continue;
        let event = "message";
        const dataLines = [];
        for (const line of block.split("\n")) {
            if (line.startsWith("event:")) {
                event = line.slice(6).trim();
            }
            else if (line.startsWith("data:")) {
                dataLines.push(line.slice(5).trim());
            }
        }
        if (dataLines.length) {
            events.push({ event, data: dataLines.join("\n") });
        }
    }
    return { events, rest };
}
function tryParseJson(data) {
    try {
        return JSON.parse(data);
    }
    catch {
        return data;
    }
}
/**
 * Consume generation agent SSE stream until `done` or `error`.
 */
export async function consumeGenerationStream(streamUrl, timeoutMs = 180_000) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
        const res = await fetch(streamUrl, {
            headers: { Accept: "text/event-stream" },
            signal: controller.signal,
        });
        if (!res.ok || !res.body) {
            throw new Error(`Generation stream failed: HTTP ${res.status}`);
        }
        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";
        let content = "";
        const drafts = [];
        let citations;
        while (true) {
            const { done, value } = await reader.read();
            if (done)
                break;
            buffer += decoder.decode(value, { stream: true });
            const parsed = parseSseChunk(buffer);
            buffer = parsed.rest;
            for (const ev of parsed.events) {
                const payload = tryParseJson(ev.data);
                if (ev.event === "task_draft" && payload && typeof payload === "object") {
                    drafts.push(payload);
                }
                else if (ev.event === "message" && payload && typeof payload === "object") {
                    const p = payload;
                    if (p.content)
                        content = p.content;
                    if (p.citations?.length)
                        citations = p.citations;
                }
                else if (ev.event === "done") {
                    if (payload && typeof payload === "object") {
                        const p = payload;
                        if (p.content)
                            content = p.content;
                        if (p.citations?.length)
                            citations = p.citations;
                    }
                    else if (typeof payload === "string" && payload && !content) {
                        content = payload;
                    }
                    return { content, drafts, citations };
                }
                else if (ev.event === "error") {
                    const msg = typeof payload === "string" ? payload : "Generation stream error";
                    throw new Error(msg);
                }
            }
        }
        return { content, drafts, citations };
    }
    finally {
        clearTimeout(timer);
    }
}
