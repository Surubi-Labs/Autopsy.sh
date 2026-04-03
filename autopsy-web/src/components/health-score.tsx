"use client";

import { cn } from "@/lib/utils";

interface HealthScoreProps {
  score: number | null;
  size?: "sm" | "md" | "lg";
  className?: string;
}

const sizeMap = { sm: 48, md: 96, lg: 144 };
const strokeMap = { sm: 4, md: 6, lg: 8 };
const fontMap = { sm: "text-xs", md: "text-xl", lg: "text-3xl" };

function scoreColor(score: number | null): string {
  if (score === null) return "text-muted-foreground";
  if (score >= 70) return "text-green-500";
  if (score >= 40) return "text-yellow-500";
  return "text-red-500";
}

function strokeColor(score: number | null): string {
  if (score === null) return "stroke-muted";
  if (score >= 70) return "stroke-green-500";
  if (score >= 40) return "stroke-yellow-500";
  return "stroke-red-500";
}

export function HealthScore({ score, size = "md", className }: HealthScoreProps) {
  const dim = sizeMap[size];
  const stroke = strokeMap[size];
  const radius = (dim - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const progress = score !== null ? (score / 100) * circumference : 0;

  return (
    <div className={cn("relative inline-flex items-center justify-center", className)}>
      <svg width={dim} height={dim} className="-rotate-90">
        <circle
          cx={dim / 2}
          cy={dim / 2}
          r={radius}
          fill="none"
          strokeWidth={stroke}
          className="stroke-muted"
        />
        <circle
          cx={dim / 2}
          cy={dim / 2}
          r={radius}
          fill="none"
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={circumference - progress}
          className={cn("transition-all duration-700", strokeColor(score))}
        />
      </svg>
      <span
        className={cn(
          "absolute font-bold",
          fontMap[size],
          scoreColor(score),
        )}
      >
        {score !== null ? Math.round(score) : "—"}
      </span>
    </div>
  );
}
