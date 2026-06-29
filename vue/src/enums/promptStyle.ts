export enum PromptStyleEnum {
  NONE = 'NONE',
  SD_POSITIVE = 'SD_POSITIVE',
  SD_NEGATIVE = 'SD_NEGATIVE',
  WAN_VIDEO = 'WAN_VIDEO',
  GENERAL = 'GENERAL'
}

export const PROMPT_STYLE_OPTIONS = [
  PromptStyleEnum.NONE,
  PromptStyleEnum.SD_POSITIVE,
  PromptStyleEnum.SD_NEGATIVE,
  PromptStyleEnum.WAN_VIDEO,
  PromptStyleEnum.GENERAL
] as const

export function isPromptAssistEnabled(style?: string | null): boolean {
  return !!style && style !== PromptStyleEnum.NONE
}
