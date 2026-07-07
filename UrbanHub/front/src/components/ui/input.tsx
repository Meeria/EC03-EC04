import * as React from "react"
import { cn } from "@/lib/utils"

export interface InputProps
  extends React.InputHTMLAttributes<HTMLInputElement> {
  error?: boolean
}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, error, type, ...props }, ref) => {
    return (
      <input
        type={type}
        className={cn(
          "flex h-11 w-full rounded-xl border bg-white px-4 py-2 text-sm text-[#1e293b] shadow-sm transition-colors",
          "focus:outline-none focus:ring-2 focus:ring-[#00e5a0] focus:border-transparent",
          "disabled:cursor-not-allowed disabled:opacity-50",
          "file:border-0 file:bg-transparent file:text-sm file:font-medium",
          error
            ? "border-red-500 focus:ring-red-500"
            : "border-[#e2e8f0] hover:border-[#00e5a0]/50",
          className
        )}
        ref={ref}
        {...props}
      />
    )
  }
)
Input.displayName = "Input"

export { Input }