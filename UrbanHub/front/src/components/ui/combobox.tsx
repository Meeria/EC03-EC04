import * as React from "react"
import { ChevronDown, Check, X } from "lucide-react"
import { cn } from "@/lib/utils"
import { Badge } from "./badge"

interface ComboboxOption {
  value: string
  label: string
}

interface ComboboxProps {
  options: ComboboxOption[]
  value: string
  onChange: (value: string) => void
  placeholder?: string
  emptyMessage?: string
  disabled?: boolean
  className?: string
}

const Combobox = React.forwardRef<HTMLDivElement, ComboboxProps>(
  ({ options, value, onChange, placeholder, emptyMessage, disabled, className }, _ref) => {
    const [open, setOpen] = React.useState(false)
    const [search, setSearch] = React.useState("")
    const containerRef = React.useRef<HTMLDivElement>(null)

    const filteredOptions = options.filter((o) =>
      o.label.toLowerCase().includes(search.toLowerCase())
    )

    const selectedOption = options.find((o) => o.value === value)

    React.useEffect(() => {
      const handleClickOutside = (e: MouseEvent) => {
        if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
          setOpen(false)
          setSearch("")
        }
      }
      document.addEventListener("mousedown", handleClickOutside)
      return () => document.removeEventListener("mousedown", handleClickOutside)
    }, [])

    return (
      <div ref={containerRef} className={cn("relative", className)}>
        <button
          type="button"
          disabled={disabled}
          onClick={() => setOpen(!open)}
          className={cn(
            "flex h-11 w-full items-center justify-between rounded-xl border bg-white px-4 py-2 text-sm shadow-sm transition-colors",
            "focus:outline-none focus:ring-2 focus:ring-[#00e5a0] focus:border-transparent",
            "disabled:cursor-not-allowed disabled:opacity-50",
            disabled
              ? "border-[#e2e8f0] opacity-50"
              : "border-[#e2e8f0] hover:border-[#00e5a0]/50"
          )}
        >
          <span className={cn(!selectedOption && "text-[#94a3b8]")}>
            {selectedOption?.label || placeholder || "Sélectionner..."}
          </span>
          <ChevronDown className="h-4 w-4 text-[#94a3b8]" />
        </button>

        {open && (
          <div className="absolute z-50 mt-1 w-full rounded-xl border border-[#e2e8f0] bg-white shadow-lg">
            <div className="p-2">
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Rechercher..."
                className="w-full rounded-lg border border-[#e2e8f0] px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#00e5a0]"
                autoFocus
              />
            </div>
            <div className="max-h-60 overflow-y-auto p-1">
              {filteredOptions.length === 0 ? (
                <p className="px-3 py-2 text-sm text-[#94a3b8]">{emptyMessage || "Aucun résultat"}</p>
              ) : (
                filteredOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => {
                      onChange(option.value)
                      setOpen(false)
                      setSearch("")
                    }}
                    className={cn(
                      "flex w-full items-center justify-between rounded-lg px-3 py-2 text-sm text-left transition-colors",
                      option.value === value
                        ? "bg-[#00e5a0]/10 text-[#00b07d]"
                        : "hover:bg-[#f8fafc] text-[#1e293b]"
                    )}
                  >
                    {option.label}
                    {option.value === value && <Check className="h-4 w-4" />}
                  </button>
                ))
              )}
            </div>
          </div>
        )}
      </div>
    )
  }
)
Combobox.displayName = "Combobox"

interface MultiComboboxProps {
  options: ComboboxOption[]
  value: string[]
  onChange: (value: string[]) => void
  placeholder?: string
  emptyMessage?: string
  maxItems?: number
  disabled?: boolean
  className?: string
}

const MultiCombobox = React.forwardRef<HTMLDivElement, MultiComboboxProps>(
  ({ options, value, onChange, placeholder, emptyMessage, maxItems = 4, disabled, className }, _ref) => {
    const [open, setOpen] = React.useState(false)
    const [search, setSearch] = React.useState("")
    const containerRef = React.useRef<HTMLDivElement>(null)

    const filteredOptions = options.filter((o) =>
      o.label.toLowerCase().includes(search.toLowerCase()) && !value.includes(o.value)
    )

    const selectedOptions = options.filter((o) => value.includes(o.value))

    React.useEffect(() => {
      const handleClickOutside = (e: MouseEvent) => {
        if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
          setOpen(false)
          setSearch("")
        }
      }
      document.addEventListener("mousedown", handleClickOutside)
      return () => document.removeEventListener("mousedown", handleClickOutside)
    }, [])

    const removeItem = (val: string) => {
      onChange(value.filter((v) => v !== val))
    }

    return (
      <div ref={containerRef} className={cn("relative", className)}>
        <div
          className={cn(
            "min-h-11 w-full rounded-xl border border-[#e2e8f0] bg-white px-3 py-2 shadow-sm transition-colors",
            "focus-within:ring-2 focus-within:ring-[#00e5a0] focus-within:border-transparent",
            disabled && "opacity-50 cursor-not-allowed"
          )}
        >
          <div className="flex flex-wrap gap-1.5">
            {selectedOptions.map((option) => (
              <Badge key={option.value} className="bg-[#00e5a0]/10 text-[#00b07d] border border-[#00e5a0]/30">
                {option.label}
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation()
                    removeItem(option.value)
                  }}
                  className="ml-0.5 rounded-full hover:bg-[#00e5a0]/20"
                >
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            ))}
            {value.length < maxItems && (
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                onClick={() => !disabled && setOpen(true)}
                placeholder={value.length === 0 ? (placeholder || "Sélectionner...") : ""}
                disabled={disabled}
                className="flex-1 min-w-[100px] bg-transparent text-sm focus:outline-none"
              />
            )}
          </div>
        </div>

        <button
          type="button"
          disabled={disabled}
          onClick={() => setOpen(!open)}
          className="absolute right-3 top-1/2 -translate-y-1/2"
        >
          <ChevronDown className="h-4 w-4 text-[#94a3b8]" />
        </button>

        {open && !disabled && (
          <div className="absolute z-50 mt-1 w-full rounded-xl border border-[#e2e8f0] bg-white shadow-lg">
            <div className="max-h-60 overflow-y-auto p-1">
              {filteredOptions.length === 0 ? (
                <p className="px-3 py-2 text-sm text-[#94a3b8]">{emptyMessage || "Aucun résultat"}</p>
              ) : (
                filteredOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => {
                      onChange([...value, option.value])
                      setSearch("")
                    }}
                    className="flex w-full items-center justify-between rounded-lg px-3 py-2 text-sm text-left hover:bg-[#f8fafc] text-[#1e293b] transition-colors"
                  >
                    {option.label}
                    {value.includes(option.value) && <Check className="h-4 w-4 text-[#00e5a0]" />}
                  </button>
                ))
              )}
            </div>
          </div>
        )}
      </div>
    )
  }
)
MultiCombobox.displayName = "MultiCombobox"

export { Combobox, MultiCombobox }
export type { ComboboxOption }