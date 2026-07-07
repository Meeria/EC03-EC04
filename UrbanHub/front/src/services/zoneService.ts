import axios from 'axios'
import type { Zone, CreateZonePayload, UpdateZonePayload } from '../types'

export const fetchZones = async (): Promise<Zone[]> => {
  const { data } = await axios.get<Zone[]>('/api/zones')
  return data
}

export const fetchZonesCount = async (): Promise<number> => {
  const { data } = await axios.get<number>('/api/zones/count')
  return data
}

export const fetchZoneById = async (zoneId: string): Promise<Zone> => {
  const { data } = await axios.get<Zone>('/api/zones/by-id', { params: { zone_id: zoneId } })
  return data
}

export const createZone = async (payload: CreateZonePayload): Promise<Zone> => {
  const { data } = await axios.post<Zone>('/api/zones', payload)
  return data
}

export const updateZone = async (zoneId: string, payload: UpdateZonePayload): Promise<Zone> => {
  const { data } = await axios.put<Zone>(`/api/zones/${encodeURIComponent(zoneId)}`, payload)
  return data
}

export const deleteZone = async (zoneId: string): Promise<void> => {
  await axios.delete(`/api/zones/${encodeURIComponent(zoneId)}`)
}
