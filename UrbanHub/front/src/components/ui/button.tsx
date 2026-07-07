import * as React from "react"
import { Slot } from "@radix-ui/react-slot"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#00e5a0] focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0",
  {
    variants: {
      variant: {
        default:
          "bg-[#00e5a0] text-[#0d0f14] hover:bg-[#00e5a0]/90 shadow-sm",
        destructive:
          "bg-red-500 text-white hover:bg-red-500/90 shadow-sm",
        outline:
          "border border-[#e2e8f0] bg-white hover:bg-[#f8fafc] hover:border-[#00e5a0]/50 text-[#1e293b]",
        secondary:
          "bg-[#f1f5f9] text-[#64748b] hover:bg-[#e2e8f0] hover:text-[#1e293b]",
        ghost:
          "hover:bg-[#f1f5f9] hover:text-[#1e293b]",
        link:
          "text-[#00b07d] underline-offset-4 hover:underline",
      },
      size: {
        default: "h-11 px-5 py-2.5 has-[>svg]:px-3",
        sm: "h-9 rounded-lg px-3 has-[>svg]:px-2.5",
        lg: "h-12 rounded-xl px-8",
        icon: "h-11 w-11",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button"
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    )
  }
)
Button.displayName = "Button"

export { Button, buttonVariants }