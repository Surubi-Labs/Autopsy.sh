"use client";

import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/lib/auth-provider";
import { useMetrics } from "@/lib/hooks/use-repo";
import { useReports, useReport } from "@/lib/hooks/use-reports";
import { useRuns } from "@/lib/hooks/use-runs";
import { triggerAnalysis } from "@/lib/api-client";
import { PageHeader } from "@/components/page-header";
import { HealthScore } from "@/components/health-score";
import { RunStatus } from "@/components/run-status";
import { ReportViewer } from "@/components/report-viewer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "sonner";
import { Play, MessageSquare } from "lucide-react";

export default function RepoDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const { data: metrics, isLoading: metricsLoading } = useMetrics(id);
  const { data: reports } = useReports(id);
  const { data: runs } = useRuns(id);
  const [selectedReport, setSelectedReport] = useState<string | null>(null);
  const { data: reportDetail } = useReport(selectedReport ?? undefined);

  useEffect(() => {
    if (!authLoading && !isAuthenticated) router.push("/login");
  }, [authLoading, isAuthenticated, router]);

  if (authLoading || !isAuthenticated) return null;

  const handleAnalyze = async () => {
    try {
      const res = await triggerAnalysis(id);
      toast.success(`Analysis queued (run ${res.runId.slice(0, 8)})`);
    } catch {
      toast.error("Failed to trigger analysis");
    }
  };

  const breakdownEntries = metrics?.breakdown
    ? Object.entries(metrics.breakdown).filter(([k]) => k !== "health_score")
    : [];

  return (
    <div>
      <PageHeader
        title={`Repository`}
        subtitle={id.slice(0, 8)}
        actions={
          <div className="flex gap-2">
            <Link href={`/repos/${id}/query`}>
              <Button variant="outline" size="sm">
                <MessageSquare className="h-4 w-4 mr-1" /> Ask AI
              </Button>
            </Link>
            <Button size="sm" onClick={handleAnalyze}>
              <Play className="h-4 w-4 mr-1" /> Analyze Now
            </Button>
          </div>
        }
      />

      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="reports">
            Reports {reports && `(${reports.total})`}
          </TabsTrigger>
          <TabsTrigger value="runs">
            Runs {runs && `(${runs.total})`}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="mt-4">
          {metricsLoading ? (
            <Skeleton className="h-48" />
          ) : (
            <div className="flex gap-6">
              <Card className="w-fit">
                <CardContent className="p-6 flex flex-col items-center">
                  <HealthScore score={metrics?.healthScore ?? null} size="lg" />
                  <p className="text-sm text-muted-foreground mt-2">
                    Health Score
                  </p>
                </CardContent>
              </Card>
              <div className="flex-1 grid grid-cols-2 gap-4">
                {breakdownEntries.map(([key, value]) => (
                  <Card key={key}>
                    <CardHeader className="pb-2">
                      <CardTitle className="text-xs uppercase text-muted-foreground">
                        {key.replace("_", " ")}
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <p className="text-2xl font-bold">
                        {typeof value === "number" ? value.toFixed(1) : value}
                      </p>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          )}
        </TabsContent>

        <TabsContent value="reports" className="mt-4 space-y-3">
          {selectedReport && reportDetail ? (
            <div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setSelectedReport(null)}
              >
                ← Back to list
              </Button>
              <ReportViewer markdown={reportDetail.markdown} />
            </div>
          ) : reports?.data.length === 0 ? (
            <p className="text-muted-foreground text-sm">
              No reports yet. Run an analysis to generate one.
            </p>
          ) : (
            reports?.data.map((r) => (
              <Card
                key={r.id}
                className="cursor-pointer hover:border-primary/50 transition-colors"
                onClick={() => setSelectedReport(r.id)}
              >
                <CardContent className="p-4 flex justify-between items-center">
                  <div>
                    <p className="text-sm font-medium">
                      {r.summary ?? "Health Report"}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {new Date(r.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                  {r.riskScore !== null && (
                    <span className="text-sm text-muted-foreground">
                      Risk: {(r.riskScore * 100).toFixed(0)}%
                    </span>
                  )}
                </CardContent>
              </Card>
            ))
          )}
        </TabsContent>

        <TabsContent value="runs" className="mt-4 space-y-3">
          {runs?.data.length === 0 ? (
            <p className="text-muted-foreground text-sm">No analysis runs yet.</p>
          ) : (
            runs?.data.map((run) => (
              <Card key={run.id}>
                <CardContent className="p-4">
                  <div className="flex justify-between items-center mb-2">
                    <span className="text-xs font-mono text-muted-foreground">
                      {run.id.slice(0, 8)}
                    </span>
                    <span className="text-xs text-muted-foreground">
                      {run.startedAt
                        ? new Date(run.startedAt).toLocaleString()
                        : "Queued"}
                    </span>
                  </div>
                  <RunStatus run={run} />
                  {run.error && (
                    <p className="text-xs text-red-400 mt-2">{run.error}</p>
                  )}
                </CardContent>
              </Card>
            ))
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
