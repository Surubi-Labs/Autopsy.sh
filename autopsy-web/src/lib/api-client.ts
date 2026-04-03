import { API_BASE_URL, AUTH_TOKEN_KEY } from "./constants";
import type {
  AuthResponse,
  BillingResponse,
  CheckoutResponse,
  ConnectRepoRequest,
  DashboardResponse,
  DeliveryPreferencesRequest,
  DeliveryPreferencesResponse,
  MetricResponse,
  MetricsSummaryResponse,
  PageResponse,
  QueryResponse,
  RepoResponse,
  ReportDetailResponse,
  ReportSummaryResponse,
  RiskItem,
  RunResponse,
  TriggerAnalysisResponse,
} from "./types";

export class AuthError extends Error {
  constructor() {
    super("Unauthorized");
    this.name = "AuthError";
  }
}

function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(AUTH_TOKEN_KEY);
}

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...((options.headers as Record<string, string>) ?? {}),
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    throw new AuthError();
  }

  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new Error(`API error ${response.status}: ${text}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

// Auth
export const authGithub = (code: string) =>
  request<AuthResponse>("/auth/github", {
    method: "POST",
    body: JSON.stringify({ code }),
  });

// Dashboard
export const getDashboard = () => request<DashboardResponse>("/dashboard");
export const getRisks = () => request<RiskItem[]>("/dashboard/risks");

// Repos
export const getRepos = () => request<RepoResponse[]>("/repos");
export const connectRepo = (req: ConnectRepoRequest) =>
  request<RepoResponse>("/repos", {
    method: "POST",
    body: JSON.stringify(req),
  });
export const deleteRepo = (id: string) =>
  request<void>(`/repos/${id}`, { method: "DELETE" });

// Analysis
export const triggerAnalysis = (repoId: string) =>
  request<TriggerAnalysisResponse>(`/repos/${repoId}/analyze`, {
    method: "POST",
  });
export const getRuns = (repoId: string, limit = 20, offset = 0) =>
  request<PageResponse<RunResponse>>(
    `/repos/${repoId}/runs?limit=${limit}&offset=${offset}`
  );
export const getRun = (runId: string) =>
  request<RunResponse>(`/runs/${runId}`);

// Metrics
export const getMetrics = (repoId: string) =>
  request<MetricsSummaryResponse>(`/repos/${repoId}/metrics`);
export const getMetricsByType = (repoId: string, type: string) =>
  request<MetricResponse[]>(`/repos/${repoId}/metrics/${type}`);

// Reports
export const getReports = (repoId: string, limit = 20, offset = 0) =>
  request<PageResponse<ReportSummaryResponse>>(
    `/repos/${repoId}/reports?limit=${limit}&offset=${offset}`
  );
export const getReport = (reportId: string) =>
  request<ReportDetailResponse>(`/reports/${reportId}`);

// Query (RAG)
export const queryRepo = (repoId: string, question: string) =>
  request<QueryResponse>(`/repos/${repoId}/query`, {
    method: "POST",
    body: JSON.stringify({ question }),
  });

// Billing
export const getBilling = () => request<BillingResponse>("/billing");
export const createCheckout = (priceId: string) =>
  request<CheckoutResponse>("/billing/checkout", {
    method: "POST",
    body: JSON.stringify({ priceId }),
  });

// Settings
export const getDeliveryPrefs = () =>
  request<DeliveryPreferencesResponse>("/settings/delivery");
export const updateDeliveryPrefs = (req: DeliveryPreferencesRequest) =>
  request<DeliveryPreferencesResponse>("/settings/delivery", {
    method: "PUT",
    body: JSON.stringify(req),
  });

// SWR fetcher
export const fetcher = <T>(path: string) => request<T>(path);
