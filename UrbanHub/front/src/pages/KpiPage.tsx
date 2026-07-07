import { useState, useEffect, useMemo } from 'react'
import { Card, CardContent } from '@/components/ui/card'
import { Breadcrumb } from '@/components/ui/breadcrumb'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select'
import { Combobox } from '@/components/ui/combobox'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { Skeleton } from '@/components/ui/skeleton'
import { useSensors } from '@/queries/sensorQueries'
import { useZones } from '@/queries/zoneQueries'
import { useSensorTrendLatest, useSensorTrend24h, useSensorTrendPeriod, useZoneTrendPeriod } from '@/queries/trendQueries'
import { useKpiByZone, useKpiBySensor, useKpiByType } from '@/queries/kpiQueries'
import type { KpiDto } from '@/services/kpiService'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts'

type Period = '1h' | '24h' | '1week'
type Unit = 'percent' | 'unit'
type Selector = 'zone' | 'sensorType' | 'sensor'
type Granularity = '1h' | '24h' | '1week'

function isoRange(period: Period): { start: string; end: string } {
  const end = new Date()
  const start = new Date()
  if (period === '1h') start.setHours(start.getHours() - 1)
  else if (period === '24h') start.setDate(start.getDate() - 1)
  else start.setDate(start.getDate() - 7)
  return { start: start.toISOString(), end: end.toISOString() }
}

// Zones don't have a dedicated 1h endpoint, always use period with a wider window for 1h
function zoneRange(period: Period): { start: string; end: string } {
  const end = new Date()
  const start = new Date()
  if (period === '1h') start.setDate(start.getDate() - 2)     // 48h → enough data from DataSeeder
  else if (period === '24h') start.setDate(start.getDate() - 1)
  else start.setDate(start.getDate() - 7)
  return { start: start.toISOString(), end: end.toISOString() }
}

const PERIODS: { value: Period; label: string }[] = [
  { value: '1h', label: '1 heure' },
  { value: '24h', label: '24 heures' },
  { value: '1week', label: '1 semaine' },
]

const GRANULARITIES: { value: Granularity; label: string }[] = [
  { value: '1h', label: '1h' },
  { value: '24h', label: '24h' },
  { value: '1week', label: '1 semaine' },
]

// Map API measure types to chart-friendly keys and colors
const TYPE_CHART_META: Record<string, { key: string; label: string; color: string }> = {
  AIR:     { key: 'air',    label: 'Air (μg/m³)',   color: '#f59e0b' },
  NOISE:   { key: 'noise',  label: 'Bruit (dB)',      color: '#ef4444' },
  TRAFFIC: { key: 'traffic', label: 'Trafic (km/h)',  color: '#00b07d' },
  WEATHER: { key: 'weather', label: 'Météo (°C)',     color: '#3b82f6' },
}

function formatBucketLabel(iso: string, granularity: Granularity): string {
  const d = new Date(iso)
  if (granularity === '1h') {
    return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', hour12: false })
  }
  return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' })
}

function kpiToChartData(kpis: KpiDto[], granularity: Granularity): Record<string, number | string>[] {
  return kpis.map(kpi => ({
    label: formatBucketLabel(kpi.bucket, granularity),
    value: kpi.average,
    unite: kpi.unite,
  }))
}

function buildZoneChartData(
  data: Record<string, KpiDto[]>,
  granularity: Granularity
): Record<string, number | string>[] {
  // Collect all unique bucket labels
  const bucketLabels = new Set<string>()
  Object.values(data).forEach(kpis => kpis.forEach(k => bucketLabels.add(formatBucketLabel(k.bucket, granularity))))

  return Array.from(bucketLabels).sort().map(label => {
    const row: Record<string, number | string> = { label }
    Object.entries(data).forEach(([type, kpis]) => {
      const meta = TYPE_CHART_META[type.toUpperCase()]
      if (!meta) return
      const kpi = kpis.find(k => formatBucketLabel(k.bucket, granularity) === label)
      if (kpi) row[meta.key] = kpi.average
    })
    return row
  })
}

function ChartBars({ data, unit }: { data: Record<string, number | string>[]; unit?: string }) {
  const types = Object.keys(TYPE_CHART_META).filter(t =>
    data.some(d => d[TYPE_CHART_META[t].key] !== undefined)
  )
  if (types.length === 0) {
    return (
      <ResponsiveContainer width="100%" height={280}>
        <BarChart data={data} margin={{ top: 5, right: 5, left: -20, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
          <XAxis dataKey="label" tick={{ fontSize: 10, fill: '#94a3b8', fontFamily: 'var(--font-mono)' }}
            axisLine={false} tickLine={false} />
          <YAxis tick={{ fontSize: 10, fill: '#94a3b8', fontFamily: 'var(--font-mono)' }}
            axisLine={false} tickLine={false} />
          <Tooltip
            contentStyle={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 10, fontSize: 12, fontFamily: 'var(--font-mono)' }}
            formatter={(val) => [`${Number(val).toFixed(1)}${unit ? ' ' + unit : ''}`, 'Moyenne']}
          />
          <Bar dataKey="value" fill="#00e5a0" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    )
  }
  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={data} margin={{ top: 5, right: 5, left: -20, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
        <XAxis dataKey="label" tick={{ fontSize: 10, fill: '#94a3b8', fontFamily: 'var(--font-mono)' }}
          axisLine={false} tickLine={false} />
        <YAxis tick={{ fontSize: 10, fill: '#94a3b8', fontFamily: 'var(--font-mono)' }}
          axisLine={false} tickLine={false} />
        <Tooltip
          contentStyle={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 10, fontSize: 12, fontFamily: 'var(--font-mono)' }}
          formatter={(value, key) => [`${Number(value).toFixed(1)}${unit ? ' ' + unit : ''}`, String(key)]}
        />
        <Legend wrapperStyle={{ fontSize: 11, fontFamily: 'var(--font-mono)', color: '#94a3b8' }} />
        {types.map(t => {
          const meta = TYPE_CHART_META[t]
          return (
            <Bar key={t} dataKey={meta.key} name={meta.label} fill={meta.color} radius={[4, 4, 0, 0]} />
          )
        })}
      </BarChart>
    </ResponsiveContainer>
  )
}

function MoyenneModule() {
  const { data: sensors } = useSensors()
  const { data: zones } = useZones()

  const [selector, setSelector] = useState<Selector>('zone')
  const [selectedId, setSelectedId] = useState<string>('')
  const [granularity, setGranularity] = useState<Granularity>('1week')
  const [dateFrom, setDateFrom] = useState<string>(() => {
    const d = new Date(); d.setDate(d.getDate() - 7)
    return d.toISOString().split('T')[0]
  })
  const [dateTo, setDateTo] = useState<string>(() => new Date().toISOString().split('T')[0])

  // Auto-select first option when data loads
  useEffect(() => {
    if (!selectedId) {
      if (selector === 'zone' && zones && zones.length > 0) setSelectedId(zones[0].zoneId)
      else if (selector === 'sensorType' && sensors && sensors.length > 0) {
        const types = Array.from(new Set(sensors.map((s: { sensorTypeId: string }) => s.sensorTypeId)))
        if (types.length > 0) setSelectedId(types[0])
      } else if (selector === 'sensor' && sensors && sensors.length > 0) {
        if (sensors[0]) setSelectedId(sensors[0].sensorId)
      }
    }
  }, [selector, zones, sensors, selectedId])

  // Build ISO datetime strings for API
  const startIso = dateFrom ? `${dateFrom}T00:00:00` : ''
  const endIso = dateTo ? `${dateTo}T23:59:59` : ''

  // API calls per selector type
  const { data: zoneData, isLoading: isoZoneLoading } = useKpiByZone(selectedId, startIso, endIso, granularity)
  const { data: sensorData, isLoading: isSensorLoading } = useKpiBySensor(selectedId, startIso, endIso, granularity)
  const { data: typeData, isLoading: isTypeLoading } = useKpiByType(selectedId, startIso, endIso, granularity)

  const isLoadingData = selector === 'zone' ? isoZoneLoading : selector === 'sensorType' ? isTypeLoading : isSensorLoading

  const selectorOptions = selector === 'zone'
    ? (zones ?? []).map(z => ({ value: z.zoneId, label: z.zoneId }))
    : selector === 'sensorType'
      ? Array.from(new Set((sensors ?? []).map((s: { sensorTypeId: string }) => s.sensorTypeId))).map(t => ({ value: t, label: t }))
      : (sensors ?? []).map(s => ({ value: s.sensorId, label: s.sensorId }))

  const selectedLabel = selectorOptions.find(o => o.value === selectedId)?.label ?? ''

  // Build chart data based on selector
  const chartData = useMemo(() => {
    if (!selectedId) return null
    if (selector === 'zone' && zoneData) return buildZoneChartData(zoneData, granularity)
    if (selector === 'sensor' && sensorData) return kpiToChartData(sensorData, granularity)
    if (selector === 'sensorType' && typeData) return kpiToChartData(typeData, granularity)
    return null
  }, [selector, selectedId, zoneData, sensorData, typeData, granularity])

  const unitLabel = selector === 'zone' ? '' : (typeData?.[0]?.unite ?? sensorData?.[0]?.unite ?? '')

  return (
    <Card className="p-6">
      <CardContent className="p-0 flex flex-col gap-5">
        {/* Filters row */}
        <div className="flex flex-wrap gap-4 items-end">
          <div className="flex flex-col gap-2">
            <label className="text-[10px] text-[#94a3b8] tracking-wider uppercase" style={{ fontFamily: 'var(--font-mono)' }}>
              Sélectionner
            </label>
            <ToggleGroup
              type="single"
              value={selector}
              onValueChange={(v) => {
                if (v) {
                  setSelector(v as Selector)
                  setSelectedId('')
                }
              }}
            >
              <ToggleGroupItem value="zone" variant="outline" className="h-8 px-3 text-xs">
                Zone
              </ToggleGroupItem>
              <ToggleGroupItem value="sensorType" variant="outline" className="h-8 px-3 text-xs">
                Type
              </ToggleGroupItem>
              <ToggleGroupItem value="sensor" variant="outline" className="h-8 px-3 text-xs">
                Capteur
              </ToggleGroupItem>
            </ToggleGroup>
          </div>

          <div className="flex flex-col gap-2 min-w-[200px]">
            <label className="text-[10px] text-[#94a3b8] tracking-wider uppercase" style={{ fontFamily: 'var(--font-mono)' }}>
              {selector === 'zone' ? 'Zone' : selector === 'sensorType' ? 'Type de capteur' : 'Capteur'}
            </label>
            <Combobox
              options={selectorOptions}
              value={selectedId}
              onChange={setSelectedId}
              placeholder="Rechercher..."
            />
          </div>

          <div className="flex flex-col gap-2">
            <label className="text-[10px] text-[#94a3b8] tracking-wider uppercase" style={{ fontFamily: 'var(--font-mono)' }}>
              Granularité
            </label>
            <Select value={granularity} onValueChange={(v) => setGranularity(v as Granularity)}>
              <SelectTrigger className="h-11 text-sm w-full min-w-[130px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {GRANULARITIES.map(g => (
                  <SelectItem key={g.value} value={g.value}>{g.label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex flex-col gap-2">
            <label className="text-[10px] text-[#94a3b8] tracking-wider uppercase" style={{ fontFamily: 'var(--font-mono)' }}>
              Du
            </label>
            <input
              type="date"
              value={dateFrom}
              onChange={(e) => setDateFrom(e.target.value)}
              className="h-11 rounded-xl border border-[#e2e8f0] bg-white px-4 text-sm text-[#1e293b] focus:outline-none focus:ring-2 focus:ring-[#00e5a0]"
              style={{ fontFamily: 'var(--font-mono)' }}
            />
          </div>

          <div className="flex flex-col gap-2">
            <label className="text-[10px] text-[#94a3b8] tracking-wider uppercase" style={{ fontFamily: 'var(--font-mono)' }}>
              Au
            </label>
            <input
              type="date"
              value={dateTo}
              onChange={(e) => setDateTo(e.target.value)}
              className="h-11 rounded-xl border border-[#e2e8f0] bg-white px-4 text-sm text-[#1e293b] focus:outline-none focus:ring-2 focus:ring-[#00e5a0]"
              style={{ fontFamily: 'var(--font-mono)' }}
            />
          </div>
        </div>

        {/* Bar chart */}
        <div className="w-full rounded-xl border border-[#e2e8f0] bg-white p-4">
          {!selectedId ? (
            <div className="flex items-center justify-center h-[280px]">
              <p className="text-xs text-[#94a3b8] italic tracking-wider" style={{ fontFamily: 'var(--font-mono)' }}>
                Sélectionnez une {selector === 'zone' ? 'zone' : selector === 'sensorType' ? 'type de capteur' : 'capteur'} pour voir la moyenne
              </p>
            </div>
          ) : isLoadingData ? (
            <div className="flex items-center justify-center h-[280px]">
              <Skeleton className="h-40 w-full rounded-xl" />
            </div>
          ) : !chartData || chartData.length === 0 ? (
            <div className="flex items-center justify-center h-[280px]">
               <p className="text-xs text-[#94a3b8] italic tracking-wider" style={{ fontFamily: 'var(--font-mono)' }}>
                 Données insuffisantes pour cette période.
               </p>
            </div>
          ) : (
            <>
              <p className="text-[11px] text-[#94a3b8] tracking-wider uppercase mb-3" style={{ fontFamily: 'var(--font-mono)' }}>
                Moyenne — {selectedLabel} — {granularity === '1h' ? 'par heure' : granularity === '24h' ? 'par jour' : 'par semaine'}
              </p>
              <ChartBars data={chartData} unit={unitLabel} />
            </>
          )}
        </div>
      </CardContent>
    </Card>
  )
}

function TrendModule({ title, entity }: { title: string; entity: 'sensor' | 'zone' }) {
  const { data: sensors } = useSensors()
  const { data: zones } = useZones()
  const [selected, setSelected] = useState<string>('')
  const [period, setPeriod] = useState<Period>('24h')
  const [unit, setUnit] = useState<Unit>('percent')

  const { start, end } = useMemo(() => isoRange(period), [period])
  const { start: zoneStart, end: zoneEnd } = useMemo(() => zoneRange(period), [period])

  const { data: trendLatest } = useSensorTrendLatest(selected)
  const { data: trend24h } = useSensorTrend24h(selected)
  const { data: trendPeriod } = useSensorTrendPeriod(selected, start, end)
  const { data: zoneTrends } = useZoneTrendPeriod(selected, zoneStart, zoneEnd)

  useEffect(() => {
    if (entity === 'sensor' && sensors && sensors.length > 0 && !selected) {
      setSelected(sensors[0].sensorId)
    }
    if (entity === 'zone' && zones && zones.length > 0 && !selected) {
      setSelected(zones[0].zoneId)
    }
  }, [entity, sensors, zones, selected])

  const options = entity === 'sensor'
    ? (sensors ?? []).map(s => ({ value: s.sensorId, label: s.sensorId }))
    : (zones ?? []).map(z => ({ value: z.zoneId, label: z.zoneId }))

  // Zones always use period endpoint; sensors use latest/24h/period depending on period
  const trend = entity === 'zone'
    ? (zoneTrends && zoneTrends.length > 0 ? zoneTrends[0] : null)
    : period === '1h'
      ? trendLatest
      : period === '24h'
        ? trend24h
        : trendPeriod

  // Determine unit label from sensor type
  const selectedSensor = (sensors ?? []).find(s => s.sensorId === selected)
  const selectedZone = (zones ?? []).find(z => z.zoneId === selected)
  const firstSensorInZone = selectedZone?.sensors?.[0]
  
  const TYPE_UNIT: Record<string, string> = { AIR: 'μg/m³', NOISE: 'dB', TRAFFIC: 'km/h', WEATHER: '°C' }
  const sensorTypeToUse = selectedSensor?.sensorTypeId || firstSensorInZone?.sensorTypeId
  const unitTypeLabel = sensorTypeToUse ? (TYPE_UNIT[sensorTypeToUse] ?? '') : ''

  const displayValue = trend
    ? (trend.changePercent >= 0 ? '+' : '') + trend.changePercent.toFixed(1)
    : null
  const unitLabel = trend
    ? unit === 'percent'
      ? '%'
      : unitTypeLabel || ((trend.changeAbsolute >= 0 ? '+' : '') + trend.changeAbsolute.toFixed(1))
    : ''
  const valueColor = trend
    ? trend.changePercent >= 0 ? 'text-[#00b07d]' : 'text-red-500'
    : 'text-[#94a3b8]'
  const isLoading = !trend && selected !== ''

  return (
    <Card className="p-6">
      <CardContent className="p-0 flex flex-col items-center gap-4">
        {/* Header row */}
        <div className="flex items-center justify-between w-full gap-3">
          <p className="text-[11px] text-[#94a3b8] tracking-[0.15em] uppercase" style={{ fontFamily: 'var(--font-mono)' }}>
            {title}
          </p>
          <ToggleGroup
            type="single"
            value={unit}
            onValueChange={(v) => { if (v) setUnit(v as Unit) }}
          >
            <ToggleGroupItem value="percent" variant="outline" className="h-6 px-2 text-[10px]">
              %
            </ToggleGroupItem>
            <ToggleGroupItem value="unit" variant="outline" className="h-6 px-2 text-[10px]">
              {unitTypeLabel || 'Valeur'}
            </ToggleGroupItem>
          </ToggleGroup>
        </div>

        {/* Filters row */}
        <div className="flex flex-wrap gap-3 w-full">
          <div className="flex-1 min-w-[140px]">
            <label className="text-[10px] text-[#94a3b8] tracking-wider uppercase mb-1.5 block" style={{ fontFamily: 'var(--font-mono)' }}>
              {entity === 'sensor' ? 'Capteur' : 'Zone'}
            </label>
            <Combobox
              options={options}
              value={selected}
              onChange={setSelected}
              placeholder="Rechercher..."
            />
          </div>
          <div className="flex-1 min-w-[120px]">
            <label className="text-[10px] text-[#94a3b8] tracking-wider uppercase mb-1.5 block" style={{ fontFamily: 'var(--font-mono)' }}>
              Période
            </label>
            <Select value={period} onValueChange={(v) => setPeriod(v as Period)}>
              <SelectTrigger className="h-11 text-sm w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {PERIODS.map(p => (
                  <SelectItem key={p.value} value={p.value}>{p.label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Big value square */}
        <div className="w-full rounded-xl border-[#e2e8f0] bg-white flex flex-row items-center justify-center py-8 px-4 mt-2">
          {isLoading ? (
            <Skeleton className="h-12 w-32" />
          ) : displayValue !== null ? (
            <>
              <p className={`text-5xl font-bold tracking-wider leading-none ${valueColor}`} style={{ fontFamily: 'var(--font-display)' }}>
                {displayValue}
              </p>
              <p className="text-[14px] text-[#94a3b8] ml-2 mt-1" style={{ fontFamily: 'var(--font-mono)' }}>
                {unitLabel}
              </p>
            </>
          ) : (
            <p className="text-xs text-[#94a3b8] italic tracking-wider" style={{ fontFamily: 'var(--font-mono)' }}>
              Pas assez de données
            </p>
          )}
        </div>


      </CardContent>
    </Card>
  )
}

const KpiPage = () => {
  return (
    <div>
      <Breadcrumb items={[{ label: 'KPIs' }]} className="mb-6" />
      <header className="mb-8">
        <p className="text-[12px] text-[#00b07d] tracking-[0.2em] uppercase mb-2" style={{ fontFamily: 'var(--font-mono)' }}>
          Indicateurs clés
        </p>
        <h1 className="text-4xl font-bold tracking-wider text-[#0d0f14] uppercase" style={{ fontFamily: 'var(--font-display)' }}>
          KPIs
        </h1>
        <div className="mt-3 h-1 w-20 bg-[#00e5a0]" />
      </header>

      <Tabs defaultValue="tendance">
        <TabsList>
          <TabsTrigger value="tendance">Tendance</TabsTrigger>
          <TabsTrigger value="moyenne">Moyenne</TabsTrigger>
        </TabsList>

        <TabsContent value="tendance">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
            <TrendModule
              title="Tendance par capteur"
              entity="sensor"
            />
            <TrendModule
              title="Tendance par zone"
              entity="zone"
            />
          </div>
        </TabsContent>

        <TabsContent value="moyenne">
          <MoyenneModule />
        </TabsContent>
      </Tabs>
    </div>
  )
}

export default KpiPage