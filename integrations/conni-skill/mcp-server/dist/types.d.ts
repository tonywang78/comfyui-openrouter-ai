export interface ApiResult<T = unknown> {
    code: number;
    msg: string;
    data: T;
}
export interface TaskNodeContainer {
    nodeKey: string;
    inputs?: string;
    nodeValue?: string;
    isUpload?: boolean;
    tips?: string;
    type?: string;
    options?: string;
}
export interface TaskProgress {
    taskId: string;
    workflowName?: string;
    progress?: number;
    status: string;
    location?: number;
    createTime?: string;
    creditsDeducted?: number;
    workflowResultModel?: Record<string, unknown>;
}
export interface GenerationDraft {
    draftId: string;
    workflowId?: number;
    workflowName?: string;
    summary?: string;
    nodeContainer?: TaskNodeContainer[];
}
export interface GenerationChatResult {
    sessionId: string;
    content: string;
    drafts: GenerationDraft[];
    citations?: unknown[];
}
