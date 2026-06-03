export type AnalysisStatus = 'NONE' | 'PENDING' | 'SUCCESS' | 'FAILED'

export interface AnalysisSource {
  analysisStatus?: string | null
  analysisError?: string | null
  interpretation?: string | null
}

const VALID_STATUSES = new Set<AnalysisStatus>(['NONE', 'PENDING', 'SUCCESS', 'FAILED'])

export function normalizeAnalysisStatus(status?: string | null): AnalysisStatus | undefined {
  const normalized = status?.trim().toUpperCase()
  if (!normalized || !VALID_STATUSES.has(normalized as AnalysisStatus)) return undefined
  return normalized as AnalysisStatus
}

export function getAnalysisFields(source: AnalysisSource) {
  const analysisStatus = normalizeAnalysisStatus(source.analysisStatus)
  const analysisError = source.analysisError?.trim()
  return {
    ...(analysisStatus ? { analysisStatus } : {}),
    ...(analysisError ? { analysisError } : {})
  }
}

export function isPendingAnalysis(dream?: AnalysisSource | null) {
  if (!dream) return false

  const status = normalizeAnalysisStatus(dream.analysisStatus)
  if (status) return status === 'PENDING'

  return isPendingInterpretation(dream.interpretation)
}

export function needsAnalysis(dream?: AnalysisSource | null) {
  if (!dream) return true

  const status = normalizeAnalysisStatus(dream.analysisStatus)
  if (status === 'PENDING' || status === 'SUCCESS') return false
  if (status === 'NONE' || status === 'FAILED') return true

  const text = dream.interpretation?.trim()
  if (!text) return true
  if (isPendingInterpretation(text)) return false
  return isPlaceholderInterpretation(text)
}

export function toSyncableInterpretation(interpretation?: string | null) {
  const text = interpretation?.trim()
  if (!text) return ''
  if (isPendingInterpretation(text) || isFailedInterpretation(text) || isPlaceholderInterpretation(text)) {
    return ''
  }
  return text
}

function isPendingInterpretation(interpretation?: string | null) {
  return Boolean(interpretation?.includes('解析中') || interpretation?.includes('后台解析'))
}

function isFailedInterpretation(interpretation: string) {
  return interpretation.includes('解析失败')
    || interpretation.includes('体验已用完')
    || interpretation.includes('请稍后重试')
}

function isPlaceholderInterpretation(interpretation: string) {
  return interpretation === '暂无解析' || interpretation === '暂无解析内容'
}
