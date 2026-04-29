import * as React from "react";
import { cn } from "@/lib/utils";

interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  tone?: "neutral" | "positive" | "warning";
}

export function Badge({ className, tone = "neutral", ...props }: BadgeProps) {
  const toneClass =
    tone === "positive"
      ? "bg-emerald-500/15 text-emerald-300 border-emerald-500/30"
      : tone === "warning"
      ? "bg-amber-500/15 text-amber-200 border-amber-500/30"
      : "bg-muted text-muted-foreground border-border";

  return (
    <span
      className={cn("inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-medium", toneClass, className)}
      {...props}
    />
  );
}
