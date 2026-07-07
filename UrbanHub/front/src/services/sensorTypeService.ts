import axios from 'axios'

export type SensorTypeDto = {
  uuid: string
  sensorTypeId: string
}

const API_URL = import.meta.env.VITE_API_URL || ''

export const fetchSensorTypes = async (): Promise<SensorTypeDto[]> => {
  const { data } = await axios.get<SensorTypeDto[]>(`${API_URL}/api/sensor-types`)
  return data
}

export const fetchSensorTypeCount = async (): Promise<number> => {
  const { data } = await axios.get<number>(`${API_URL}/api/sensor-types/count`)
  return data
}