export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export const GITHUB_CLIENT_ID =
  process.env.NEXT_PUBLIC_GITHUB_CLIENT_ID ?? "";

export const GITHUB_OAUTH_URL = `https://github.com/login/oauth/authorize?client_id=${GITHUB_CLIENT_ID}&scope=read:org,repo`;

export const AUTH_TOKEN_KEY = "autopsy_token";
export const AUTH_ORG_KEY = "autopsy_org";
export const AUTH_ORG_NAME_KEY = "autopsy_org_name";
