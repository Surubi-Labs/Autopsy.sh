// API response types matching Kotlin DTOs in autopsy-api

export interface AuthResponse {
  token: string;
  orgId: string;
  orgName: string;
}

export interface PageResponse<T> {
  data: T[];
  total: number;
  limit: number;
  offset: number;
}

export interface DashboardResponse {
  orgName: string;
  plan: string;
  repoCount: number;
  repos: RepoHealthSummary[];
}

export interface RepoHealthSummary {
  repoId: string;
  name: string;
  healthScore: number | null;
  lastAnalyzedAt: string | null;
}

export interface RiskItem {
  repoId: string;
  repoName: string;
  riskType: string;
  severity: "critical" | "warning" | "info";
  description: string;
}

export interface RepoResponse {
  id: string;
  name: string;
  gitUrl: string;
  defaultBranch: string;
  schedule: string;
  active: boolean;
  lastAnalyzedSha: string | null;
  lastAnalyzedAt: string | null;
  createdAt: string;
}

export interface RunResponse {
  id: string;
  repoId: string;
  status: string;
  currentStage: string | null;
  startedAt: string | null;
  completedAt: string | null;
  error: string | null;
  commitRange: string | null;
  createdAt: string;
}

export interface TriggerAnalysisResponse {
  runId: string;
  status: string;
}

export interface MetricsSummaryResponse {
  repoId: string;
  healthScore: number | null;
  breakdown: Record<string, number>;
  computedAt: string | null;
}

export interface MetricResponse {
  id: string;
  modulePath: string | null;
  metricType: string;
  value: number;
  details: string | null;
  computedAt: string;
}

export interface ReportSummaryResponse {
  id: string;
  runId: string;
  summary: string | null;
  riskScore: number | null;
  createdAt: string;
}

export interface ReportDetailResponse {
  id: string;
  runId: string;
  repoId: string;
  markdown: string;
  summary: string | null;
  riskScore: number | null;
  createdAt: string;
}

export interface BillingResponse {
  plan: string;
  repoLimit: number;
  repoCount: number;
}

export interface QueryRequest {
  question: string;
}

export interface QueryResponse {
  answer: string;
  sources: string[];
}

export interface ConnectRepoRequest {
  name: string;
  gitUrl: string;
  defaultBranch?: string;
}

export interface DeliveryPreferencesResponse {
  emailAddress: string | null;
  slackWebhookUrl: string | null;
}

export interface DeliveryPreferencesRequest {
  emailAddress: string | null;
  slackWebhookUrl: string | null;
}

export interface CheckoutRequest {
  priceId: string;
}

export interface CheckoutResponse {
  url: string;
}
