"use client";

import Link from "next/link";
import { Card, CardContent } from "@/components/ui/card";
import { HealthScore } from "./health-score";
import { RiskBadge } from "./risk-badge";
import type { RepoHealthSummary } from "@/lib/types";

function timeAgo(dateStr: string | null): string {
  if (!dateStr) return "Never";
  const diff = Date.now() - new Date(dateStr).getTime();
  const hours = Math.floor(diff / 3600000);
  if (hours < 1) return "Just now";
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

function scoreSeverity(score: number | null): "healthy" | "warning" | "critical" | undefined {
  if (score === null) return undefined;
  if (score >= 70) return "healthy";
  if (score >= 40) return "warning";
  return "critical";
}

interface RepoCardProps {
  repo: RepoHealthSummary;
}

export function RepoCard({ repo }: RepoCardProps) {
  const severity = scoreSeverity(repo.healthScore);

  return (
    <Link href={`/repos/${repo.repoId}`}>
      <Card className="hover:border-primary/50 transition-colors cursor-pointer">
        <CardContent className="flex items-center gap-4 p-4">
          <HealthScore score={repo.healthScore} size="sm" />
          <div className="flex-1 min-w-0">
            <p className="font-semibold truncate">{repo.name}</p>
            <p className="text-sm text-muted-foreground">
              {timeAgo(repo.lastAnalyzedAt)}
            </p>
          </div>
          {severity && <RiskBadge severity={severity} />}
        </CardContent>
      </Card>
    </Link>
  );
}
