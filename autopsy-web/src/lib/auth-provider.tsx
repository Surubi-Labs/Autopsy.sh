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

interface AuthData {
  token: string | null;
  orgId: string | null;
  orgName: string | null;
  isLoading: boolean;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [authData, setAuthData] = useState<AuthData>({
    token: null,
    orgId: null,
    orgName: null,
    isLoading: true,
  });

  useEffect(() => {
    setAuthData({
      token: localStorage.getItem(AUTH_TOKEN_KEY),
      orgId: localStorage.getItem(AUTH_ORG_KEY),
      orgName: localStorage.getItem(AUTH_ORG_NAME_KEY),
      isLoading: false,
    });
  }, []);

  const login = useCallback((t: string, oid: string, oname: string) => {
    localStorage.setItem(AUTH_TOKEN_KEY, t);
    localStorage.setItem(AUTH_ORG_KEY, oid);
    localStorage.setItem(AUTH_ORG_NAME_KEY, oname);
    setAuthData({ token: t, orgId: oid, orgName: oname, isLoading: false });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(AUTH_ORG_KEY);
    localStorage.removeItem(AUTH_ORG_NAME_KEY);
    setAuthData({ token: null, orgId: null, orgName: null, isLoading: false });
  }, []);

  return (
    <AuthContext.Provider
      value={{
        token: authData.token,
        orgId: authData.orgId,
        orgName: authData.orgName,
        isAuthenticated: !!authData.token,
        isLoading: authData.isLoading,
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
