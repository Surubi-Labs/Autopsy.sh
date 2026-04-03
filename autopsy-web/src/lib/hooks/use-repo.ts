import useSWR from "swr";
import { getRepos, getMetrics, getMetricsByType } from "../api-client";
import type { MetricResponse, MetricsSummaryResponse, RepoResponse } from "../types";

export function useRepos() {
  return useSWR<RepoResponse[]>("repos", getRepos);
}

export function useMetrics(repoId: string | undefined) {
  return useSWR<MetricsSummaryResponse>(
    repoId ? `repos/${repoId}/metrics` : null,
    () => getMetrics(repoId!),
  );
}

export function useMetricsByType(repoId: string | undefined, type: string) {
  return useSWR<MetricResponse[]>(
    repoId ? `repos/${repoId}/metrics/${type}` : null,
    () => getMetricsByType(repoId!, type),
  );
}
