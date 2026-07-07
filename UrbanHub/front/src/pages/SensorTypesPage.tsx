import { Link } from 'react-router-dom'
import { useMemo } from 'react'
import { useSensors } from '../queries/sensorQueries'
import { Card, CardContent } from '@/components/ui/card'
import { Breadcrumb } from '@/components/ui/breadcrumb'
import { Skeleton } from '@/components/ui/skeleton'

const SENSOR_TYPE_ICONS: Record<string, React.ReactNode> = {
  AIR: (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.7 7.7a8 8 0 1 1-11.4 0" />
      <path d="M9.5 4A5.5 5.5 0 0 1 15 9.5" />
      <path d="M6 13l4-4 4 4 3-3" />
    </svg>
  ),
  NOISE: (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
      <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
      <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
    </svg>
  ),
  TRAFFIC: (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <rect x="1" y="3" width="15" height="13" rx="2" />
      <path d="m16 8 3-3v6l-3-3" />
      <circle cx="5.5" cy="18.5" r="2.5" />
      <circle cx="18.5" cy="18.5" r="2.5" />
    </svg>
  ),
  WEATHER: (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z" />
    </svg>
  ),
}

const DEFAULT_ICON = (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="3" />
    <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
  </svg>
)

const SensorTypesPage = () => {
  const { data: sensors, isLoading: sensorsLoading, isError: sensorsError } = useSensors()

  const typesWithCount = useMemo(() => {
    if (!sensors) return []
    const map = new Map<string, number>()
    sensors.forEach((s) => {
      map.set(s.sensorTypeId, (map.get(s.sensorTypeId) || 0) + 1)
    })
    return Array.from(map.entries())
      .map(([typeId, count]) => ({ typeId, count }))
      .sort((a, b) => a.typeId.localeCompare(b.typeId))
  }, [sensors])

  return (
    <div>
      <Breadcrumb items={[{ label: 'Types de capteur' }]} className="mb-6" />
      <header className="mb-8">
        <p className="text-[11px] text-[#00b07d] tracking-[0.2em] uppercase mb-2" style={{ fontFamily: 'var(--font-mono)' }}>
          Classification
        </p>
        <h1 className="text-5xl font-bold tracking-wider text-[#0d0f14] uppercase" style={{ fontFamily: 'var(--font-display)' }}>
          Types de capteur
        </h1>
        <div className="mt-4 h-1 w-20 bg-[#00e5a0]" />
      </header>

      {sensorsLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {[1, 2, 3, 4].map(i => (
            <Skeleton key={i} className="h-32 w-full" />
          ))}
        </div>
      )}
      {sensorsError && (
        <p className="text-sm text-red-500 tracking-wider" style={{ fontFamily: 'var(--font-mono)' }}>
          Erreur de connexion au backend.
        </p>
      )}
      {sensors && !sensorsLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {typesWithCount.map(({ typeId, count }) => (
            <Link key={typeId} to={`/capteurs?type=${typeId}`}>
              <Card className="hover:border-[#00e5a0]/50 hover:shadow-md transition-all duration-200 group cursor-pointer">
                <CardContent className="p-8">
                  <div className="flex items-start gap-5">
                    <div className="w-14 h-14 rounded-xl bg-[#00e5a0]/10 flex items-center justify-center text-[#00b07d] shrink-0 group-hover:bg-[#00e5a0]/20 transition-colors">
                      {SENSOR_TYPE_ICONS[typeId.toUpperCase()] || DEFAULT_ICON}
                    </div>
                    <div className="flex-1 min-w-0">
                      <h2 className="text-xl font-bold tracking-wider text-[#0d0f14] uppercase mb-1" style={{ fontFamily: 'var(--font-display)' }}>
                        {typeId}
                      </h2>
                      <p className="text-sm text-[#94a3b8] tracking-wider" style={{ fontFamily: 'var(--font-mono)' }}>
                        {count} mesure{count > 1 ? 's' : ''}
                      </p>
                    </div>
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-[#94a3b8] group-hover:text-[#00b07d] transition-colors shrink-0 mt-1">
                      <polyline points="9 18 15 12 9 6" />
                    </svg>
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}

export default SensorTypesPage