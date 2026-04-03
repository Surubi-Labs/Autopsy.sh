import useSWR from "swr";
import { getRun, getRuns } from "../api-client";
import type { PageResponse, RunResponse } from "../types";

export function useRuns(repoId: string | undefined, limit = 20, offset = 0) {
  return useSWR<PageResponse<RunResponse>>(
    repoId ? `repos/${repoId}/runs?l=${limit}&o=${offset}` : null,
    () => getRuns(repoId!, limit, offset),
  );
}

export function useRun(runId: string | undefined, refreshInterval = 0) {
  return useSWR<RunResponse>(
    runId ? `runs/${runId}` : null,
    () => getRun(runId!),
    { refreshInterval },
  );
}
