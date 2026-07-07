import React, { useMemo, useState, useEffect } from 'react'
import { MapContainer, TileLayer, Marker, Popup, useMap, Circle, Polygon, Tooltip } from 'react-leaflet'
import L from 'leaflet'
import { useSearchParams } from 'react-router-dom'
// Import Leaflet CSS directly from JS so Vite resolves it from node_modules
import 'leaflet/dist/leaflet.css'
import { useMeasures } from '../queries/measureQueries'
import { useZones } from '../queries/zoneQueries'
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { Breadcrumb } from '@/components/ui/breadcrumb'

// ─── Icon SVG paths by sensor type keyword ───────────────────────────────────
const TYPE_ICON: Record<string, string> = {
  temp: `<path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/>`,
  hum: `<path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/>`,
  co2: `<circle cx="12" cy="12" r="10"/><path d="M8 12a4 4 0 0 1 8 0"/><line x1="12" y1="8" x2="12" y2="12"/>`,
  noise: `<path d="M11 5L6 9H2v6h4l5 4V5z"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/>`,
  pm: `<circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="4"/><line x1="4.93" y1="4.93" x2="7.76" y2="7.76"/><line x1="16.24" y1="16.24" x2="19.07" y2="19.07"/>`,
  light: `<circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>`,
  default: `<circle cx="12" cy="12" r="4"/><path d="M6.3 6.3a8 8 0 0 0 0 11.4M17.7 6.3a8 8 0 0 1 0 11.4"/>`,
}

const getTypeIcon = (typeId: string): string => {
  const key = typeId.toLowerCase()
  for (const k of Object.keys(TYPE_ICON)) {
    if (k !== 'default' && key.includes(k)) return TYPE_ICON[k]
  }
  return TYPE_ICON.default
}

const makeMarkerIcon = (typeId: string, active: boolean) => {
  const iconPaths = getTypeIcon(typeId)
  const dot = active ? '#00e5a0' : '#94a3b8'
  const border = active ? '#00b07d' : '#64748b'
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="36" height="44" viewBox="0 0 36 44">
      <filter id="sh" x="-40%" y="-20%" width="180%" height="180%">
        <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#0d0f14" flood-opacity="0.18"/>
      </filter>
      <path d="M18 2C10.27 2 4 8.27 4 16c0 10.5 14 26 14 26S32 26.5 32 16C32 8.27 25.73 2 18 2z"
        fill="white" stroke="${border}" stroke-width="1.5" filter="url(#sh)"/>
      <g transform="translate(6,5)" fill="none" stroke="${dot}" stroke-width="1.75"
         stroke-linecap="round" stroke-linejoin="round">
        ${iconPaths}
      </g>
      <circle cx="28" cy="8" r="5" fill="white" stroke="${border}" stroke-width="1"/>
      <circle cx="28" cy="8" r="3" fill="${dot}"/>
    </svg>`

  return L.divIcon({
    html: svg,
    className: '',
    iconSize: [36, 44],
    iconAnchor: [18, 44],
    popupAnchor: [0, -44],
  })
}

// ─── Auto-fit map bounds to NW/SE corners ─────────────────────────────────────
function FitBounds({ coords, focusCoord }: { coords: [number, number][]; focusCoord?: [number, number] }) {
  const map = useMap()
  useEffect(() => {
    if (focusCoord) {
      map.setView(focusCoord, 16)
    } else if (coords.length === 0) {
      return
    } else if (coords.length === 1) {
      map.setView(coords[0], 15)
    } else {
      // Find NW (max lat, min lon) and SE (min lat, max lon) corners
      const lats = coords.map(c => c[0])
      const lons = coords.map(c => c[1])
      const northWest: [number, number] = [Math.max(...lats), Math.min(...lons)]
      const southEast: [number, number] = [Math.min(...lats), Math.max(...lons)]
      map.fitBounds(L.latLngBounds([northWest, southEast]), { padding: [50, 50] })
    }
  }, [map, coords, focusCoord])
  return null
}

// ─── Zone shape helpers ─────────────────────────────────────────────────────
const ZONE_COLOR = '#ef4444'

function getZoneCenter(coords: [number, number][]): [number, number] {
  const lats = coords.map(c => c[0])
  const lons = coords.map(c => c[1])
  return [(Math.max(...lats) + Math.min(...lats)) / 2, (Math.max(...lons) + Math.min(...lons)) / 2]
}

function getZoneRadius(coords: [number, number][]): number {
  const [clat, clon] = getZoneCenter(coords)
  return Math.max(...coords.map(c => Math.sqrt((c[0] - clat) ** 2 + (c[1] - clon) ** 2))) * 111000 * 1.3 + 100
}

// Returns polygon points forming a simple convex hull ring around coords
function getHullPoints(coords: [number, number][]): [number, number][] {
  if (coords.length === 0) return []
  if (coords.length === 1) return coords
  if (coords.length === 2) return coords
  const [clat, clon] = getZoneCenter(coords)
  const r = getZoneRadius(coords) / 111000
  const sides = Math.max(6, coords.length)
  return Array.from({ length: sides }, (_, i) => {
    const angle = (2 * Math.PI * i) / sides
    return [clat + r * Math.cos(angle), clon + r * Math.sin(angle)] as [number, number]
  })
}

const formatDateTime = (ts: string): string => {
  const d = new Date(ts)
  if (isNaN(d.getTime())) return ts
  return d.toLocaleString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}


// ─── Page ─────────────────────────────────────────────────────────────────────
const MapPage = () => {
  const [searchParams] = useSearchParams()
  const { data } = useMeasures()
  const { data: zones } = useZones()
  const [typeFilter, setTypeFilter] = useState('all')
  const [zoneFilter, setZoneFilter] = useState('none')

  // Single-sensor focus from "Voir sur la carte" button
  const focusCoord: [number, number] | undefined = (() => {
    const lat = searchParams.get('lat')
    const lng = searchParams.get('lng')
    if (lat && lng) {
      const latNum = parseFloat(lat)
      const lngNum = parseFloat(lng)
      if (!isNaN(latNum) && !isNaN(lngNum)) return [latNum, lngNum]
    }
    return undefined
  })()

  const sensors = useMemo(() => {
    if (!data) return []
    const map = new Map<string, {
      sensorId: string
      lat: number
      lng: number
      typeId: string
      status: boolean
      lastValue: number
      unit: string
      lastTimestamp: string
      zoneId?: string
    }>()

    // Sort ascending → last write is most recent
    const sorted = [...data].sort(
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    )
    sorted.forEach((m) => {
      map.set(m.sensorId, {
        sensorId: m.sensorId,
        lat: m.latitude,
        lng: m.longitude,
        typeId: m.sensorTypeId,
        status: m.sensorStatus,
        lastValue: m.value,
        unit: m.unit,
        lastTimestamp: m.timestamp,
      })
    })
    // Enrich with zone info
    if (zones) {
      const zoneSensorMap = new Map<string, string>()
      zones.forEach(z => z.sensors?.forEach(s => zoneSensorMap.set(s.sensorId, z.zoneId)))
      sorted.forEach(m => {
        if (map.has(m.sensorId)) {
          const s = map.get(m.sensorId)!
          s.zoneId = zoneSensorMap.get(m.sensorId) ?? undefined
        }
      })
    }

    return Array.from(map.values())
  }, [data, zones])

  const uniqueTypes = useMemo(
    () => Array.from(new Set(sensors.map((s) => s.typeId))),
    [sensors]
  )

  const uniqueZones = useMemo(
    () => Array.from(new Set(sensors.filter(s => s.zoneId).map(s => s.zoneId!))),
    [sensors]
  )

  const filtered = useMemo(() => {
    let result = typeFilter === 'all' ? sensors : sensors.filter(s => s.typeId === typeFilter)
    if (zoneFilter === 'none') result = result.filter(s => !s.zoneId)
    else if (zoneFilter !== 'all') result = result.filter(s => s.zoneId === zoneFilter)
    return result
  }, [sensors, typeFilter, zoneFilter])

  const coords: [number, number][] = filtered.map((s) => [s.lat, s.lng])

  // Default center: midpoint between NW-most (max lat, min lon) and SE-most (min lat, max lon)
  const defaultCenter: [number, number] = useMemo(() => {
    if (filtered.length === 0) return [49.185, -0.360] // Caen default
    const northMost = filtered.reduce((a, b) => a.lat > b.lat ? a : b)
    const westMost = filtered.reduce((a, b) => a.lng < b.lng ? a : b)
    const southMost = filtered.reduce((a, b) => a.lat < b.lat ? a : b)
    const eastMost = filtered.reduce((a, b) => a.lng > b.lng ? a : b)
    const centerLat = (northMost.lat + southMost.lat) / 2
    const centerLng = (westMost.lng + eastMost.lng) / 2
    return [centerLat, centerLng]
  }, [filtered])

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: 'calc(100vh - 64px)' }}>
      <Breadcrumb items={[{ label: 'Carte' }]} className="mb-6" />
      <header style={{ marginBottom: 24 }}>
        <p className="text-[12px] text-[#00b07d] tracking-[0.2em] uppercase mb-2" style={{ fontFamily: 'var(--font-mono)' }}>
          Visualisation spatiale
        </p>
        <h1 className="text-5xl font-bold tracking-wider text-[#0d0f14] uppercase" style={{ fontFamily: 'var(--font-display)' }}>
          Carte
        </h1>
        <div className="mt-4 h-1 w-20 bg-[#00e5a0]" />
      </header>

      {/* Toolbar — must be above the map (higher stacking context) */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16, position: 'relative', zIndex: 1000 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <label className="text-[11px] text-[#94a3b8] tracking-[0.15em] uppercase" style={{ fontFamily: 'var(--font-mono)' }}>
            Type
          </label>
          <Select value={typeFilter} onValueChange={setTypeFilter}>
            <SelectTrigger className="w-[200px]">
              <SelectValue placeholder="Tous les types" />
            </SelectTrigger>
            <SelectContent style={{ zIndex: 2000 }}>
              <SelectItem value="all">Tous les types</SelectItem>
              {uniqueTypes.map((t) => (
                <SelectItem key={t} value={t}>{t}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {uniqueZones.length > 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <label className="text-[11px] text-[#94a3b8] tracking-[0.15em] uppercase" style={{ fontFamily: 'var(--font-mono)' }}>
              Zone
            </label>
            <Select value={zoneFilter} onValueChange={setZoneFilter}>
              <SelectTrigger className="w-[200px]">
                <SelectValue placeholder="Aucune" />
              </SelectTrigger>
              <SelectContent style={{ zIndex: 2000 }}>
                <SelectItem value="none">Aucune</SelectItem>
                {uniqueZones.map((z) => (
                  <SelectItem key={z} value={z}>{z}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        )}

        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 20 }}>
          <Badge className="bg-[#00e5a0]/10 text-[#00b07d] border border-[#00e5a0]/30" style={{ fontFamily: 'var(--font-mono)' }}>
            {filtered.length} capteur{filtered.length > 1 ? 's' : ''}
          </Badge>
        </div>
      </div>

      {/* Map wrapper */}
      <div style={{
        flex: 1,
        minHeight: 520,
        borderRadius: 16,
        overflow: 'hidden',
        border: '1px solid #e2e8f0',
        boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
        position: 'relative',
        zIndex: 1,
      }}>
        <MapContainer
          center={defaultCenter}
          zoom={13}
          style={{ width: '100%', height: '100%', minHeight: 520, zIndex: 1 }}
          zoomControl
          attributionControl={false}
        >
          {/* OpenStreetMap — reliable, always has a background */}
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            subdomains="abc"
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          />

          <FitBounds coords={coords} focusCoord={focusCoord} />

          {/* Zone overlays */}
          {zones && zones.map(zone => {
            const zoneCoords: [number, number][] = filtered
              .filter(s => s.zoneId === zone.zoneId)
              .map(s => [s.lat, s.lng] as [number, number])
            if (zoneCoords.length === 0) return null

            const count = zoneCoords.length
            const [clat, clon] = getZoneCenter(zoneCoords)
            const radius = getZoneRadius(zoneCoords)

            const fillOpacity = count >= 4 ? 0.18 : count >= 2 ? 0.14 : 0.20
            const commonStyle = {
              color: ZONE_COLOR,
              fillColor: ZONE_COLOR,
              fillOpacity,
              weight: count >= 4 ? 2.5 : count >= 2 ? 1.5 : 1,
            }

            return (
              <React.Fragment key={zone.zoneId}>
                {count === 1 ? (
                  <Circle
                    center={[clat, clon]}
                    radius={radius}
                    pathOptions={{ ...commonStyle }}
                  >
                    <Tooltip direction="top" offset={[0, -radius - 5]} opacity={1} sticky>
                      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: ZONE_COLOR, fontWeight: 700, background: 'white', padding: '2px 6px', borderRadius: 4 }}>
                        {zone.zoneId}
                      </span>
                    </Tooltip>
                  </Circle>
                ) : count === 2 ? (
                  <Circle
                    center={[clat, clon]}
                    radius={radius}
                    pathOptions={{ ...commonStyle }}
                  >
                    <Tooltip direction="top" offset={[0, -10]} opacity={1} sticky>
                      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: ZONE_COLOR, fontWeight: 700 }}>
                        {zone.zoneId}
                      </span>
                    </Tooltip>
                  </Circle>
                ) : count === 3 ? (
                  <Circle
                    center={[clat, clon]}
                    radius={radius}
                    pathOptions={{ ...commonStyle, dashArray: '6,4' }}
                  >
                    <Tooltip direction="top" offset={[0, -10]} opacity={1} sticky>
                      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: ZONE_COLOR, fontWeight: 700 }}>
                        {zone.zoneId}
                      </span>
                    </Tooltip>
                  </Circle>
                ) : (
                  <Polygon
                    positions={getHullPoints(zoneCoords)}
                    pathOptions={{ ...commonStyle }}
                  >
                    <Tooltip direction="top" offset={[0, -10]} opacity={1} sticky>
                      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: ZONE_COLOR, fontWeight: 700 }}>
                        {zone.zoneId} ({count})
                      </span>
                    </Tooltip>
                  </Polygon>
                )}
              </React.Fragment>
            )
          })}

          {filtered.map((sensor) => (
            <Marker
              key={sensor.sensorId}
              position={[sensor.lat, sensor.lng]}
              icon={makeMarkerIcon(sensor.typeId, sensor.status)}
            >
              <Popup closeButton={false} offset={[0, -8]}>
                <div style={{ fontFamily: 'var(--font-mono)', minWidth: 172, padding: '2px 0' }}>

                  {/* Type + ID */}
                  <p style={{ fontSize: 10, letterSpacing: '0.18em', textTransform: 'uppercase', color: '#94a3b8', marginBottom: 2 }}>
                    {sensor.typeId}
                  </p>
                  <p style={{ fontSize: 15, fontWeight: 700, color: '#0d0f14', fontFamily: 'var(--font-display)', letterSpacing: '0.1em', textTransform: 'uppercase', marginBottom: 10 }}>
                    {sensor.sensorId}
                  </p>

                  {/* Last value */}
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: 5, marginBottom: 10 }}>
                    <span style={{ fontSize: 28, fontWeight: 700, color: '#0d0f14', fontFamily: 'var(--font-display)', lineHeight: 1 }}>
                      {sensor.lastValue}
                    </span>
                    <span style={{ fontSize: 13, color: '#64748b' }}>{sensor.unit}</span>
                  </div>

                  {/* Last timestamp */}
                  <p style={{ fontSize: 10, color: '#94a3b8', letterSpacing: '0.08em', marginBottom: 10 }}>
                    {formatDateTime(sensor.lastTimestamp)}
                  </p>

                  {/* Divider */}
                  <div style={{ height: 1, background: '#f1f5f9', marginBottom: 8 }} />

                  {/* Status */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <span style={{ width: 7, height: 7, borderRadius: '50%', background: sensor.status ? '#00e5a0' : '#94a3b8', display: 'inline-block' }} />
                    <span style={{ fontSize: 10, color: sensor.status ? '#00b07d' : '#94a3b8', letterSpacing: '0.12em', textTransform: 'uppercase' }}>
                      {sensor.status ? 'En ligne' : 'Hors ligne'}
                    </span>
                  </div>
                </div>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>
    </div>
  )
}

export default MapPage
