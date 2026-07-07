import axios from 'axios'

export type TrendDto = {
  sensorId: string
  zoneId: string
  timestamp: string
  value: number
  previousValue: number
  changeAbsolute: number
  changePercent: number
  comparedTo: string
}

const API_URL = import.meta.env.VITE_API_URL || ''

export const fetchSensorTrendLatest = async (sensorId: string): Promise<TrendDto | null> => {
  const { data } = await axios.get<TrendDto>(`${API_URL}/api/trends/sensor/latest-vs-previous`, {
    params: { sensor_id: sensorId },
  })
  return data
}

export const fetchSensorTrend24h = async (sensorId: string): Promise<TrendDto | null> => {
  const { data } = await axios.get<TrendDto>(`${API_URL}/api/trends/sensor/latest-vs-24h`, {
    params: { sensor_id: sensorId },
  })
  return data
}

export const fetchSensorTrendPeriod = async (
  sensorId: string,
  start: string,
  end: string
): Promise<TrendDto | null> => {
  const { data } = await axios.get<TrendDto>(`${API_URL}/api/trends/sensor/period`, {
    params: { sensor_id: sensorId, start, end },
  })
  return data
}

export const fetchZoneTrendPeriod = async (
  zoneId: string,
  start: string,
  end: string
): Promise<TrendDto[]> => {
  const { data } = await axios.get<TrendDto[]>(`${API_URL}/api/trends/zone/period`, {
    params: { zone_id: zoneId, start, end },
  })
  return data
}

export const fetchTrendsPeriod = async (
  start: string,
  end: string
): Promise<TrendDto[]> => {
  const { data } = await axios.get<TrendDto[]>(`${API_URL}/api/trends/period`, {
    params: { start, end },
  })
  return data
}
