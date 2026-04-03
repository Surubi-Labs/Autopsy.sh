"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-provider";
import { GITHUB_OAUTH_URL } from "@/lib/constants";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Activity } from "lucide-react";

export default function LoginPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuth();

  useEffect(() => {
    if (!isLoading && isAuthenticated) router.push("/");
  }, [isLoading, isAuthenticated, router]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-background to-muted/30">
      <Card className="w-full max-w-sm">
        <CardContent className="pt-8 pb-8 text-center space-y-6">
          <div className="flex justify-center">
            <Activity className="h-12 w-12 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Autopsy</h1>
            <p className="text-sm text-muted-foreground mt-1">
              Codebase health monitoring for engineering teams
            </p>
          </div>
          <Button className="w-full" size="lg" onClick={() => window.location.href = GITHUB_OAUTH_URL}>
            Sign in with GitHub
          </Button>
          <p className="text-xs text-muted-foreground">
            Open source. Privacy first.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
