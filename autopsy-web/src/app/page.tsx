"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-provider";
import { useDashboard, useRisks } from "@/lib/hooks/use-dashboard";
import { PageHeader } from "@/components/page-header";
import { RepoCard } from "@/components/repo-card";
import { RiskBadge } from "@/components/risk-badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

export default function DashboardPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const { data: dashboard, isLoading } = useDashboard();
  const { data: risks } = useRisks();

  useEffect(() => {
    if (!authLoading && !isAuthenticated) router.push("/login");
  }, [authLoading, isAuthenticated, router]);

  if (authLoading || !isAuthenticated) return null;

  return (
    <div>
      <PageHeader
        title="Dashboard"
        subtitle={dashboard?.orgName ?? "Loading..."}
      />
      <div className="flex gap-6">
        <div className="flex-1">
          {isLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
              {Array.from({ length: 6 }).map((_, i) => (
                <Skeleton key={i} className="h-24 rounded-lg" />
              ))}
            </div>
          ) : dashboard?.repos.length === 0 ? (
            <Card>
              <CardContent className="p-8 text-center text-muted-foreground">
                No repositories connected. Go to Settings to add one.
              </CardContent>
            </Card>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
              {dashboard?.repos.map((repo) => (
                <RepoCard key={repo.repoId} repo={repo} />
              ))}
            </div>
          )}
        </div>
        <div className="w-80 shrink-0 hidden lg:block">
          <Card>
            <CardHeader>
              <CardTitle className="text-sm">Top Risks</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {!risks || risks.length === 0 ? (
                <p className="text-sm text-muted-foreground">No risks detected</p>
              ) : (
                risks.map((risk, i) => (
                  <div key={i} className="flex items-start gap-2">
                    <RiskBadge severity={risk.severity as "critical" | "warning" | "info"} />
                    <div className="min-w-0">
                      <p className="text-xs font-medium truncate">{risk.repoName}</p>
                      <p className="text-xs text-muted-foreground">{risk.description}</p>
                    </div>
                  </div>
                ))
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
