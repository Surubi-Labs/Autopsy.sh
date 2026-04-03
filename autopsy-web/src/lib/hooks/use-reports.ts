import useSWR from "swr";
import { getReport, getReports } from "../api-client";
import type { PageResponse, ReportDetailResponse, ReportSummaryResponse } from "../types";

export function useReports(repoId: string | undefined, limit = 20, offset = 0) {
  return useSWR<PageResponse<ReportSummaryResponse>>(
    repoId ? `repos/${repoId}/reports?l=${limit}&o=${offset}` : null,
    () => getReports(repoId!, limit, offset),
  );
}

export function useReport(reportId: string | undefined) {
  return useSWR<ReportDetailResponse>(
    reportId ? `reports/${reportId}` : null,
    () => getReport(reportId!),
  );
}
