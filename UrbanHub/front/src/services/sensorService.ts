import axios from 'axios'
import type { Sensor } from '../types'

const API_URL = import.meta.env.VITE_API_URL || ''

export const fetchSensors = async (): Promise<Sensor[]> => {
  const { data } = await axios.get<Sensor[]>(`${API_URL}/api/sensors`)
  return data
}

export const fetchSensorsByType = async (type: string): Promise<Sensor[]> => {
  const { data } = await axios.get<Sensor[]>(`${API_URL}/api/sensors`, {
    params: { type },
  })
  return data
}

export const fetchSensorCount = async (): Promise<number> => {
  const { data } = await axios.get<number>(`${API_URL}/api/sensors/count`)
  return data
}

export const fetchSensorStatusCount = async (alive: boolean): Promise<number> => {
  const { data } = await axios.get<number>(`${API_URL}/api/sensors/status/count`, {
    params: { alive },
  })
  return data
}
