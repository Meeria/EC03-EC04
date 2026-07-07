import { useQuery } from '@tanstack/react-query'
import {
  fetchKpiByZone,
  fetchKpiBySensor,
  fetchKpiByType,
} from '../services/kpiService'
import type { KpiByZoneDto, KpiDto } from '../services/kpiService'

export const useKpiByZone = (
  zoneId: string,
  start: string,
  end: string,
  granularity: string
) => {
  return useQuery<KpiByZoneDto>({
    queryKey: ['kpi', 'zone', zoneId, start, end, granularity],
    queryFn: () => fetchKpiByZone(zoneId, start, end, granularity),
    enabled: !!zoneId,
  })
}

export const useKpiBySensor = (
  sensorId: string,
  start: string,
  end: string,
  granularity: string
) => {
  return useQuery<KpiDto[]>({
    queryKey: ['kpi', 'sensor', sensorId, start, end, granularity],
    queryFn: () => fetchKpiBySensor(sensorId, start, end, granularity),
    enabled: !!sensorId,
  })
}

export const useKpiByType = (
  measureType: string,
  start: string,
  end: string,
  granularity: string
) => {
  return useQuery<KpiDto[]>({
    queryKey: ['kpi', 'type', measureType, start, end, granularity],
    queryFn: () => fetchKpiByType(measureType, start, end, granularity),
    enabled: !!measureType,
  })
}
