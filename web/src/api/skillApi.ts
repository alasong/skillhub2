import { fetchJson } from './client'

// ---------- Types ----------

export interface SkillSummary {
  id: string
  namespace: string
  name: string
  displayName: string
  description: string
  latestVersion: string
  downloadCount: number
  evalScore: number | null
  updatedAt: string
  tags: string[]
}

export interface SkillDetail {
  namespace: string
  name: string
  displayName: string
  description: string
  inputsSchema: Record<string, unknown>
  outputsSchema: Record<string, unknown>
  latestVersion: string
  allVersions: SkillVersionRow[]
  downloadCount: number
  evalScore: number | null
  tags: string[]
  createdAt: string
  updatedAt: string
}

export interface SkillVersionRow {
  version: string
  publishedAt: string
  isDeprecated: boolean
  deprecationMessage?: string
  downloadCount: number
  semver: {
    major: number
    minor: number
    patch: number
    prerelease: string[]
  }
}

export interface EvalRunSummary {
  evalId: string
  skillVersion: string
  totalCases: number
  passed: number
  failed: number
  skipped: number
  overallScore: number
  ranAt: string
  cases: EvalCaseResult[]
}

export interface EvalCaseResult {
  caseName: string
  status: 'pass' | 'fail' | 'skip'
  score: number
  durationMs: number
  reason?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export interface SkillSearchFilters {
  query?: string
  namespace?: string
  tags?: string[]
  sort?: 'downloads' | 'updated' | 'name'
  page?: number
  size?: number
}

export interface VersionInfo {
  namespace: string
  name: string
  version: string
  constraint?: string
  publishedAt: string
  isDeprecated: boolean
  deprecationMessage?: string
}

// ---------- API Functions ----------

/**
 * Search for skills matching the given query and filters.
 */
export async function fetchSkills(
  query: string,
  filters: SkillSearchFilters,
  page: number,
): Promise<PageResponse<SkillSummary>> {
  const params = new URLSearchParams()
  if (query) params.set('q', query)
  if (filters.namespace) params.set('namespace', filters.namespace)
  if (filters.tags?.length) params.set('tags', filters.tags.join(','))
  if (filters.sort) params.set('sort', filters.sort)
  params.set('page', String(page))
  params.set('size', String(filters.size ?? 20))
  return fetchJson<PageResponse<SkillSummary>>(`/api/v2/skills?${params.toString()}`)
}

/**
 * Get full detail for a single skill including schemas and tags.
 */
export async function fetchSkillDetail(
  namespace: string,
  name: string,
): Promise<SkillDetail> {
  const cleanNamespace = namespace.startsWith('@') ? namespace.slice(1) : namespace
  return fetchJson<SkillDetail>(
    `/api/v2/skills/${encodeURIComponent(cleanNamespace)}/${encodeURIComponent(name)}`,
  )
}

/**
 * List all published versions for a skill.
 */
export async function fetchSkillVersions(
  namespace: string,
  name: string,
  page = 0,
  size = 50,
): Promise<SkillVersionRow[]> {
  const cleanNamespace = namespace.startsWith('@') ? namespace.slice(1) : namespace
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  const response = await fetchJson<PageResponse<SkillVersionRow>>(
    `/api/v2/skills/${encodeURIComponent(cleanNamespace)}/${encodeURIComponent(name)}/versions?${params.toString()}`,
  )
  return response.content
}

/**
 * Get the latest evaluation results for a skill version.
 * Returns null if no evaluation has been run.
 */
export async function fetchEvalResults(
  namespace: string,
  name: string,
  version?: string,
): Promise<EvalRunSummary | null> {
  const cleanNamespace = namespace.startsWith('@') ? namespace.slice(1) : namespace
  const params = version ? `?version=${encodeURIComponent(version)}` : ''
  return fetchJson<EvalRunSummary | null>(
    `/api/v2/skills/${encodeURIComponent(cleanNamespace)}/${encodeURIComponent(name)}/eval${params}`,
  )
}

/**
 * Resolve a semver constraint to the best matching version for a skill.
 */
export async function resolveVersion(
  namespace: string,
  name: string,
  constraint: string,
): Promise<VersionInfo> {
  const cleanNamespace = namespace.startsWith('@') ? namespace.slice(1) : namespace
  const params = new URLSearchParams({ constraint })
  return fetchJson<VersionInfo>(
    `/api/v2/skills/${encodeURIComponent(cleanNamespace)}/${encodeURIComponent(name)}/versions/resolve?${params.toString()}`,
  )
}

// ---------- Convenience formatters ----------

export function formatEvalScore(score: number | null): string {
  if (score === null) return 'N/A'
  return `${Math.round(score)}`
}

export function getEvalBadgeColor(score: number | null): string {
  if (score === null) return 'bg-gray-100 text-gray-400 dark:bg-gray-800 dark:text-gray-500'
  if (score >= 80) return 'bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-400'
  if (score >= 60) return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/40 dark:text-yellow-400'
  return 'bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-400'
}

export function formatDownloadCount(count: number): string {
  if (count >= 1_000_000) return `${(count / 1_000_000).toFixed(1)}M`
  if (count >= 1_000) return `${(count / 1_000).toFixed(1)}k`
  return String(count)
}
