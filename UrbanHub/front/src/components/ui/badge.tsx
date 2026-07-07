import * as React from "react"
import { cn } from "@/lib/utils"

const Badge = React.forwardRef<
  HTMLSpanElement,
  React.HTMLAttributes<HTMLSpanElement>
>(({ className, ...props }, ref) => (
  <span
    ref={ref}
    className={cn(
      "inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs font-medium tracking-wider transition-colors",
      className
    )}
    {...props}
  />
))
Badge.displayName = "Badge"

export { Badge }