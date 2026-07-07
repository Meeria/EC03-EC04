import { Toaster } from 'sonner'

export function Sonner() {
  return (
    <Toaster
      position="bottom-right"
      toastOptions={{
        style: {
          fontFamily: 'var(--font-mono)',
          fontSize: '13px',
          borderRadius: '10px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
        },
        className: 'text-[#334155]',
      }}
      invert
      theme="light"
    />
  )
}