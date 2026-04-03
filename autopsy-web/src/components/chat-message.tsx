"use client";

import { cn } from "@/lib/utils";
import { User, Bot } from "lucide-react";

interface ChatMessageProps {
  role: "user" | "assistant";
  content: string;
  sources?: string[];
}

export function ChatMessage({ role, content, sources }: ChatMessageProps) {
  return (
    <div
      className={cn(
        "flex gap-3 p-4 rounded-lg",
        role === "user" ? "bg-primary/10 ml-12" : "bg-muted mr-12",
      )}
    >
      <div className="shrink-0 mt-0.5">
        {role === "user" ? (
          <User className="h-5 w-5 text-primary" />
        ) : (
          <Bot className="h-5 w-5 text-muted-foreground" />
        )}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm whitespace-pre-wrap">{content}</p>
        {sources && sources.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-1">
            {sources.map((src, i) => (
              <span
                key={i}
                className="text-xs bg-muted-foreground/20 px-2 py-0.5 rounded font-mono"
              >
                {src}
              </span>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
