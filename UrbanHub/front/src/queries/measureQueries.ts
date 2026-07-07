import { useQuery } from '@tanstack/react-query'
import {
  fetchMeasures,
  fetchMeasuresBySensorId,
  fetchMeasuresCount,
  fetchMeasuresByDay,
  fetchMeasuresByDateRange,
} from '../services/measureService'

export const useMeasures = () => {
  return useQuery({
    queryKey: ['measures'],
    queryFn: fetchMeasures,
  })
}

export const useMeasuresBySensor = (sensorId: string) => {
  return useQuery({
    queryKey: ['measures', sensorId],
    queryFn: () => fetchMeasuresBySensorId(sensorId),
  })
}

export const useMeasuresCount = () => {
  return useQuery({
    queryKey: ['measures', 'count'],
    queryFn: fetchMeasuresCount,
  })
}

export const useMeasuresByDay = (date: string) => {
  return useQuery({
    queryKey: ['measures', 'by-day', date],
    queryFn: () => fetchMeasuresByDay(date),
    enabled: !!date,
  })
}

export const useMeasuresByDateRange = (from: string, to: string) => {
  return useQuery({
    queryKey: ['measures', 'by-date-range', from, to],
    queryFn: () => fetchMeasuresByDateRange(from, to),
    enabled: !!from && !!to,
  })
}
