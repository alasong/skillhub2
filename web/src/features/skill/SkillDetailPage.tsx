import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from '@tanstack/react-router'
import { ArrowLeft, ChevronRight, AlertCircle, RefreshCw, CheckCircle, XCircle, SkipForward } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { Card } from '@/shared/ui/card'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/shared/ui/tabs'
import { NamespaceBadge } from '@/shared/components/namespace-badge'
import { SkeletonCard } from '@/shared/components/skeleton-loader'
import { EmptyState } from '@/shared/components/empty-state'
import { InstallButton } from '@/features/skill/InstallButton'
import { fetchSkillDetail, fetchSkillVersions, fetchEvalResults } from '@/api/skillApi'
import { getEvalBadgeColor, formatDownloadCount } from '@/api/skillApi'
import type { SkillDetail, SkillVersionRow, EvalRunSummary, SkillDetail as SkillDetailType } from '@/api/skillApi'

/**
 * Skill detail page showing full information, version history, evaluation results,
 * and dependencies for a single skill.
 *
 * Route: /skills/:namespace/:name
 */
export function SkillDetailPage() {
  const navigate = useNavigate()
  const { namespace, name } = useParams({ from: '/skills/$namespace/$name' })

  const [skill, setSkill] = useState<SkillDetailType | null>(null)
  const [versions, setVersions] = useState<SkillVersionRow[]>([])
  const [evalResults, setEvalResults] = useState<EvalRunSummary | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)
  const [is404, setIs404] = useState(false)

  const fetchData = async () => {
    setIsLoading(true)
    setError(null)
    setIs404(false)

    try {
      const [skillData, versionsData, evalData] = await Promise.all([
        fetchSkillDetail(namespace, name),
        fetchSkillVersions(namespace, name),
        fetchEvalResults(namespace, name).catch(() => null),
      ])
      setSkill(skillData)
      setVersions(versionsData)
      setEvalResults(evalData)
    } catch (err) {
      const apiError = err as { status?: number; message?: string }
      if (apiError.status === 404 || apiError.status === 400) {
        setIs404(true)
      } else {
        setError(err instanceof Error ? err : new Error('Failed to load skill'))
      }
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    if (namespace && name) {
      fetchData()
    }
  }, [namespace, name])

  // ---- Loading state ----
  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto space-y-6 animate-fade-up py-8">
        <div className="h-5 w-48 animate-shimmer rounded-md" />
        <div className="h-10 w-64 animate-shimmer rounded-lg" />
        <div className="h-5 w-96 animate-shimmer rounded-md" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <SkeletonCard />
          <SkeletonCard />
        </div>
        <div className="h-48 animate-shimmer rounded-xl" />
      </div>
    )
  }

  // ---- 404 state ----
  if (is404) {
    return (
      <div className="max-w-4xl mx-auto py-20 animate-fade-up text-center">
        <AlertCircle className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
        <h2 className="text-2xl font-bold font-heading text-foreground mb-2">
          Skill not found
        </h2>
        <p className="text-muted-foreground mb-6">
          The skill <span className="font-mono">{namespace}/{name}</span> does not exist or has been removed.
        </p>
        <Button
          variant="outline"
          onClick={() => navigate({ to: '/' })}
        >
          Back to Home
        </Button>
      </div>
    )
  }

  // ---- Error state ----
  if (error && !skill) {
    return (
      <div className="max-w-4xl mx-auto py-20 animate-fade-up text-center">
        <div className="w-16 h-16 rounded-2xl bg-red-100 dark:bg-red-900/20 flex items-center justify-center mx-auto mb-5">
          <RefreshCw className="w-8 h-8 text-red-500" />
        </div>
        <h2 className="text-2xl font-bold font-heading text-foreground mb-2">
          Failed to load skill
        </h2>
        <p className="text-muted-foreground mb-6 max-w-md mx-auto">
          {error.message || 'Something went wrong. Please try again.'}
        </p>
        <Button variant="outline" onClick={fetchData} className="gap-2">
          <RefreshCw className="h-4 w-4" />
          Retry
        </Button>
      </div>
    )
  }

  if (!skill) return null

  // ---- Normal render ----
  const tabs = [
    { key: 'overview', label: 'Overview' },
    { key: 'versions', label: `Versions (${versions.length})` },
    { key: 'eval', label: 'Eval Results' },
    { key: 'deps', label: 'Dependencies' },
  ] as const

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-fade-up py-6">
      {/* ===== Breadcrumb ===== */}
      <nav className="flex items-center gap-2 text-sm text-muted-foreground">
        <Link to="/" className="hover:text-foreground transition-colors">
          Home
        </Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <span className="text-foreground font-medium">{skill.namespace}</span>
        <ChevronRight className="h-3.5 w-3.5" />
        <span className="text-foreground">{skill.displayName}</span>
      </nav>

      {/* ===== Header ===== */}
      <div className="space-y-4">
        <div className="flex flex-wrap items-center gap-3">
          <Button
            variant="ghost"
            size="sm"
            className="gap-1.5 px-2 text-muted-foreground hover:text-foreground"
            onClick={() => navigate({ to: '/' })}
          >
            <ArrowLeft className="h-4 w-4" />
            Back
          </Button>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
          <div className="space-y-3 min-w-0">
            <div className="flex items-center gap-3 flex-wrap">
              <h1 className="text-3xl sm:text-4xl font-bold font-heading text-foreground break-words">
                {skill.displayName}
              </h1>
              <NamespaceBadge type="GLOBAL" name={skill.namespace} />
            </div>

            <div className="flex items-center gap-3 text-sm text-muted-foreground flex-wrap">
              <span className="px-2.5 py-0.5 rounded-full bg-secondary/60 font-mono text-xs">
                v{skill.latestVersion}
              </span>

              <span className="flex items-center gap-1">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10" />
                </svg>
                {formatDownloadCount(skill.downloadCount)} downloads
              </span>

              {skill.evalScore !== null && (
                <span
                  className={`px-2.5 py-0.5 rounded-full text-xs font-medium ${getEvalBadgeColor(skill.evalScore)}`}
                >
                  Score: {Math.round(skill.evalScore)}
                </span>
              )}
            </div>
          </div>

          {/* Install button */}
          <div className="flex-shrink-0">
            <InstallButton namespace={skill.namespace} name={skill.name} />
          </div>
        </div>

        {/* Tags */}
        {skill.tags.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {skill.tags.map((tag) => (
              <span
                key={tag}
                className="inline-flex items-center rounded-full border border-border/60 bg-secondary/40 px-2.5 py-0.5 text-xs text-muted-foreground"
              >
                {tag}
              </span>
            ))}
          </div>
        )}
      </div>

      {/* ===== Tabs ===== */}
      <Tabs defaultValue="overview">
        <TabsList className="overflow-x-auto">
          {tabs.map((tab) => (
            <TabsTrigger key={tab.key} value={tab.key}>
              {tab.label}
            </TabsTrigger>
          ))}
        </TabsList>

        {/* ---- Overview tab ---- */}
        <TabsContent value="overview" className="mt-6 space-y-6">
          <Card className="p-6">
            <h3 className="text-base font-semibold text-foreground mb-3">Description</h3>
            <p className="text-sm text-muted-foreground leading-relaxed whitespace-pre-wrap">
              {skill.description || 'No description provided.'}
            </p>
          </Card>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Inputs Schema */}
            <Card className="p-6">
              <h3 className="text-base font-semibold text-foreground mb-3">Inputs</h3>
              {Object.keys(skill.inputsSchema).length > 0 ? (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border/60">
                      <th className="text-left py-2 pr-3 font-medium text-muted-foreground">Name</th>
                      <th className="text-left py-2 font-medium text-muted-foreground">Type</th>
                    </tr>
                  </thead>
                  <tbody>
                    {Object.entries(skill.inputsSchema).map(([key, value]) => (
                      <tr key={key} className="border-b border-border/30">
                        <td className="py-2 pr-3 font-mono text-foreground">{key}</td>
                        <td className="py-2 font-mono text-muted-foreground">
                          {typeof value === 'object' && value !== null
                            ? String((value as Record<string, unknown>).type ?? JSON.stringify(value))
                            : String(value)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <p className="text-sm text-muted-foreground">No inputs defined.</p>
              )}

              <details className="mt-3">
                <summary className="text-xs text-muted-foreground cursor-pointer hover:text-foreground">
                  Raw JSON Schema
                </summary>
                <pre className="mt-2 p-3 rounded-lg bg-muted/50 text-xs overflow-x-auto text-foreground">
                  {JSON.stringify(skill.inputsSchema, null, 2)}
                </pre>
              </details>
            </Card>

            {/* Outputs Schema */}
            <Card className="p-6">
              <h3 className="text-base font-semibold text-foreground mb-3">Outputs</h3>
              {Object.keys(skill.outputsSchema).length > 0 ? (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border/60">
                      <th className="text-left py-2 pr-3 font-medium text-muted-foreground">Name</th>
                      <th className="text-left py-2 font-medium text-muted-foreground">Type</th>
                    </tr>
                  </thead>
                  <tbody>
                    {Object.entries(skill.outputsSchema).map(([key, value]) => (
                      <tr key={key} className="border-b border-border/30">
                        <td className="py-2 pr-3 font-mono text-foreground">{key}</td>
                        <td className="py-2 font-mono text-muted-foreground">
                          {typeof value === 'object' && value !== null
                            ? String((value as Record<string, unknown>).type ?? JSON.stringify(value))
                            : String(value)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <p className="text-sm text-muted-foreground">No outputs defined.</p>
              )}

              <details className="mt-3">
                <summary className="text-xs text-muted-foreground cursor-pointer hover:text-foreground">
                  Raw JSON Schema
                </summary>
                <pre className="mt-2 p-3 rounded-lg bg-muted/50 text-xs overflow-x-auto text-foreground">
                  {JSON.stringify(skill.outputsSchema, null, 2)}
                </pre>
              </details>
            </Card>
          </div>

          <Card className="p-6">
            <h3 className="text-base font-semibold text-foreground mb-3">Install</h3>
            <div className="flex items-center gap-3">
              <InstallButton namespace={skill.namespace} name={skill.name} />
              <code className="text-sm text-muted-foreground font-mono">
                skillhub install {skill.namespace}/{skill.name}
              </code>
            </div>
          </Card>
        </TabsContent>

        {/* ---- Versions tab ---- */}
        <TabsContent value="versions" className="mt-6">
          {versions.length > 0 ? (
            <Card className="p-6">
              <div className="space-y-0 divide-y divide-border/40">
                {versions.map((v) => (
                  <div
                    key={v.version}
                    className="py-4 first:pt-0 last:pb-0 flex items-start justify-between gap-4"
                  >
                    <div className="space-y-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-mono font-semibold text-foreground">
                          v{v.version}
                        </span>
                        {v.isDeprecated && (
                          <span className="px-2 py-0.5 rounded-full text-[11px] font-medium bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400">
                            Deprecated
                          </span>
                        )}
                      </div>
                      {v.deprecationMessage && (
                        <p className="text-xs text-muted-foreground">
                          {v.deprecationMessage}
                        </p>
                      )}
                      <div className="flex items-center gap-3 text-xs text-muted-foreground">
                        <span>Published {formatDate(v.publishedAt)}</span>
                        <span className="w-1 h-1 rounded-full bg-muted-foreground/40" />
                        <span>{formatDownloadCount(v.downloadCount)} downloads</span>
                      </div>
                    </div>
                    <div className="flex-shrink-0 text-right text-xs text-muted-foreground">
                      <span className="font-mono">
                        v{v.semver.major}.{v.semver.minor}.{v.semver.patch}
                      </span>
                      {v.semver.prerelease.length > 0 && (
                        <span className="ml-1 text-amber-600">
                          -{v.semver.prerelease.join('.')}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          ) : (
            <Card className="p-8 text-center text-muted-foreground">
              No versions published yet.
            </Card>
          )}
        </TabsContent>

        {/* ---- Eval Results tab ---- */}
        <TabsContent value="eval" className="mt-6">
          {evalResults ? (
            <div className="space-y-5">
              {/* Summary bar */}
              <Card className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-base font-semibold text-foreground">
                    Latest Evaluation
                  </h3>
                  <span className="text-xs text-muted-foreground">
                    {formatDate(evalResults.ranAt)}
                  </span>
                </div>

                <div className="flex items-center gap-6 flex-wrap">
                  <div className="flex items-center gap-2">
                    <span className="text-2xl font-bold font-heading text-foreground">
                      {Math.round(evalResults.overallScore)}
                    </span>
                    <span className="text-sm text-muted-foreground">/ 100</span>
                  </div>

                  <div className="flex items-center gap-4 text-sm">
                    <span className="flex items-center gap-1.5 text-green-600 dark:text-green-400">
                      <CheckCircle className="h-4 w-4" />
                      {evalResults.passed} passed
                    </span>
                    <span className="flex items-center gap-1.5 text-red-600 dark:text-red-400">
                      <XCircle className="h-4 w-4" />
                      {evalResults.failed} failed
                    </span>
                    <span className="flex items-center gap-1.5 text-muted-foreground">
                      <SkipForward className="h-4 w-4" />
                      {evalResults.skipped} skipped
                    </span>
                  </div>

                  <span className="text-xs text-muted-foreground">
                    Skill version: v{evalResults.skillVersion}
                  </span>
                </div>
              </Card>

              {/* Per-case results */}
              <Card className="p-6">
                <h4 className="text-sm font-semibold text-foreground mb-4">
                  Test Cases ({evalResults.totalCases})
                </h4>
                <div className="space-y-2">
                  {evalResults.cases.map((c) => (
                    <div
                      key={c.caseName}
                      className="flex items-start gap-3 rounded-lg border border-border/60 p-3"
                    >
                      <span className="mt-0.5 flex-shrink-0">
                        {c.status === 'pass' && (
                          <CheckCircle className="h-4 w-4 text-green-500" />
                        )}
                        {c.status === 'fail' && (
                          <XCircle className="h-4 w-4 text-red-500" />
                        )}
                        {c.status === 'skip' && (
                          <SkipForward className="h-4 w-4 text-muted-foreground" />
                        )}
                      </span>
                      <div className="flex-1 min-w-0 space-y-1">
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-sm font-medium text-foreground truncate">
                            {c.caseName}
                          </span>
                          <div className="flex items-center gap-3 flex-shrink-0">
                            <span className="text-xs text-muted-foreground">
                              Score: {Math.round(c.score)}
                            </span>
                            <span className="text-xs text-muted-foreground font-mono">
                              {c.durationMs}ms
                            </span>
                          </div>
                        </div>
                        {c.reason && (
                          <p className="text-xs text-red-600 dark:text-red-400 whitespace-pre-wrap">
                            {c.reason}
                          </p>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </Card>
            </div>
          ) : (
            <Card className="p-8 text-center text-muted-foreground">
              No evaluation results available for this skill.
            </Card>
          )}
        </TabsContent>

        {/* ---- Dependencies tab ---- */}
        <TabsContent value="deps" className="mt-6">
          <Card className="p-6">
            <h3 className="text-base font-semibold text-foreground mb-3">Dependencies</h3>
            <p className="text-sm text-muted-foreground">
              Dependency information is not available for this version. Check the skill manifest
              for dependency details.
            </p>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}

// ---- Helpers ----

function formatDate(iso: string): string {
  try {
    const date = new Date(iso)
    return date.toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  } catch {
    return iso
  }
}
