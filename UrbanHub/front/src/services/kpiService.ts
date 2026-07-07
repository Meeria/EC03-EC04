import axios from 'axios'

const API_URL = import.meta.env.VITE_API_URL || ''

export type KpiDto = {
  bucket: string   // ISO instant
  average: number
  unite: string
}

export type KpiByZoneDto = Record<string, KpiDto[]>  // measureType -> KPIs

export type ZoneKpiRequest = {
  zoneId: string
  start: string  // ISO date string
  end: string    // ISO date string
  bucket: string // TimescaleDB bucket string e.g. "1 hour", "1 day", "1 week"
}

export type SensorKpiRequest = {
  sensorId: string
  start: string
  end: string
  bucket: string
}

export type MeasureTypeKpiRequest = {
  measureType: string
  start: string
  end: string
  bucket: string
}

const toLocalDateTime = (iso: string): string => {
  // Convert ISO date to LocalDateTime string for the Java API
  const d = new Date(iso)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const granularityToBucket = (g: string): string => {
  switch (g) {
    case '1h':    return '1 hour'
    case '24h':   return '1 day'
    case '1week': return '1 week'
    default:      return '1 day'
  }
}

export const fetchKpiByZone = async (
  zoneId: string,
  start: string,
  end: string,
  granularity: string
): Promise<KpiByZoneDto> => {
  const body: ZoneKpiRequest = {
    zoneId,
    start: toLocalDateTime(start),
    end: toLocalDateTime(end),
    bucket: granularityToBucket(granularity),
  }
  const { data } = await axios.get<KpiByZoneDto>(`${API_URL}/analytic/kpi/average/byzone`, {
    params: body
  })
  return data
}

export const fetchKpiBySensor = async (
  sensorId: string,
  start: string,
  end: string,
  granularity: string
): Promise<KpiDto[]> => {
  const body: SensorKpiRequest = {
    sensorId,
    start: toLocalDateTime(start),
    end: toLocalDateTime(end),
    bucket: granularityToBucket(granularity),
  }
  const { data } = await axios.get<KpiDto[]>(`${API_URL}/analytic/kpi/average/bysensor`, {
    params: body
  })
  return data
}

export const fetchKpiByType = async (
  measureType: string,
  start: string,
  end: string,
  granularity: string
): Promise<KpiDto[]> => {
  const body: MeasureTypeKpiRequest = {
    measureType,
    start: toLocalDateTime(start),
    end: toLocalDateTime(end),
    bucket: granularityToBucket(granularity),
  }
  const { data } = await axios.get<KpiDto[]>(`${API_URL}/analytic/kpi/average/bytype`, {
    params: body
  })
  return data
}
