"use client";

import { cn } from "@/lib/utils";
import { CheckCircle2, Circle, Loader2, XCircle } from "lucide-react";
import type { RunResponse } from "@/lib/types";

const STAGES = ["clone", "extract", "metrics", "embed", "report", "deliver"];

interface RunStatusProps {
  run: RunResponse;
}

function stageStatus(
  stage: string,
  currentStage: string | null,
  runStatus: string
): "completed" | "active" | "pending" | "failed" {
  if (runStatus === "completed") return "completed";
  if (runStatus === "failed" && currentStage === stage) return "failed";

  const currentIdx = currentStage ? STAGES.indexOf(currentStage) : -1;
  const stageIdx = STAGES.indexOf(stage);

  if (stageIdx < currentIdx) return "completed";
  if (stageIdx === currentIdx) return "active";
  return "pending";
}

export function RunStatus({ run }: RunStatusProps) {
  return (
    <div className="flex items-center gap-1">
      {STAGES.map((stage) => {
        const status = stageStatus(stage, run.currentStage, run.status);
        return (
          <div key={stage} className="flex items-center gap-1">
            {status === "completed" && (
              <CheckCircle2 className="h-4 w-4 text-green-500" />
            )}
            {status === "active" && (
              <Loader2 className="h-4 w-4 text-blue-500 animate-spin" />
            )}
            {status === "pending" && (
              <Circle className="h-4 w-4 text-muted-foreground" />
            )}
            {status === "failed" && (
              <XCircle className="h-4 w-4 text-red-500" />
            )}
            <span
              className={cn(
                "text-xs capitalize",
                status === "completed" && "text-green-500",
                status === "active" && "text-blue-500",
                status === "pending" && "text-muted-foreground",
                status === "failed" && "text-red-500",
              )}
            >
              {stage}
            </span>
            {stage !== "deliver" && (
              <span className="text-muted-foreground mx-1">→</span>
            )}
          </div>
        );
      })}
    </div>
  );
}
