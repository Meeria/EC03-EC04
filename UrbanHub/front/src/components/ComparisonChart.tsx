import { useMemo } from 'react'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts'
import type { Measure } from '../types'

interface ComparisonChartProps {
  data: Measure[]
  height?: number
  unit?: string
}

const COLORS = [
  '#00e5a0', // vert
  '#3b82f6', // bleu
  '#f59e0b', // amber
  '#ef4444', // rouge
  '#a855f7', // violet
  '#06b6d4', // cyan
  '#f97316', // orange
  '#ec4899', // rose
  '#84cc16', // lime
  '#6366f1', // indigo
]

const formatTimestamp = (ts: string | number) => {
  if (!ts) return ''
  const date = new Date(ts)
  if (isNaN(date.getTime())) return ''
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const ComparisonChart = ({ data, height = 400, unit }: ComparisonChartProps) => {
  const sensorIds = useMemo(
    () => Array.from(new Set(data.map((m) => m.sensorId))),
    [data]
  )

  /**
   * Build a unified timeline:
   * - Collect every unique timestamp across all sensors (sorted)
   * - For each point in time, assign each sensor's value if it has one at that exact ts,
   *   otherwise leave it undefined so Recharts skips it (connectNulls joins the gaps).
   *
   * This means two sensors with completely disjoint timestamps will still render
   * correctly as two independent lines sharing the same X axis (time).
   */
  const chartData = useMemo(() => {
    // Build per-sensor sorted arrays
    const bySensor = new Map<string, Measure[]>()
    sensorIds.forEach((id) => {
      bySensor.set(
        id,
        data
          .filter((m) => m.sensorId === id)
          .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
      )
    })

    // Collect all unique timestamps
    const allTs = Array.from(new Set(data.map((m) => m.timestamp))).sort(
      (a, b) => new Date(a).getTime() - new Date(b).getTime()
    )

    // Build value lookup: sensorId → timestamp → value
    const lookup = new Map<string, Map<string, number>>()
    sensorIds.forEach((id) => {
      const inner = new Map<string, number>()
      ;(bySensor.get(id) ?? []).forEach((m) => inner.set(m.timestamp, m.value))
      lookup.set(id, inner)
    })

    // Assemble chart rows
    return allTs.map((ts) => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const row: Record<string, any> = { timestamp: ts }
      sensorIds.forEach((id) => {
        const val = lookup.get(id)?.get(ts)
        // Only set the value if this sensor actually has a measurement at this ts
        if (val !== undefined) row[id] = val
      })
      return row
    })
  }, [data, sensorIds])

  if (chartData.length === 0) {
    return (
      <p className="text-sm text-[#94a3b8] tracking-wider text-center py-12" style={{ fontFamily: 'var(--font-mono)' }}>
        Aucune donnée à afficher.
      </p>
    )
  }

  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={chartData} margin={{ top: 10, right: 20, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
        <XAxis
          dataKey="timestamp"
          tickFormatter={formatTimestamp}
          tick={{ fontSize: 10, fill: '#94a3b8', fontFamily: 'var(--font-mono)' }}
          tickLine={false}
          axisLine={{ stroke: '#e2e8f0' }}
          interval="preserveStartEnd"
          minTickGap={60}
        />
        <YAxis
          tick={{ fontSize: 10, fill: '#94a3b8', fontFamily: 'var(--font-mono)' }}
          tickLine={false}
          axisLine={{ stroke: '#e2e8f0' }}
          width={60}
          label={
            unit
              ? {
                  value: unit,
                  angle: -90,
                  position: 'insideLeft',
                  style: { fontSize: 10, fill: '#94a3b8', fontFamily: 'var(--font-mono)' },
                }
              : undefined
          }
        />
        <Tooltip
          labelFormatter={(value) => formatTimestamp(value as string)}
          contentStyle={{
            backgroundColor: '#fff',
            border: '1px solid #e2e8f0',
            borderRadius: '12px',
            fontSize: 12,
            fontFamily: 'var(--font-mono)',
            boxShadow: '0 4px 24px rgba(0,0,0,0.07)',
          }}
          labelStyle={{ color: '#1e293b', fontWeight: 600, marginBottom: 4 }}
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          formatter={(value: any, name: any) => [
            `${value}${unit ? ' ' + unit : ''}`,
            name,
          ]}
        />
        <Legend
          wrapperStyle={{
            fontFamily: 'var(--font-mono)',
            fontSize: 12,
            color: '#64748b',
            paddingTop: 16,
          }}
        />
        {sensorIds.map((sensorId, index) => (
          <Line
            key={sensorId}
            type="monotone"
            dataKey={sensorId}
            stroke={COLORS[index % COLORS.length]}
            strokeWidth={2}
            dot={false}
            connectNulls
            isAnimationActive={false}
            activeDot={{ r: 4, strokeWidth: 0 }}
          />
        ))}
      </LineChart>
    </ResponsiveContainer>
  )
}

export default ComparisonChart
