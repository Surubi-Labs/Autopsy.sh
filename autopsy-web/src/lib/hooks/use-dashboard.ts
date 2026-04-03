import useSWR from "swr";
import { getDashboard, getRisks } from "../api-client";
import type { DashboardResponse, RiskItem } from "../types";

export function useDashboard() {
  return useSWR<DashboardResponse>("dashboard", getDashboard, {
    refreshInterval: 30000,
  });
}

export function useRisks() {
  return useSWR<RiskItem[]>("dashboard/risks", getRisks, {
    refreshInterval: 30000,
  });
}
