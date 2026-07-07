import { useState, useRef, useEffect } from 'react'
import { NavLink } from 'react-router-dom'
import { cn } from '@/lib/utils'

const NAV_ITEMS = [
  {
    to: '/',
    label: 'Tableau de bord',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="3" width="7" height="7" />
        <rect x="14" y="3" width="7" height="7" />
        <rect x="14" y="14" width="7" height="7" />
        <rect x="3" y="14" width="7" height="7" />
      </svg>
    ),
  },
  {
    to: '/zones',
    label: 'Zones',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
        <polyline points="9 22 9 12 15 12 15 22" />
      </svg>
    ),
  },
  {
    to: '/capteurs',
    label: 'Capteurs',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="3" />
        <path d="M6.3 6.3a8 8 0 0 0 0 11.4M17.7 6.3a8 8 0 0 1 0 11.4M3.5 3.5a13 13 0 0 0 0 17M20.5 3.5a13 13 0 0 1 0 17" />
      </svg>
    ),
  },
  {
    to: '/comparer',
    label: 'Comparer',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <line x1="18" y1="20" x2="18" y2="10" />
        <line x1="12" y1="20" x2="12" y2="4" />
        <line x1="6" y1="20" x2="6" y2="14" />
      </svg>
    ),
  },
  {
    to: '/carte',
    label: 'Carte',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" />
        <line x1="8" y1="2" x2="8" y2="18" />
        <line x1="16" y1="6" x2="16" y2="22" />
      </svg>
    ),
  },
  {
    to: '/types-capteur',
    label: 'Types de capteur',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="3" width="7" height="7" />
        <rect x="14" y="3" width="7" height="7" />
        <rect x="14" y="14" width="7" height="7" />
        <rect x="3" y="14" width="7" height="7" />
      </svg>
    ),
  },
  {
    to: '/kpis',
    label: 'KPIs',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <path d="M3 3v18h18" /><path d="m19 9-5 5-4-4-3 3" />
      </svg>
    ),
  },
]

// Mock notifications (à remplacer par appels API)
const MOCK_NOTIFICATIONS: { id: number; message: string; time: string; type: 'info' | 'warning' | 'error' }[] = [
  { id: 1, message: 'Capteur AIR_03 hors ligne depuis 15 min', time: 'il y a 5 min', type: 'warning' },
  { id: 2, message: 'Niveau sonore élevé zone Centre (72 dB)', time: 'il y a 12 min', type: 'error' },
  { id: 3, message: 'Nouveau capteur NOISE_07 enregistré', time: 'il y a 1h', type: 'info' },
]

interface SidebarProps {
  className?: string
}

const Sidebar = ({ className }: SidebarProps) => {
  const [notifOpen, setNotifOpen] = useState(false)
  const notifRef = useRef<HTMLDivElement>(null)

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (notifRef.current && !notifRef.current.contains(e.target as Node)) {
        setNotifOpen(false)
      }
    }
    if (notifOpen) {
      document.addEventListener('mousedown', handleClick)
    }
    return () => document.removeEventListener('mousedown', handleClick)
  }, [notifOpen])

  const unreadCount = MOCK_NOTIFICATIONS.length

  return (
    <aside
      className={cn("w-64 shrink-0 bg-white border-r border-[#e2e8f0] flex flex-col h-screen sticky top-0", className)}
      style={{ fontFamily: 'var(--font-display)' }}
    >
      {/* Logo */}
      <div className="px-6 py-8 border-b border-[#e2e8f0]">
        <span className="text-2xl font-bold tracking-wider text-[#0d0f14] uppercase">
          UrbanHub
        </span>
        <p className="mt-1 text-sm tracking-wide text-[#94a3b8] font-medium">
          Réseau de capteurs
        </p>
      </div>

      {/* Nav */}
      <nav className="flex-1 px-4 py-6 flex flex-col gap-1 overflow-y-auto">
        <p className="px-3 mb-3 text-[11px] tracking-[0.15em] text-[#94a3b8] uppercase font-semibold">
          Navigation
        </p>
        {NAV_ITEMS.map(({ to, label, icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 px-3 py-3 rounded-lg text-sm font-medium tracking-wide transition-all duration-150',
                isActive
                  ? 'bg-[#00e5a0]/10 text-[#00b07d] border border-[#00e5a0]/20'
                  : 'text-[#64748b] border border-transparent hover:text-[#1e293b] hover:bg-[#f1f5f9]'
              )
            }
          >
            {icon}
            <span className="tracking-wider text-sm">{label}</span>
          </NavLink>
        ))}
      </nav>

      {/* Footer with notifications */}
      <div className="border-t border-[#e2e8f0]" ref={notifRef}>
        {/* Notification button */}
        <button
          onClick={() => setNotifOpen(!notifOpen)}
          className="w-full px-6 py-4 flex items-center justify-between hover:bg-[#f8fafc] transition-colors duration-150"
        >
          <div className="flex items-center gap-2">
            <span style={{ fontFamily: 'var(--font-mono)' }} className="text-[11px] text-[#94a3b8] tracking-wider uppercase">
              Notifications
            </span>
          </div>
          <div className="flex items-center gap-2">
            <div className="relative">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" className="text-[#94a3b8]">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
              {unreadCount > 0 && (
                <span className="absolute -top-1.5 -right-1.5 w-4 h-4 bg-red-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center"
                  style={{ fontFamily: 'var(--font-mono)' }}>
                  {unreadCount}
                </span>
              )}
            </div>
            <svg
              width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
              className={cn("text-[#94a3b8] transition-transform duration-200", notifOpen && "rotate-180")}
            >
              <path d="m18 15-6-6-6 6" />
            </svg>
          </div>
        </button>

        {/* Notification dropdown */}
        {notifOpen && (
          <div className="border-t border-[#e2e8f0] bg-white">
            <div className="px-4 py-3">
              {MOCK_NOTIFICATIONS.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-6 gap-2">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"
                    className="text-[#cbd5e1]">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                    <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                  </svg>
                  <p className="text-xs text-[#94a3b8]" style={{ fontFamily: 'var(--font-mono)' }}>
                    Aucune notification
                  </p>
                </div>
              ) : (
                <div className="flex flex-col gap-1">
                  {MOCK_NOTIFICATIONS.map((n) => (
                    <div key={n.id}
                      className={cn(
                        "flex items-start gap-2.5 px-3 py-2.5 rounded-lg text-left",
                        n.type === 'error' ? 'bg-red-50 border border-red-100' :
                          n.type === 'warning' ? 'bg-amber-50 border border-amber-100' :
                            'bg-blue-50 border border-blue-100'
                      )}
                    >
                      <span className={cn(
                        "w-1.5 h-1.5 rounded-full mt-1.5 shrink-0",
                        n.type === 'error' ? 'bg-red-500' :
                          n.type === 'warning' ? 'bg-amber-500' :
                            'bg-blue-500'
                      )} />
                      <div className="min-w-0">
                        <p className="text-[11px] text-[#334155] leading-snug"
                          style={{ fontFamily: 'var(--font-mono)' }}>
                          {n.message}
                        </p>
                        <p className="text-[9px] text-[#94a3b8] mt-0.5"
                          style={{ fontFamily: 'var(--font-mono)' }}>
                          {n.time}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </aside>
  )
}

export { Sidebar }