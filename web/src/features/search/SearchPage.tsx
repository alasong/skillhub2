import { useState, useEffect, useCallback } from 'react'
import { Search, SlidersHorizontal, X, RefreshCw } from 'lucide-react'
import { Input } from '@/shared/ui/input'
import { Button } from '@/shared/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/ui/select'
import { SkeletonList } from '@/shared/components/skeleton-loader'
import { EmptyState } from '@/shared/components/empty-state'
import { useDebounce } from '@/shared/hooks/use-debounce'
import { APP_SHELL_PAGE_CLASS_NAME } from '@/app/page-shell-style'
import { fetchSkills } from '@/api/skillApi'
import type { SkillSummary, SkillSearchFilters } from '@/api/skillApi'
import { SkillCard } from '@/features/search/SkillCard'

const PAGE_SIZE = 20
const DEBOUNCE_MS = 300

/**
 * Full skill browser search page.
 *
 * Provides a debounced search bar, filter sidebar (namespace, tags, sort), and a
 * responsive results grid with loading, empty, and error states.
 */
export function SearchPage() {
  // ---- Search state ----
  const [queryInput, setQueryInput] = useState('')
  const debouncedQuery = useDebounce(queryInput, DEBOUNCE_MS)

  // ---- Filter state ----
  const [namespaceFilter, setNamespaceFilter] = useState('')
  const [tagsInput, setTagsInput] = useState('')
  const [sort, setSort] = useState<'downloads' | 'updated' | 'name'>('downloads')
  const [page, setPage] = useState(0)

  // ---- Results state ----
  const [skills, setSkills] = useState<SkillSummary[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  // ---- Mobile filter toggle ----
  const [showFilters, setShowFilters] = useState(false)

  // ---- Derived filters for API call ----
  const activeTags = tagsInput
    .split(',')
    .map((t) => t.trim())
    .filter(Boolean)

  const fetchData = useCallback(async () => {
    setIsLoading(true)
    setError(null)

    try {
      const result = await fetchSkills(
        debouncedQuery,
        {
          namespace: namespaceFilter || undefined,
          tags: activeTags.length > 0 ? activeTags : undefined,
          sort,
          size: PAGE_SIZE,
        },
        page,
      )
      setSkills(result.content)
      setTotalElements(result.totalElements)
      setTotalPages(result.totalPages)
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to fetch skills'))
    } finally {
      setIsLoading(false)
    }
  }, [debouncedQuery, namespaceFilter, activeTags, sort, page])

  // Fetch when filters or page change
  useEffect(() => {
    fetchData()
  }, [fetchData])

  // Reset to first page when query or filters change
  useEffect(() => {
    setPage(0)
  }, [debouncedQuery, namespaceFilter, tagsInput, sort])

  const handleResetFilters = () => {
    setNamespaceFilter('')
    setTagsInput('')
    setSort('downloads')
    setQueryInput('')
    setPage(0)
  }

  const hasActiveFilters =
    !!namespaceFilter || activeTags.length > 0 || sort !== 'downloads'

  const hasResults = skills.length > 0 && !isLoading

  return (
    <div className={APP_SHELL_PAGE_CLASS_NAME}>
      {/* Search bar row */}
      <div className="max-w-3xl mx-auto w-full">
        <div className="relative">
          <Search className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground pointer-events-none" />
          <Input
            type="text"
            value={queryInput}
            onChange={(e) => setQueryInput(e.target.value)}
            placeholder="Search skills by name, description, or tag..."
            className="pl-12 pr-12 h-13 text-base rounded-xl border-border/70"
          />
          {queryInput ? (
            <button
              type="button"
              onClick={() => setQueryInput('')}
              className="absolute right-4 top-1/2 inline-flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-secondary/70 hover:text-foreground"
              aria-label="Clear search"
            >
              <X className="h-4 w-4" />
            </button>
          ) : null}
        </div>
      </div>

      {/* Mobile filter toggle */}
      <div className="lg:hidden flex items-center justify-between">
        <Button
          variant="outline"
          size="sm"
          onClick={() => setShowFilters((v) => !v)}
          className="gap-2"
        >
          <SlidersHorizontal className="h-4 w-4" />
          Filters
          {hasActiveFilters ? (
            <span className="ml-1 inline-flex h-5 w-5 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-primary-foreground">
              {(namespaceFilter ? 1 : 0) + activeTags.length + (sort !== 'downloads' ? 1 : 0)}
            </span>
          ) : null}
        </Button>

        {totalElements > 0 && (
          <span className="text-sm text-muted-foreground">
            {totalElements} skill{totalElements !== 1 ? 's' : ''}
          </span>
        )}
      </div>

      {/* Two-column layout: filters + results */}
      <div className="flex gap-6 lg:gap-8">
        {/* ===== Filter sidebar ===== */}
        <aside
          className={`
            shrink-0 w-full lg:w-64 space-y-5
            ${showFilters ? 'block' : 'hidden'} lg:block
          `}
        >
          <div className="rounded-xl border bg-card p-5 space-y-5 shadow-sm">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold text-foreground">Filters</h3>
              {hasActiveFilters && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleResetFilters}
                  className="h-auto px-2 py-1 text-xs text-muted-foreground"
                >
                  Reset
                </Button>
              )}
            </div>

            {/* Namespace filter */}
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Namespace
              </label>
              <Input
                type="text"
                value={namespaceFilter}
                onChange={(e) => setNamespaceFilter(e.target.value)}
                placeholder="Filter by namespace..."
              />
            </div>

            {/* Tags filter */}
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Tags
              </label>
              <Input
                type="text"
                value={tagsInput}
                onChange={(e) => setTagsInput(e.target.value)}
                placeholder="tag1, tag2, ..."
              />
              <p className="text-[11px] text-muted-foreground">
                Comma-separated tags (AND logic)
              </p>
            </div>

            {/* Sort filter */}
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Sort by
              </label>
              <Select
                value={sort}
                onValueChange={(value: 'downloads' | 'updated' | 'name') => setSort(value)}
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Sort by" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="downloads">Downloads</SelectItem>
                  <SelectItem value="updated">Last Updated</SelectItem>
                  <SelectItem value="name">Name</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </aside>

        {/* ===== Results area ===== */}
        <div className="flex-1 min-w-0 space-y-5">
          {/* Desktop result count */}
          {totalElements > 0 && (
            <div className="hidden lg:flex items-center justify-between">
              <span className="text-sm text-muted-foreground">
                {totalElements} skill{totalElements !== 1 ? 's' : ''} found
              </span>
              {isLoading && (
                <span className="text-sm text-muted-foreground animate-pulse">
                  Updating...
                </span>
              )}
            </div>
          )}

          {/* Loading skeleton */}
          {isLoading && !hasResults && (
            <SkeletonList count={6} />
          )}

          {/* Error state */}
          {error && !isLoading && (
            <div className="flex flex-col items-center justify-center py-16 text-center">
              <div className="w-16 h-16 rounded-2xl bg-red-100 dark:bg-red-900/20 flex items-center justify-center mb-5">
                <RefreshCw className="w-8 h-8 text-red-500" />
              </div>
              <h3 className="text-lg font-semibold text-foreground mb-2">
                Failed to load skills
              </h3>
              <p className="text-sm text-muted-foreground max-w-md mb-6">
                {error.message || 'Something went wrong. Please try again.'}
              </p>
              <Button variant="outline" onClick={fetchData} className="gap-2">
                <RefreshCw className="h-4 w-4" />
                Retry
              </Button>
            </div>
          )}

          {/* Results grid */}
          {hasResults && (
            <>
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                {skills.map((skill) => (
                  <div key={skill.id} className="h-full animate-fade-up">
                    <SkillCard skill={skill} />
                  </div>
                ))}
              </div>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 pt-4">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 0}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                  >
                    Previous
                  </Button>
                  <span className="text-sm text-muted-foreground px-3">
                    Page {page + 1} of {totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Next
                  </Button>
                </div>
              )}
            </>
          )}

          {/* Empty state */}
          {!isLoading && !error && !hasResults && (
            <EmptyState
              title="No skills found"
              description={
                debouncedQuery || hasActiveFilters
                  ? 'No skills match your search criteria. Try different keywords or clear your filters.'
                  : 'There are no skills available yet. Be the first to publish one!'
              }
              action={
                debouncedQuery || hasActiveFilters ? (
                  <Button variant="outline" onClick={handleResetFilters}>
                    Clear filters
                  </Button>
                ) : undefined
              }
            />
          )}
        </div>
      </div>
    </div>
  )
}
