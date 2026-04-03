import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

interface RiskBadgeProps {
  severity: "critical" | "warning" | "healthy" | "info";
  className?: string;
}

const colors: Record<string, string> = {
  critical: "bg-red-500/20 text-red-400 border-red-500/30",
  warning: "bg-yellow-500/20 text-yellow-400 border-yellow-500/30",
  healthy: "bg-green-500/20 text-green-400 border-green-500/30",
  info: "bg-blue-500/20 text-blue-400 border-blue-500/30",
};

export function RiskBadge({ severity, className }: RiskBadgeProps) {
  return (
    <Badge variant="outline" className={cn(colors[severity], className)}>
      {severity.charAt(0).toUpperCase() + severity.slice(1)}
    </Badge>
  );
}
