"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { AUTH_ORG_KEY, AUTH_ORG_NAME_KEY, AUTH_TOKEN_KEY } from "./constants";

interface AuthState {
  token: string | null;
  orgId: string | null;
  orgName: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (token: string, orgId: string, orgName: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [orgId, setOrgId] = useState<string | null>(null);
  const [orgName, setOrgName] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    setToken(localStorage.getItem(AUTH_TOKEN_KEY));
    setOrgId(localStorage.getItem(AUTH_ORG_KEY));
    setOrgName(localStorage.getItem(AUTH_ORG_NAME_KEY));
    setIsLoading(false);
  }, []);

  const login = useCallback((t: string, oid: string, oname: string) => {
    localStorage.setItem(AUTH_TOKEN_KEY, t);
    localStorage.setItem(AUTH_ORG_KEY, oid);
    localStorage.setItem(AUTH_ORG_NAME_KEY, oname);
    setToken(t);
    setOrgId(oid);
    setOrgName(oname);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(AUTH_ORG_KEY);
    localStorage.removeItem(AUTH_ORG_NAME_KEY);
    setToken(null);
    setOrgId(null);
    setOrgName(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        token,
        orgId,
        orgName,
        isAuthenticated: !!token,
        isLoading,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
