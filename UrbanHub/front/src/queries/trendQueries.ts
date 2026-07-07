import { useQuery } from '@tanstack/react-query'
import {
  fetchSensorTrendLatest,
  fetchSensorTrend24h,
  fetchSensorTrendPeriod,
  fetchZoneTrendPeriod,
  fetchTrendsPeriod,
} from '@/services/trendService'

export const useSensorTrendLatest = (sensorId: string) =>
  useQuery({
    queryKey: ['trends', 'sensor', 'latest', sensorId],
    queryFn: () => fetchSensorTrendLatest(sensorId),
    enabled: !!sensorId,
  })

export const useSensorTrend24h = (sensorId: string) =>
  useQuery({
    queryKey: ['trends', 'sensor', '24h', sensorId],
    queryFn: () => fetchSensorTrend24h(sensorId),
    enabled: !!sensorId,
  })

export const useSensorTrendPeriod = (sensorId: string, start: string, end: string) =>
  useQuery({
    queryKey: ['trends', 'sensor', 'period', sensorId, start, end],
    queryFn: () => fetchSensorTrendPeriod(sensorId, start, end),
    enabled: !!sensorId && !!start && !!end,
  })

export const useZoneTrendPeriod = (zoneId: string, start: string, end: string) =>
  useQuery({
    queryKey: ['trends', 'zone', zoneId, start, end],
    queryFn: () => fetchZoneTrendPeriod(zoneId, start, end),
    enabled: !!zoneId && !!start && !!end,
    staleTime: 0,
  })

export const useTrendsPeriod = (start: string, end: string) =>
  useQuery({
    queryKey: ['trends', 'period', start, end],
    queryFn: () => fetchTrendsPeriod(start, end),
    enabled: !!start && !!end,
  })
