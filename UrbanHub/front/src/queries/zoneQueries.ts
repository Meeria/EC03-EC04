import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { fetchZones, fetchZonesCount, fetchZoneById, createZone, updateZone, deleteZone } from '../services/zoneService'
import type { CreateZonePayload, UpdateZonePayload } from '../types'

export const useZones = () => {
  return useQuery({
    queryKey: ['zones'],
    queryFn: fetchZones,
    retry: false,
  })
}

export const useZonesCount = () => {
  return useQuery({
    queryKey: ['zones', 'count'],
    queryFn: fetchZonesCount,
  })
}

export const useZoneById = (zoneId: string) => {
  return useQuery({
    queryKey: ['zones', zoneId],
    queryFn: () => fetchZoneById(zoneId),
    enabled: !!zoneId,
  })
}

export const useCreateZone = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CreateZonePayload) => createZone(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['zones'] })
    },
  })
}

export const useUpdateZone = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ zoneId, payload }: { zoneId: string; payload: UpdateZonePayload }) =>
      updateZone(zoneId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['zones'] })
    },
  })
}

export const useDeleteZone = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (zoneId: string) => deleteZone(zoneId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['zones'] })
    },
  })
}
