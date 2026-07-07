import * as React from "react"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { format, startOfMonth, endOfMonth, eachDayOfInterval, isSameMonth, isSameDay, isBefore, startOfWeek, endOfWeek } from "date-fns"
import { fr } from "date-fns/locale"
import { cn } from "@/lib/utils"

export type CalendarProps = {
  selected?: Date
  onSelect?: (date: Date) => void
  fromDate?: Date
  toDate?: Date
  className?: string
}

const WEEKDAYS = ["Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"]

const Calendar = ({ selected, onSelect, fromDate, toDate, className }: CalendarProps) => {
  const [currentMonth, setCurrentMonth] = React.useState(selected || new Date())
  const calendarRef = React.useRef<HTMLDivElement>(null)

  const days = React.useMemo(() => {
    const start = startOfWeek(startOfMonth(currentMonth), { weekStartsOn: 1 })
    const end = endOfWeek(endOfMonth(currentMonth), { weekStartsOn: 1 })
    return eachDayOfInterval({ start, end })
  }, [currentMonth])

  const prevMonth = () => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1))
  const nextMonth = () => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1))

  const isDisabled = (date: Date) => {
    if (fromDate && isBefore(date, fromDate)) return true
    if (toDate && isBefore(toDate, date)) return true
    return false
  }

  const handleSelect = (date: Date) => {
    if (isDisabled(date)) return
    onSelect?.(date)
  }

  return (
    <div
      ref={calendarRef}
      className={cn("p-4 rounded-xl border border-[#e2e8f0] bg-white shadow-lg z-50 w-max", className)}
    >
      <div className="flex items-center justify-between mb-4">
        <button
          type="button"
          onClick={prevMonth}
          className="p-2 rounded-lg hover:bg-[#f1f5f9] transition-colors"
        >
          <ChevronLeft className="h-4 w-4 text-[#64748b]" />
        </button>
        <span className="text-sm font-medium text-[#1e293b] capitalize" style={{ fontFamily: 'var(--font-mono)' }}>
          {format(currentMonth, "MMMM yyyy", { locale: fr })}
        </span>
        <button
          type="button"
          onClick={nextMonth}
          className="p-2 rounded-lg hover:bg-[#f1f5f9] transition-colors"
        >
          <ChevronRight className="h-4 w-4 text-[#64748b]" />
        </button>
      </div>

      <div className="grid grid-cols-7 gap-1 mb-2">
        {WEEKDAYS.map((day) => (
          <div key={day} className="text-center text-[11px] text-[#94a3b8] py-2" style={{ fontFamily: 'var(--font-mono)' }}>
            {day}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-1">
        {days.map((day) => {
          const disabled = isDisabled(day)
          const isSelected = selected && isSameDay(day, selected)
          const isCurrentMonth = isSameMonth(day, currentMonth)

          return (
            <button
              key={day.toISOString()}
              type="button"
              onClick={() => handleSelect(day)}
              disabled={disabled || !isCurrentMonth}
              className={cn(
                "h-9 w-9 rounded-lg text-sm transition-colors",
                !isCurrentMonth && "text-[#cbd5e1]",
                isCurrentMonth && !isSelected && !disabled && "hover:bg-[#f1f5f9] text-[#1e293b]",
                isSelected && "bg-[#00e5a0] text-[#0d0f14] font-medium",
                disabled && "opacity-40 cursor-not-allowed"
              )}
              style={{ fontFamily: 'var(--font-mono)' }}
            >
              {format(day, "d")}
            </button>
          )
        })}
      </div>
    </div>
  )
}

export { Calendar }