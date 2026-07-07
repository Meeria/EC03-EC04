import * as React from "react"
import { Link } from "react-router-dom"
import { ChevronRight, Home } from "lucide-react"
import { cn } from "@/lib/utils"

export interface BreadcrumbItem {
  label: string
  to?: string
}

interface BreadcrumbProps extends React.HTMLAttributes<HTMLElement> {
  items: BreadcrumbItem[]
  separator?: React.ReactNode
}

const Breadcrumb = React.forwardRef<HTMLElement, BreadcrumbProps>(
  ({ className, items, ...props }, ref) => (
    <nav
      ref={ref}
      aria-label="breadcrumb"
      className={cn("flex items-center gap-1.5 text-sm", className)}
      {...props}
    >
      <ol className="flex items-center gap-1.5">
        {/* Home */}
        <li>
          <Link
            to="/"
            className="text-[#94a3b8] hover:text-[#00b07d] transition-colors duration-150"
            aria-label="Accueil"
          >
            <Home className="h-4 w-4" />
          </Link>
        </li>

        {items.map((item, i) => (
          <li key={i} className="flex items-center gap-1.5">
            <span className="text-[#cbd5e1]">
              <ChevronRight className="h-3.5 w-3.5" />
            </span>
            {item.to ? (
              <Link
                to={item.to}
                className="text-[#94a3b8] hover:text-[#00b07d] transition-colors duration-150 tracking-wide"
                style={{ fontFamily: "var(--font-mono)" }}
              >
                {item.label}
              </Link>
            ) : (
              <span
                className="text-[#334155] font-medium tracking-wide"
                style={{ fontFamily: "var(--font-mono)" }}
                aria-current="page"
              >
                {item.label}
              </span>
            )}
          </li>
        ))}
      </ol>
    </nav>
  )
)
Breadcrumb.displayName = "Breadcrumb"

export { Breadcrumb }