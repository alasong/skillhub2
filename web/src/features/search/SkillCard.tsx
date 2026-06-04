import type { SkillSummary } from '@/api/skillApi'
import { getEvalBadgeColor, formatDownloadCount } from '@/api/skillApi'
import { Card } from '@/shared/ui/card'
import { NamespaceBadge } from '@/shared/components/namespace-badge'

interface SkillCardProps {
  skill: SkillSummary
}

export function SkillCard({ skill }: SkillCardProps) {
  return (
    <Card className="h-full p-5 flex flex-col bg-white border shadow-sm transition-shadow hover:shadow-md">
      {/* Header: name + namespace badge */}
      <div className="flex items-start justify-between mb-3 gap-2">
        <h3 className="font-semibold text-lg text-foreground line-clamp-1 min-w-0 break-words">
          {skill.displayName}
        </h3>
        <div className="flex-shrink-0">
          <NamespaceBadge type="GLOBAL" name={skill.namespace} />
        </div>
      </div>

      {/* Description: max 2 lines */}
      {skill.description && (
        <p className="text-sm text-muted-foreground mb-4 line-clamp-2 leading-relaxed">
          {skill.description}
        </p>
      )}

      {/* Footer: version, downloads, eval score */}
      <div className="mt-auto flex items-center gap-3 text-xs text-muted-foreground flex-wrap">
        {skill.latestVersion && (
          <span className="px-2.5 py-1 rounded-full bg-secondary/60 font-mono">
            v{skill.latestVersion}
          </span>
        )}

        <span className="flex items-center gap-1">
          <svg
            className="w-3.5 h-3.5"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10"
            />
          </svg>
          {formatDownloadCount(skill.downloadCount)}
        </span>

        <span
          className={`px-2 py-0.5 rounded-full text-xs font-medium ${getEvalBadgeColor(skill.evalScore)}`}
        >
          {skill.evalScore !== null ? `${Math.round(skill.evalScore)}` : 'N/A'}
        </span>
      </div>
    </Card>
  )
}
