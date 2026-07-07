import * as React from 'react'
import * as ToggleGroupPrimitive from '@radix-ui/react-toggle-group'
import { cn } from '@/lib/utils'

const ToggleGroup = React.forwardRef<
  React.ElementRef<typeof ToggleGroupPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof ToggleGroupPrimitive.Root>
>(({ className, ...props }, ref) => (
  <ToggleGroupPrimitive.Root
    ref={ref}
    className={cn(
      'inline-flex items-center justify-center rounded-lg border border-[#e2e8f0] bg-white overflow-hidden',
      className
    )}
    {...props}
  />
))
ToggleGroup.displayName = ToggleGroupPrimitive.Root.displayName

const ToggleGroupItem = React.forwardRef<
  React.ElementRef<typeof ToggleGroupPrimitive.Item>,
  React.ComponentPropsWithoutRef<typeof ToggleGroupPrimitive.Item> & {
    variant?: 'default' | 'outline'
  }
>(({ className, variant = 'default', ...props }, ref) => {
  const base = 'flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-medium tracking-wide transition-all duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-[#00e5a0] focus-visible:ring-offset-1 disabled:pointer-events-none disabled:opacity-50'

  const variants = {
    default: cn(
      'data-[state=on]:bg-[#00e5a0] data-[state=on]:text-[#0d0f14] data-[state=on]:border-[#00e5a0]',
      'data-[state=off]:bg-white data-[state=off]:text-[#64748b] data-[state=off]:border-transparent',
      'hover:bg-[#f8fafc] text-[#64748b] border border-transparent'
    ),
    outline: cn(
      'data-[state=on]:bg-[#00e5a0]/10 data-[state=on]:text-[#00b07d] data-[state=on]:border-[#00e5a0]/30',
      'hover:bg-[#f8fafc] text-[#64748b] border border-transparent'
    ),
  }

  return (
    <ToggleGroupPrimitive.Item
      ref={ref}
      className={cn(base, variants[variant], className)}
      {...props}
    />
  )
})
ToggleGroupItem.displayName = ToggleGroupPrimitive.Item.displayName

export { ToggleGroup, ToggleGroupItem }