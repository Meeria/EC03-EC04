import { useMemo } from 'react'
import { LineChart, CartesianGrid, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import type { Measure } from '../types'

interface DataGraphProps {
  data: Measure[]
  height?: number
  /** Window size for moving average smoothing. 1 = no smoothing. Default 5. */
  smooth?: number
}

const formatTimestamp = (timestamp: string) => {
  const date = new Date(timestamp)
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** Simple moving average to smooth noisy sensor data */
function smoothData(data: Measure[], window: number): Measure[] {
  if (window <= 1 || data.length < window) return data
  return data.map((point, i) => {
    const half = Math.floor(window / 2)
    const start = Math.max(0, i - half)
    const end = Math.min(data.length, i + half + 1)
    const slice = data.slice(start, end)
    const avg = slice.reduce((sum, p) => sum + p.value, 0) / slice.length
    return { ...point, value: avg }
  })
}

const DataGraph = ({ data, height = 300, smooth = 5 }: DataGraphProps) => {
  const smoothed = useMemo(() => smoothData(data, smooth), [data, smooth])

  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={smoothed}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
        <XAxis
          dataKey="timestamp"
          tickFormatter={(value) => formatTimestamp(value)}
          tick={{ fontSize: 10, fill: '#94a3b8', fontFamily: 'var(--font-mono)' }}
          tickLine={false}
          axisLine={{ stroke: '#e2e8f0' }}
          interval="preserveStartEnd"
        />
        <YAxis
          tick={{ fontSize: 10, fill: '#94a3b8', fontFamily: 'var(--font-mono)' }}
          tickLine={false}
          axisLine={{ stroke: '#e2e8f0' }}
        />
        <Tooltip
          labelFormatter={(value) => formatTimestamp(value as string)}
          contentStyle={{
            backgroundColor: '#fff',
            border: '1px solid #e2e8f0',
            borderRadius: '8px',
            fontSize: 12,
            fontFamily: 'var(--font-mono)',
          }}
          labelStyle={{ color: '#1e293b', fontWeight: 600 }}
          itemStyle={{ color: '#00b07d' }}
          formatter={(v) => [`${(v as number).toFixed(2)}`, 'Valeur']}
        />
        <Line
          dataKey="value"
          stroke="#00e5a0"
          dot={false}
          strokeWidth={2}
          type="monotone"
        />
      </LineChart>
    </ResponsiveContainer>
  )
}

export default DataGraph
