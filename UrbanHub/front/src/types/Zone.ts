export type Zone = {
  uuid?: string
  zoneId: string
  sensors?: { sensorId: string; sensorTypeId?: string; status?: boolean }[]
}

export type CreateZonePayload = {
  zoneId: string
  sensorIds: string[]
}

export type UpdateZonePayload = {
  zoneId?: string
  sensorIds?: string[]
}
