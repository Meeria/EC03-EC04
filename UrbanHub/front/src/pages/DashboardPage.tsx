import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useMeasuresCount, useMeasuresByDateRange } from '../queries/measureQueries'
import { useSensorStatusCount } from '../queries/sensorQueries'
import { Card, CardContent } from '@/components/ui/card'
import { Breadcrumb } from '@/components/ui/breadcrumb'
import { Badge } from '@/components/ui/badge'
import { AlertTriangle } from 'lucide-react'

const today = new Date().toISOString().split('T')[0]
const yesterday = new Date(Date.now() - 86_400_000).toISOString().split('T')[0]

const formatCount = (n: number): string => {
  if (n >= 1_000_000) {
    const val = n / 1_000_000
    return val % 1 === 0 ? `${val}M` : `${val.toFixed(3).replace(/\.?0+$/, '')}M`
  }
  if (n >= 1_000) {
    const val = n / 1_000
    return val % 1 === 0 ? `${val}k` : `${val.toFixed(1)}k`
  }
  return String(n)
}

const DashboardPage = () => {
  // Total count: instant (1 integer), no need to load all measures
  const { data: measureCount } = useMeasuresCount()

  // Last 2 days only instead of full history — much smaller payload,
  // covers "no data today yet" cases and gives us sensor status + values
  const { data } = useMeasuresByDateRange(yesterday, today)
  const { data: activeSensorCount } = useSensorStatusCount(true)
  const { data: inactiveSensorCount } = useSensorStatusCount(false)

  const stats = useMemo(() => {
    if (!data || data.length === 0) {
      return {
        sensorCount: 0,
        activeSensors: 0,
        inactiveSensors: 0,
        uptimePercent: 0,
        typeCount: 0,
        typeBreakdown: [] as { type: string; count: number }[],
        mostActiveSensor: null as string | null,
        mostActiveMeasureCount: 0,
        typeStats: [] as { type: string; unit: string; avg: number; latest: number; lastTimestamp: string }[],
      }
    }

    const sensorMap = new Map<string, boolean>()
    const measureCountBySensor = new Map<string, number>()
    const measureCountByType = new Map<string, number>()
    const typeValues = new Map<string, number[]>()
    const typeUnits = new Map<string, string>()
    const typeLatest = new Map<string, { value: number; timestamp: string }>()

    const sorted = [...data].sort(
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime(),
    )

    sorted.forEach((m) => {
      sensorMap.set(m.sensorId, m.sensorStatus)
      measureCountBySensor.set(m.sensorId, (measureCountBySensor.get(m.sensorId) ?? 0) + 1)
      measureCountByType.set(m.sensorTypeId, (measureCountByType.get(m.sensorTypeId) ?? 0) + 1)

      if (!typeValues.has(m.sensorTypeId)) {
        typeValues.set(m.sensorTypeId, [])
        typeUnits.set(m.sensorTypeId, m.unit)
      }
      typeValues.get(m.sensorTypeId)!.push(m.value)
      typeLatest.set(m.sensorTypeId, { value: m.value, timestamp: m.timestamp })
    })

    const sensorCount = sensorMap.size
    const activeSensors = Array.from(sensorMap.values()).filter(Boolean).length
    const inactiveSensors = sensorCount - activeSensors
    const uptimePercent = sensorCount > 0 ? Math.round((activeSensors / sensorCount) * 100) : 0
    const typeCount = typeValues.size

    const typeBreakdown = Array.from(measureCountByType.entries())
      .map(([type, count]) => ({ type, count }))
      .sort((a, b) => b.count - a.count)

    let mostActiveSensor: string | null = null
    let mostActiveMeasureCount = 0
    measureCountBySensor.forEach((count, sensorId) => {
      if (count > mostActiveMeasureCount) {
        mostActiveMeasureCount = count
        mostActiveSensor = sensorId
      }
    })

    const typeStats = Array.from(typeValues.entries())
      .map(([type, values]) => {
        const avg = values.reduce((a, b) => a + b, 0) / values.length
        const { value: latest, timestamp: lastTimestamp } = typeLatest.get(type)!
        return { type, unit: typeUnits.get(type)!, avg, latest, lastTimestamp }
      })
      .sort((a, b) => a.type.localeCompare(b.type))

    return {
      sensorCount,
      activeSensors,
      inactiveSensors,
      uptimePercent,
      typeCount,
      typeBreakdown,
      mostActiveSensor,
      mostActiveMeasureCount,
      typeStats,
      recentMeasureCount: data.length
    }
  }, [data])

  const availability = useMemo(() => {
    const activeSensors = activeSensorCount ?? stats.activeSensors
    const inactiveSensors = inactiveSensorCount ?? stats.inactiveSensors
    const sensorCount = activeSensors + inactiveSensors
    const uptimePercent = sensorCount > 0 ? Math.round((activeSensors / sensorCount) * 100) : 0

    return {
      activeSensors,
      inactiveSensors,
      sensorCount,
      uptimePercent,
    }
  }, [activeSensorCount, inactiveSensorCount, stats.activeSensors, stats.inactiveSensors])

  const showOfflineAlert = availability.inactiveSensors > 0

  return (
    <div>
      <Breadcrumb items={[{ label: 'Tableau de bord' }]} className="mb-6" />
      <header className="mb-10">
        <p className="text-[12px] text-[#00b07d] tracking-[0.2em] uppercase mb-2" style={{ fontFamily: 'var(--font-mono)' }}>
          Vue d'ensemble
        </p>
        <h1 className="text-5xl font-bold tracking-wider text-[#0d0f14] uppercase" style={{ fontFamily: 'var(--font-display)' }}>
          Tableau de bord
        </h1>
        <div className="mt-4 h-1 w-20 bg-[#00e5a0]" />
      </header>

      {/* Offline alert banner */}
      {showOfflineAlert && (
        <div className="mb-6 flex items-center gap-3 px-5 py-4 rounded-xl border border-amber-200 bg-amber-50">
          <AlertTriangle className="h-5 w-5 text-amber-500 shrink-0" />
          <p className="text-sm text-amber-700 tracking-wide" style={{ fontFamily: 'var(--font-mono)' }}>
            <span className="font-semibold">{availability.inactiveSensors} capteur{availability.inactiveSensors > 1 ? 's' : ''} hors-ligne</span>
            {' '}— vérifiez l'état du réseau ou des dispositifs.
          </p>
          <Link
            to="/capteurs"
            className="ml-auto text-xs text-amber-600 hover:text-amber-800 underline underline-offset-2 tracking-wider transition-colors shrink-0"
            style={{ fontFamily: 'var(--font-mono)' }}
          >
            Voir les capteurs →
          </Link>
        </div>
      )}

      {/* Stat cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mb-10">

        {/* Capteurs card */}
        <Card className="p-8">
          <CardContent className="flex items-start gap-5 p-0">
            <div className="w-14 h-14 rounded-xl bg-[#00e5a0]/10 flex items-center justify-center text-[#00b07d] shrink-0">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="3" />
                <path d="M6.3 6.3a8 8 0 0 0 0 11.4M17.7 6.3a8 8 0 0 1 0 11.4M3.5 3.5a13 13 0 0 0 0 17M20.5 3.5a13 13 0 0 1 0 17" />
              </svg>
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[11px] text-[#94a3b8] tracking-[0.15em] uppercase mb-1" style={{ fontFamily: 'var(--font-mono)' }}>
                Capteurs
              </p>
              <p className="text-4xl font-bold tracking-wider text-[#0d0f14]" style={{ fontFamily: 'var(--font-display)' }}>
                {availability.sensorCount}
              </p>
              {availability.sensorCount > 0 && (
                <div className="flex items-center gap-1.5 mt-2">
                  <span className="relative flex h-2 w-2 shrink-0">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#00e5a0] opacity-75" />
                    <span className="relative inline-flex rounded-full h-2 w-2 bg-[#00e5a0]" />
                  </span>
                  <span className="text-xs text-[#64748b] tracking-wider" style={{ fontFamily: 'var(--font-mono)' }}>
                    <span className="text-[#00b07d] font-semibold">{availability.activeSensors}</span>
                    /{availability.sensorCount} en ligne
                  </span>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Taux de disponibilité card */}
        <Card className="p-8">
          <CardContent className="flex items-start gap-5 p-0">
            <div className="w-14 h-14 rounded-xl bg-[#00e5a0]/10 flex items-center justify-center text-[#00b07d] shrink-0">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z" />
                <path d="M12 6v6l4 2" />
              </svg>
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[11px] text-[#94a3b8] tracking-[0.15em] uppercase mb-1" style={{ fontFamily: 'var(--font-mono)' }}>
                Disponibilité
              </p>
              <p className="text-4xl font-bold tracking-wider text-[#0d0f14]" style={{ fontFamily: 'var(--font-display)' }}>
                {availability.uptimePercent}
                <span className="text-xl text-[#94a3b8] ml-1">%</span>
              </p>
              {availability.sensorCount > 0 && (
                <div className="mt-3">
                  <div className="h-1.5 w-full rounded-full bg-[#e2e8f0] overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-700"
                      style={{
                        width: `${availability.uptimePercent}%`,
                        background: availability.uptimePercent === 100
                          ? '#00e5a0'
                          : availability.uptimePercent >= 80
                          ? '#00e5a0'
                          : availability.uptimePercent >= 50
                          ? '#f59e0b'
                          : '#ef4444',
                      }}
                    />
                  </div>
                  <p className="text-[10px] text-[#94a3b8] tracking-wider mt-1.5" style={{ fontFamily: 'var(--font-mono)' }}>
                    {availability.activeSensors} actif{availability.activeSensors > 1 ? 's' : ''} · {availability.inactiveSensors} inactif{availability.inactiveSensors > 1 ? 's' : ''}
                  </p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Mesures card */}
        <Card className="p-8">
          <CardContent className="flex items-start gap-5 p-0">
            <div className="w-14 h-14 rounded-xl bg-[#00e5a0]/10 flex items-center justify-center text-[#00b07d] shrink-0">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
              </svg>
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[11px] text-[#94a3b8] tracking-[0.15em] uppercase mb-1" style={{ fontFamily: 'var(--font-mono)' }}>
                Mesures
              </p>
              <p className="text-4xl font-bold tracking-wider text-[#0d0f14]" style={{ fontFamily: 'var(--font-display)' }}>
                {measureCount !== undefined ? formatCount(measureCount) : '—'}
              </p>
              {stats.mostActiveSensor && (
                <p className="text-[10px] text-[#94a3b8] tracking-wider mt-2 truncate" style={{ fontFamily: 'var(--font-mono)' }}>
                  Top :{' '}
                  <Link
                    to={`/capteurs/${stats.mostActiveSensor}`}
                    className="text-[#00b07d] hover:underline underline-offset-2 uppercase font-semibold"
                  >
                    {stats.mostActiveSensor}
                  </Link>
                  {' '}({stats.mostActiveMeasureCount} mesures récentes)
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Types de capteur card — full width on md, 3rd col on lg */}
        <Card className="p-8 sm:col-span-2 lg:col-span-3">
          <CardContent className="p-0">
            <div className="flex items-start gap-5">
              <div className="w-14 h-14 rounded-xl bg-[#00e5a0]/10 flex items-center justify-center text-[#00b07d] shrink-0">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="3" width="7" height="7" />
                  <rect x="14" y="3" width="7" height="7" />
                  <rect x="14" y="14" width="7" height="7" />
                  <rect x="3" y="14" width="7" height="7" />
                </svg>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-[11px] text-[#94a3b8] tracking-[0.15em] uppercase mb-1" style={{ fontFamily: 'var(--font-mono)' }}>
                  Types de capteur
                </p>
                <p className="text-4xl font-bold tracking-wider text-[#0d0f14] mb-3" style={{ fontFamily: 'var(--font-display)' }}>
                  {stats.typeCount}
                </p>
                {stats.typeBreakdown.length > 0 && (
                  <div className="flex flex-wrap gap-2">
                    {stats.typeBreakdown.map(({ type, count }, i) => {
                      const palette = [
                        'bg-[#00e5a0]/10 text-[#00b07d] border-[#00e5a0]/30',
                        'bg-blue-50 text-blue-600 border-blue-200',
                        'bg-violet-50 text-violet-600 border-violet-200',
                        'bg-amber-50 text-amber-600 border-amber-200',
                        'bg-rose-50 text-rose-600 border-rose-200',
                        'bg-cyan-50 text-cyan-600 border-cyan-200',
                      ]
                      const cls = palette[i % palette.length]
                      return (
                        <Badge
                          key={type}
                          className={`border text-xs tracking-wider uppercase ${cls}`}
                          style={{ fontFamily: 'var(--font-mono)' }}
                        >
                          {type} ×{count}
                        </Badge>
                      )
                    })}
                  </div>
                )}
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

export default DashboardPage
