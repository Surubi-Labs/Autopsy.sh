"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "@/lib/auth-provider";
import { useBilling, useDeliveryPrefs } from "@/lib/hooks/use-billing";
import { useRepos } from "@/lib/hooks/use-repo";
import {
  connectRepo,
  createCheckout,
  deleteRepo,
  updateDeliveryPrefs,
} from "@/lib/api-client";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { toast } from "sonner";
import { Trash2, Plus } from "lucide-react";

function SettingsContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const { data: billing, mutate: mutateBilling } = useBilling();
  const { data: prefs, mutate: mutatePrefs } = useDeliveryPrefs();
  const { data: repos, mutate: mutateRepos } = useRepos();

  const [email, setEmail] = useState("");
  const [slackUrl, setSlackUrl] = useState("");
  const [newRepoName, setNewRepoName] = useState("");
  const [newRepoUrl, setNewRepoUrl] = useState("");
  const [showAddRepo, setShowAddRepo] = useState(false);

  useEffect(() => {
    if (!authLoading && !isAuthenticated) router.push("/login");
  }, [authLoading, isAuthenticated, router]);

  useEffect(() => {
    if (prefs) {
      setEmail(prefs.emailAddress ?? "");
      setSlackUrl(prefs.slackWebhookUrl ?? "");
    }
  }, [prefs]);

  useEffect(() => {
    const billingStatus = searchParams.get("billing");
    if (billingStatus === "success") toast.success("Subscription updated!");
    if (billingStatus === "canceled") toast.info("Checkout canceled");
  }, [searchParams]);

  if (authLoading || !isAuthenticated) return null;

  const handleSaveDelivery = async () => {
    try {
      await updateDeliveryPrefs({
        emailAddress: email || null,
        slackWebhookUrl: slackUrl || null,
      });
      mutatePrefs();
      toast.success("Delivery preferences saved");
    } catch {
      toast.error("Failed to save preferences");
    }
  };

  const handleUpgrade = async () => {
    try {
      const proPriceId = process.env.NEXT_PUBLIC_STRIPE_PRO_PRICE_ID ?? "";
      const res = await createCheckout(proPriceId);
      window.location.href = res.url;
    } catch {
      toast.error("Failed to start checkout");
    }
  };

  const handleConnectRepo = async () => {
    try {
      await connectRepo({ name: newRepoName, gitUrl: newRepoUrl });
      mutateRepos();
      mutateBilling();
      setNewRepoName("");
      setNewRepoUrl("");
      setShowAddRepo(false);
      toast.success("Repository connected");
    } catch {
      toast.error("Failed to connect repository");
    }
  };

  const handleDeleteRepo = async (repoId: string) => {
    if (!confirm("Disconnect this repository?")) return;
    try {
      await deleteRepo(repoId);
      mutateRepos();
      mutateBilling();
      toast.success("Repository disconnected");
    } catch {
      toast.error("Failed to disconnect repository");
    }
  };

  const atLimit = billing
    ? billing.repoCount >= billing.repoLimit
    : false;

  return (
    <div>
      <PageHeader title="Settings" />

      <div className="space-y-6 max-w-3xl">
        {/* Billing */}
        <Card>
          <CardHeader>
            <CardTitle>Plan & Billing</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center gap-3">
              <Badge variant="outline" className="text-sm capitalize">
                {billing?.plan ?? "free"}
              </Badge>
              <span className="text-sm text-muted-foreground">
                {billing?.repoCount ?? 0} / {billing?.repoLimit ?? 1} repos
              </span>
            </div>
            <div className="w-full bg-muted rounded-full h-2">
              <div
                className="bg-primary h-2 rounded-full transition-all"
                style={{
                  width: billing
                    ? `${Math.min((billing.repoCount / billing.repoLimit) * 100, 100)}%`
                    : "0%",
                }}
              />
            </div>
            {billing?.plan === "free" && (
              <Button onClick={handleUpgrade} size="sm">
                Upgrade to Pro
              </Button>
            )}
          </CardContent>
        </Card>

        {/* Delivery */}
        <Card>
          <CardHeader>
            <CardTitle>Delivery Preferences</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <Label htmlFor="email">Email Address</Label>
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="reports@company.com"
                className="mt-1"
              />
            </div>
            <div>
              <Label htmlFor="slack">Slack Webhook URL</Label>
              <Input
                id="slack"
                value={slackUrl}
                onChange={(e) => setSlackUrl(e.target.value)}
                placeholder="https://hooks.slack.com/services/..."
                className="mt-1"
              />
            </div>
            <Button size="sm" onClick={handleSaveDelivery}>
              Save Preferences
            </Button>
          </CardContent>
        </Card>

        {/* Repos */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>Connected Repositories</CardTitle>
            <Button
              size="sm"
              variant="outline"
              disabled={atLimit}
              onClick={() => setShowAddRepo(true)}
            >
              <Plus className="h-4 w-4 mr-1" /> Connect Repo
            </Button>
          </CardHeader>
          <CardContent>
            {atLimit && (
              <p className="text-xs text-yellow-400 mb-3">
                Repo limit reached. Upgrade your plan to add more.
              </p>
            )}

            {showAddRepo && (
              <div className="border border-border rounded-lg p-4 mb-4 space-y-3">
                <div>
                  <Label>Name</Label>
                  <Input
                    value={newRepoName}
                    onChange={(e) => setNewRepoName(e.target.value)}
                    placeholder="my-service"
                    className="mt-1"
                  />
                </div>
                <div>
                  <Label>Git URL</Label>
                  <Input
                    value={newRepoUrl}
                    onChange={(e) => setNewRepoUrl(e.target.value)}
                    placeholder="https://github.com/org/repo.git"
                    className="mt-1"
                  />
                </div>
                <div className="flex gap-2">
                  <Button size="sm" onClick={handleConnectRepo}>
                    Connect
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setShowAddRepo(false)}
                  >
                    Cancel
                  </Button>
                </div>
              </div>
            )}

            <div className="space-y-2">
              {repos?.map((repo) => (
                <div
                  key={repo.id}
                  className="flex items-center justify-between py-2"
                >
                  <div>
                    <p className="text-sm font-medium">{repo.name}</p>
                    <p className="text-xs text-muted-foreground font-mono">
                      {repo.gitUrl}
                    </p>
                  </div>
                  <Button
                    size="sm"
                    variant="ghost"
                    className="text-red-400 hover:text-red-300"
                    onClick={() => handleDeleteRepo(repo.id)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              ))}
              {(!repos || repos.length === 0) && (
                <p className="text-sm text-muted-foreground">
                  No repositories connected yet.
                </p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default function SettingsPage() {
  return (
    <Suspense>
      <SettingsContent />
    </Suspense>
  );
}
