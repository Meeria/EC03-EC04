import axios from 'axios'
import type { Measure } from '../types'

const API_URL = import.meta.env.VITE_API_URL || ''

export const fetchMeasures = async (): Promise<Measure[]> => {
  const { data } = await axios.get<Measure[]>(`${API_URL}/api/measures`)
  return data
}

export const fetchMeasuresBySensorId = async (sensorId: string): Promise<Measure[]> => {
  const { data } = await axios.get<Measure[]>(`${API_URL}/api/measures`, { params: { sensor_id: sensorId } })
  return data
}

export const fetchMeasuresCount = async (): Promise<number> => {
  const { data } = await axios.get<number>(`${API_URL}/api/measures/count`)
  return data
}

export const fetchMeasuresByDay = async (date: string): Promise<Measure[]> => {
  const { data } = await axios.get<Measure[]>(`${API_URL}/api/measures/by-day`, { params: { date } })
  return data
}

export const fetchMeasuresByDateRange = async (from: string, to: string): Promise<Measure[]> => {
  const { data } = await axios.get<Measure[]>(`${API_URL}/api/measures/by-date-range`, { params: { from, to } })
  return data
}