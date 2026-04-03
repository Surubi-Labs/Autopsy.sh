"use client";

import { usePathname } from "next/navigation";
import { AuthProvider } from "@/lib/auth-provider";
import { Sidebar } from "@/components/sidebar";
import { Toaster } from "@/components/ui/sonner";

const PUBLIC_PATHS = ["/login", "/auth/callback"];

export function ClientLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const isPublic = PUBLIC_PATHS.some((p) => pathname.startsWith(p));

  return (
    <AuthProvider>
      {isPublic ? (
        <>{children}</>
      ) : (
        <div className="flex">
          <Sidebar />
          <main className="flex-1 ml-56 p-6 min-h-screen">{children}</main>
        </div>
      )}
      <Toaster />
    </AuthProvider>
  );
}
