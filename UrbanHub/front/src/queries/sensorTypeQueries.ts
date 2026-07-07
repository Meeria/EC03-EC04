import { useQuery } from '@tanstack/react-query'
import { fetchSensorTypes, fetchSensorTypeCount } from '../services/sensorTypeService'

export const useSensorTypes = () => {
  return useQuery({
    queryKey: ['sensor-types'],
    queryFn: fetchSensorTypes,
  })
}

export const useSensorTypeCount = () => {
  return useQuery({
    queryKey: ['sensor-types', 'count'],
    queryFn: fetchSensorTypeCount,
  })
}
