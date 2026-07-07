import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, MapPin, Cpu, AlertTriangle, Map as MapIcon, ChevronRight, ChevronDown, Square, Pencil, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { MapContainer, TileLayer, Marker } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import { useZones, useCreateZone, useUpdateZone, useDeleteZone } from '../queries/zoneQueries'
import { useMeasures } from '../queries/measureQueries'
import { Card, CardContent } from '@/components/ui/card'
import { Breadcrumb } from '@/components/ui/breadcrumb'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog'

// ─── Sensor with coords (for area calc & map) ────────────────────────────
type SensorWithCoords = { sensorId: string; sensorTypeId: string; lat: number; lng: number }

// ─── Area calculation via convex hull (Shoelace formula) ──────────────────
function calcConvexHullArea(coords: [number, number][]): number {
  if (coords.length < 3) return 0
  // Convert to km using cos(lat) for longitude compression
  const lat0 = coords[0][0]
  const kmPerDegLat = 111.32
  const kmPerDegLon = 111.32 * Math.cos((lat0 * Math.PI) / 180)
  const kmCoords = coords.map(c => [c[0] * kmPerDegLat, c[1] * kmPerDegLon])
  let area = 0
  for (let i = 0; i < kmCoords.length; i++) {
    const j = (i + 1) % kmCoords.length
    area += kmCoords[i][0] * kmCoords[j][1]
    area -= kmCoords[j][0] * kmCoords[i][1]
  }
  return Math.abs(area / 2)
}

// ─── Auto-fit map bounds ────────────────────────────────────────────────
function FitBounds({ coords: _coords }: { coords: [number, number][] }) {
  // This is a stub for the map preview - we use a simple center approach instead
  return null
}

// ─── Marker icon helper ─────────────────────────────────────────────────
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

const ZonesPage = () => {
  const navigate = useNavigate()
  const { data: zones, isLoading, isError } = useZones()
  const { data: measures } = useMeasures()
  const createZone = useCreateZone()
  const updateZone = useUpdateZone()
  const deleteZone = useDeleteZone()

  const [open, setOpen] = useState(false)
  const [zoneName, setZoneName] = useState('')
  const [selectedSensors, setSelectedSensors] = useState<string[]>([])
  const [searchSensor, setSearchSensor] = useState('')
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [showMap, setShowMap] = useState(false)
  const [expandedZone, setExpandedZone] = useState<string | null>(null)

  // Edit dialog state
  const [editOpen, setEditOpen] = useState(false)
  const [editingZone, setEditingZone] = useState<string | null>(null)
  const [editZoneName, setEditZoneName] = useState('')
  const [editSelectedSensors, setEditSelectedSensors] = useState<string[]>([])
  const [editSubmitError, setEditSubmitError] = useState<string | null>(null)

  // Delete dialog state
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [deletingZoneId, setDeletingZoneId] = useState<string | null>(null)
  const [deleteSubmitError, setDeleteSubmitError] = useState<string | null>(null)

  // All sensors deduplicated with coords
  const sensorMap = useMemo<Map<string, SensorWithCoords>>(() => {
    if (!measures) return new Map()
    const map = new Map<string, SensorWithCoords>()
    measures.forEach(m => {
      if (!map.has(m.sensorId)) {
        map.set(m.sensorId, { sensorId: m.sensorId, sensorTypeId: m.sensorTypeId, lat: m.latitude, lng: m.longitude })
      }
    })
    return map
  }, [measures])

  const availableSensors = useMemo(() => Array.from(sensorMap.values()), [sensorMap])

  const selectedSensorData = useMemo(() => {
    return availableSensors.filter(s => selectedSensors.includes(s.sensorId))
  }, [availableSensors, selectedSensors])

  const mapCenter = useMemo<[number, number]>(() => {
    if (selectedSensorData.length === 0) return [49.185, -0.360]
    const lats = selectedSensorData.map(s => s.lat)
    const lons = selectedSensorData.map(s => s.lng)
    return [(Math.max(...lats) + Math.min(...lats)) / 2, (Math.max(...lons) + Math.min(...lons)) / 2]
  }, [selectedSensorData])

  const mapCoords: [number, number][] = selectedSensorData.map(s => [s.lat, s.lng])

  // Build sensor list per zone with coords + area
  const zoneData = useMemo(() => {
    if (!zones) return []
    return zones.map(zone => {
      const sensorsWithCoords = (zone.sensors ?? [])
        .map(s => sensorMap.get(s.sensorId))
        .filter((s): s is SensorWithCoords => s !== undefined)
      const coords = sensorsWithCoords.map(s => [s.lat, s.lng] as [number, number])
      const area = sensorsWithCoords.length >= 3 ? calcConvexHullArea(coords) : 0
      return { ...zone, sensorsWithCoords, area }
    })
  }, [zones, sensorMap])

  const filteredSensors = useMemo(() => {
    if (!searchSensor.trim()) return availableSensors
    return availableSensors.filter(
      s => s.sensorId.toLowerCase().includes(searchSensor.toLowerCase()) ||
           s.sensorTypeId.toLowerCase().includes(searchSensor.toLowerCase())
    )
  }, [availableSensors, searchSensor])

  const toggleSensor = (sensorId: string) => {
    setSelectedSensors(prev =>
      prev.includes(sensorId) ? prev.filter(id => id !== sensorId) : [...prev, sensorId]
    )
  }

  const handleOpenChange = (val: boolean) => {
    setOpen(val)
    if (!val) {
      setZoneName(''); setSelectedSensors([]); setSearchSensor(''); setSubmitError(null); setShowMap(false)
    }
  }

  const handleCreate = async () => {
    if (!zoneName.trim()) { setSubmitError('Le nom de la zone est requis.'); return }
    setSubmitError(null)
    try {
      await createZone.mutateAsync({ zoneId: zoneName.trim(), sensorIds: selectedSensors })
      toast.success(`Zone "${zoneName.trim()}" créée avec succès`)
      handleOpenChange(false)
    } catch {
      toast.error("Impossible de créer la zone. Veuillez réessayer.")
      setSubmitError("Impossible de créer la zone. Veuillez réessayer.")
    }
  }

  const handleViewOnMap = (zoneId: string) => {
    navigate(`/carte?zone=${encodeURIComponent(zoneId)}`)
  }

  const handleEditOpen = (zoneId: string, currentName: string, currentSensorIds: string[]) => {
    setEditingZone(zoneId)
    setEditZoneName(currentName)
    setEditSelectedSensors(currentSensorIds)
    setEditSubmitError(null)
    setEditOpen(true)
  }

  const handleEditSave = async () => {
    if (!editZoneName.trim()) { setEditSubmitError('Le nom de la zone est requis.'); return }
    if (!editingZone) return
    setEditSubmitError(null)
    try {
      await updateZone.mutateAsync({
        zoneId: editingZone,
        payload: { zoneId: editZoneName.trim(), sensorIds: editSelectedSensors },
      })
      toast.success(`Zone "${editZoneName.trim()}" mise à jour`)
      setEditOpen(false)
    } catch {
      toast.error("Impossible de mettre à jour la zone. Veuillez réessayer.")
      setEditSubmitError("Impossible de mettre à jour la zone. Veuillez réessayer.")
    }
  }

  const handleEditOpenChange = (val: boolean) => {
    setEditOpen(val)
    if (!val) { setEditingZone(null); setEditZoneName(''); setEditSelectedSensors([]); setEditSubmitError(null) }
  }

  const handleDeleteOpen = (zoneId: string) => {
    setDeletingZoneId(zoneId)
    setDeleteSubmitError(null)
    setDeleteOpen(true)
  }

  const handleDeleteConfirm = async () => {
    if (!deletingZoneId) return
    setDeleteSubmitError(null)
    try {
      await deleteZone.mutateAsync(deletingZoneId)
      toast.success(`Zone supprimée`)
      setDeleteOpen(false)
      setExpandedZone(null)
    } catch {
      toast.error("Impossible de supprimer la zone. Veuillez réessayer.")
      setDeleteSubmitError("Impossible de supprimer la zone. Veuillez réessayer.")
    }
  }

  const formatArea = (km2: number) => {
    if (km2 === 0) return '< 0.1 km²'
    if (km2 < 1) return `${km2.toFixed(2)} km²`
    return `${km2.toFixed(1)} km²`
  }

  return (
    <div>
      <Breadcrumb items={[{ label: 'Zones' }]} className="mb-6" />
      {/* Header */}
      <header className="mb-10 flex items-start justify-between">
        <div>
          <p
            className="text-[12px] text-[#00b07d] tracking-[0.2em] uppercase mb-2"
            style={{ fontFamily: 'var(--font-mono)' }}
          >
            Périmètres urbains
          </p>
          <h1
            className="text-5xl font-bold tracking-wider text-[#0d0f14] uppercase"
            style={{ fontFamily: 'var(--font-display)' }}
          >
            Zones
          </h1>
          <div className="mt-4 h-1 w-20 bg-[#00e5a0]" />
        </div>
        <Button
          onClick={() => setOpen(true)}
          className="mt-2 flex items-center gap-2 bg-[#00e5a0] text-[#0d0f14] hover:bg-[#00e5a0]/90 shadow-md"
        >
          <Plus className="h-4 w-4" />
          Nouvelle zone
        </Button>
      </header>

      {/* Content card */}
      <Card>
        <CardContent className="p-8">
          {isLoading && (
            <p
              className="text-sm text-[#94a3b8] tracking-wider animate-pulse"
              style={{ fontFamily: 'var(--font-mono)' }}
            >
              Chargement des zones...
            </p>
          )}

          {isError && (
            <div className="flex flex-col items-center justify-center py-16 gap-4">
              <div className="flex items-center justify-center w-16 h-16 rounded-2xl bg-amber-50 border border-amber-100">
                <AlertTriangle className="h-8 w-8 text-amber-400" />
              </div>
              <p
                className="text-lg font-semibold tracking-wider text-[#94a3b8] uppercase text-center"
                style={{ fontFamily: 'var(--font-display)' }}
              >
                Pas encore disponible
              </p>
              <p
                className="text-xs text-[#94a3b8] tracking-wider text-center max-w-xs"
                style={{ fontFamily: 'var(--font-mono)' }}
              >
                L'API des zones n'est pas encore accessible. Réessayez plus tard.
              </p>
            </div>
          )}

          {!isLoading && !isError && (!zones || zones.length === 0) && (
            <div className="flex flex-col items-center justify-center py-16 gap-4">
              <div className="flex items-center justify-center w-16 h-16 rounded-2xl bg-[#f8fafc] border border-[#e2e8f0]">
                <MapPin className="h-8 w-8 text-[#cbd5e1]" />
              </div>
              <p
                className="text-lg font-semibold tracking-wider text-[#94a3b8] uppercase text-center"
                style={{ fontFamily: 'var(--font-display)' }}
              >
                Aucune zone définie
              </p>
              <p
                className="text-xs text-[#94a3b8] tracking-wider text-center max-w-xs"
                style={{ fontFamily: 'var(--font-mono)' }}
              >
                Créez votre première zone pour regrouper vos capteurs par périmètre urbain.
              </p>
              <Button
                onClick={() => setOpen(true)}
                variant="outline"
                className="mt-2"
              >
                <Plus className="h-4 w-4" />
                Créer une zone
              </Button>
            </div>
          )}

          {zones && zones.length > 0 && (
            <>
              <p
                className="text-xs text-[#94a3b8] tracking-wider mb-4"
                style={{ fontFamily: 'var(--font-mono)' }}
              >
                {zones.length} zone(s) trouvée(s)
              </p>
              <ul className="flex flex-col gap-3">
                {zoneData.map((zone) => (
                  <li key={zone.uuid ?? zone.zoneId}>
                    {/* Zone row — click to expand */}
                    <div className="flex items-center justify-between px-5 py-4 rounded-lg border border-[#e2e8f0] hover:border-[#00e5a0]/50 hover:bg-[#f8fafc] transition-all duration-150">
                      <button
                        onClick={() => setExpandedZone(expandedZone === zone.zoneId ? null : zone.zoneId)}
                        className="flex items-center gap-3 flex-1"
                      >
                        <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-[#00e5a0]/10">
                          <MapPin className="h-4 w-4 text-[#00b07d]" />
                        </div>
                        <span
                          className="text-base font-semibold tracking-wider text-[#1e293b] uppercase"
                          style={{ fontFamily: 'var(--font-display)' }}
                        >
                          {zone.zoneId}
                        </span>
                      </button>
                      <div className="flex items-center gap-3">
                        {zone.sensorsWithCoords.length > 0 && (
                          <div className="flex items-center gap-1.5 text-xs text-[#94a3b8]" style={{ fontFamily: 'var(--font-mono)' }}>
                            <Cpu className="h-3.5 w-3.5" />
                            {zone.sensorsWithCoords.length}
                          </div>
                        )}
                        <Badge className="bg-[#f1f5f9] text-[#64748b]">
                          {zone.area > 0 ? formatArea(zone.area) : '< 0.1 km²'}
                        </Badge>

                        {/* Edit button */}
                        <button
                          onClick={(e) => { e.stopPropagation(); handleEditOpen(zone.zoneId, zone.zoneId, zone.sensorsWithCoords.map(s => s.sensorId)) }}
                          className="flex items-center justify-center w-8 h-8 rounded-lg border border-[#e2e8f0] hover:border-[#00e5a0] hover:bg-[#00e5a0]/5 transition-all duration-150"
                          title="Modifier la zone"
                        >
                          <Pencil className="h-3.5 w-3.5 text-[#64748b] hover:text-[#00b07d]" />
                        </button>

                        {/* Delete button */}
                        <button
                          onClick={(e) => { e.stopPropagation(); handleDeleteOpen(zone.zoneId) }}
                          className="flex items-center justify-center w-8 h-8 rounded-lg border border-[#e2e8f0] hover:border-red-300 hover:bg-red-50 transition-all duration-150"
                          title="Supprimer la zone"
                        >
                          <Trash2 className="h-3.5 w-3.5 text-[#94a3b8] hover:text-red-500" />
                        </button>

                        {expandedZone === zone.zoneId
                          ? <ChevronDown className="h-4 w-4 text-[#94a3b8]" />
                          : <ChevronRight className="h-4 w-4 text-[#94a3b8]" />
                        }
                      </div>
                    </div>

                    {/* Expanded details */}
                    {expandedZone === zone.zoneId && (
                      <div className="mt-2 ml-4 pl-4 border-l-2 border-[#00e5a0]/30 space-y-3">
                        {/* Sensor list */}
                        {zone.sensorsWithCoords.length > 0 ? (
                          <div className="flex flex-wrap gap-2">
                            {zone.sensorsWithCoords.map((s) => (
                              <Badge
                                key={s.sensorId}
                                className="bg-[#f1f5f9] text-[#64748b] font-normal"
                                style={{ fontFamily: 'var(--font-mono)' }}
                              >
                                <Cpu className="h-3 w-3 mr-1 inline" />
                                {s.sensorId}
                              </Badge>
                            ))}
                          </div>
                        ) : (
                          <p className="text-xs text-[#94a3b8] italic" style={{ fontFamily: 'var(--font-mono)' }}>
                            Aucun capteur associé
                          </p>
                        )}
                        {/* Area */}
                        <div className="flex items-center gap-2">
                          <Square className="h-3.5 w-3.5 text-[#94a3b8]" />
                          <span className="text-xs text-[#94a3b8]" style={{ fontFamily: 'var(--font-mono)' }}>
                            {zone.area > 0 ? `${formatArea(zone.area)} (${zone.sensorsWithCoords.length} capteurs)` : 'Surface non calculable (< 3 capteurs)'}
                          </span>
                        </div>
                        {/* View on map button */}
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleViewOnMap(zone.zoneId)}
                          className="mt-1 flex items-center gap-2 text-xs"
                        >
                          <MapIcon className="h-3.5 w-3.5" />
                          Visualiser sur la carte
                        </Button>
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            </>
          )}
        </CardContent>
      </Card>

      {/* Create Zone Dialog */}
      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle style={{ fontFamily: 'var(--font-display)' }}>
              Nouvelle zone
            </DialogTitle>
            <DialogDescription style={{ fontFamily: 'var(--font-mono)' }}>
              Définissez un périmètre urbain et associez-y des capteurs.
            </DialogDescription>
          </DialogHeader>

          <div className="flex flex-col gap-5 mt-2">
            {/* Zone name */}
            <div className="flex flex-col gap-2">
              <label
                className="text-[11px] text-[#94a3b8] tracking-[0.15em] uppercase"
                style={{ fontFamily: 'var(--font-mono)' }}
              >
                Nom de la zone <span className="text-red-400">*</span>
              </label>
              <Input
                placeholder="ex : Zone Nord, Quartier Vaugueux…"
                value={zoneName}
                onChange={(e) => setZoneName(e.target.value)}
                className="h-11"
              />
            </div>

            {/* Sensor selector */}
            <div className="flex flex-col gap-2">
              <div className="flex items-center justify-between">
                <label
                  className="text-[11px] text-[#94a3b8] tracking-[0.15em] uppercase"
                  style={{ fontFamily: 'var(--font-mono)' }}
                >
                  Capteurs associés
                  {selectedSensors.length > 0 && (
                    <span className="ml-1 text-[#00b07d]">({selectedSensors.length})</span>
                  )}
                </label>
                {selectedSensors.length > 0 && (
                  <button
                    onClick={() => setShowMap(v => !v)}
                    className="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg border transition-colors"
                    style={{
                      fontFamily: 'var(--font-mono)',
                      borderColor: showMap ? '#00e5a0' : '#e2e8f0',
                      color: showMap ? '#00b07d' : '#64748b',
                      background: showMap ? '#00e5a0/5' : 'transparent',
                    }}
                  >
                    <MapIcon className="h-3.5 w-3.5" />
                    {showMap ? 'Masquer' : 'Visualiser'} sur carte
                  </button>
                )}
              </div>

              {/* Search */}
              <Input
                placeholder="Rechercher un capteur…"
                value={searchSensor}
                onChange={(e) => setSearchSensor(e.target.value)}
                className="h-9 text-sm"
              />

              {/* Map preview */}
              {showMap && selectedSensorData.length > 0 && (
                <div className="rounded-xl overflow-hidden border border-[#e2e8f0]" style={{ height: 220 }}>
                  <MapContainer
                    center={mapCenter}
                    zoom={13}
                    style={{ width: '100%', height: '100%' }}
                    zoomControl
                    attributionControl={false}
                  >
                    <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" subdomains="abc" />
                    <FitBounds coords={mapCoords} />
                    {selectedSensorData.map((sensor) => (
                      <Marker
                        key={sensor.sensorId}
                        position={[sensor.lat, sensor.lng]}
                        icon={makeMarkerIcon(sensor.sensorTypeId, true)}
                      />
                    ))}
                  </MapContainer>
                </div>
              )}

              <div className="max-h-52 overflow-y-auto rounded-xl border border-[#e2e8f0] divide-y divide-[#f1f5f9]">
                {filteredSensors.length === 0 && (
                  <p
                    className="text-xs text-[#94a3b8] text-center py-6 tracking-wider"
                    style={{ fontFamily: 'var(--font-mono)' }}
                  >
                    Aucun capteur disponible
                  </p>
                )}
                {filteredSensors.map((sensor) => (
                  <label
                    key={sensor.sensorId}
                    className="flex items-center gap-3 px-4 py-3 cursor-pointer hover:bg-[#f8fafc] transition-colors"
                  >
                    <Checkbox
                      id={`sensor-${sensor.sensorId}`}
                      checked={selectedSensors.includes(sensor.sensorId)}
                      onCheckedChange={() => toggleSensor(sensor.sensorId)}
                    />
                    <div className="flex flex-col">
                      <span
                        className="text-sm font-semibold text-[#1e293b] tracking-wider uppercase"
                        style={{ fontFamily: 'var(--font-display)' }}
                      >
                        {sensor.sensorId}
                      </span>
                      <span
                        className="text-xs text-[#94a3b8] tracking-wider"
                        style={{ fontFamily: 'var(--font-mono)' }}
                      >
                        {sensor.sensorTypeId}
                      </span>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            {submitError && (
              <p
                className="text-xs text-red-500 tracking-wider"
                style={{ fontFamily: 'var(--font-mono)' }}
              >
                {submitError}
              </p>
            )}
          </div>

          <DialogFooter className="mt-4 gap-2">
            <Button
              variant="outline"
              onClick={() => handleOpenChange(false)}
              disabled={createZone.isPending}
            >
              Annuler
            </Button>
            <Button
              onClick={handleCreate}
              disabled={createZone.isPending || !zoneName.trim()}
              className="bg-[#00e5a0] text-[#0d0f14] hover:bg-[#00e5a0]/90"
            >
              {createZone.isPending ? 'Création…' : 'Créer la zone'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Zone Dialog */}
      <Dialog open={editOpen} onOpenChange={handleEditOpenChange}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle style={{ fontFamily: 'var(--font-display)' }}>
              Modifier la zone
            </DialogTitle>
            <DialogDescription style={{ fontFamily: 'var(--font-mono)' }}>
              Renommez la zone ou modifiez les capteurs associés.
            </DialogDescription>
          </DialogHeader>

          <div className="flex flex-col gap-5 mt-2">
            {/* Zone name */}
            <div className="flex flex-col gap-2">
              <label
                className="text-[11px] text-[#94a3b8] tracking-[0.15em] uppercase"
                style={{ fontFamily: 'var(--font-mono)' }}
              >
                Nom de la zone <span className="text-red-400">*</span>
              </label>
              <Input
                placeholder="ex : Zone Nord, Quartier Vaugueux…"
                value={editZoneName}
                onChange={(e) => setEditZoneName(e.target.value)}
                className="h-11"
              />
            </div>

            {/* Sensor selector */}
            <div className="flex flex-col gap-2">
              <label
                className="text-[11px] text-[#94a3b8] tracking-[0.15em] uppercase"
                style={{ fontFamily: 'var(--font-mono)' }}
              >
                Capteurs associés
                {editSelectedSensors.length > 0 && (
                  <span className="ml-1 text-[#00b07d]">({editSelectedSensors.length})</span>
                )}
              </label>

              <Input
                placeholder="Rechercher un capteur…"
                value={searchSensor}
                onChange={(e) => setSearchSensor(e.target.value)}
                className="h-9 text-sm"
              />

              <div className="max-h-52 overflow-y-auto rounded-xl border border-[#e2e8f0] divide-y divide-[#f1f5f9]">
                {filteredSensors.length === 0 && (
                  <p
                    className="text-xs text-[#94a3b8] text-center py-6 tracking-wider"
                    style={{ fontFamily: 'var(--font-mono)' }}
                  >
                    Aucun capteur disponible
                  </p>
                )}
                {filteredSensors.map((sensor) => (
                  <label
                    key={sensor.sensorId}
                    className="flex items-center gap-3 px-4 py-3 cursor-pointer hover:bg-[#f8fafc] transition-colors"
                  >
                    <Checkbox
                      id={`edit-sensor-${sensor.sensorId}`}
                      checked={editSelectedSensors.includes(sensor.sensorId)}
                      onCheckedChange={() => {
                        setEditSelectedSensors(prev =>
                          prev.includes(sensor.sensorId)
                            ? prev.filter(id => id !== sensor.sensorId)
                            : [...prev, sensor.sensorId]
                        )
                      }}
                    />
                    <div className="flex flex-col">
                      <span
                        className="text-sm font-semibold text-[#1e293b] tracking-wider uppercase"
                        style={{ fontFamily: 'var(--font-display)' }}
                      >
                        {sensor.sensorId}
                      </span>
                      <span
                        className="text-xs text-[#94a3b8] tracking-wider"
                        style={{ fontFamily: 'var(--font-mono)' }}
                      >
                        {sensor.sensorTypeId}
                      </span>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            {editSubmitError && (
              <p
                className="text-xs text-red-500 tracking-wider"
                style={{ fontFamily: 'var(--font-mono)' }}
              >
                {editSubmitError}
              </p>
            )}
          </div>

          <DialogFooter className="mt-4 gap-2">
            <Button
              variant="outline"
              onClick={() => handleEditOpenChange(false)}
              disabled={updateZone.isPending}
            >
              Annuler
            </Button>
            <Button
              onClick={handleEditSave}
              disabled={updateZone.isPending || !editZoneName.trim()}
              className="bg-[#00e5a0] text-[#0d0f14] hover:bg-[#00e5a0]/90"
            >
              {updateZone.isPending ? 'Mise à jour…' : 'Enregistrer'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle style={{ fontFamily: 'var(--font-display)' }}>
              Supprimer la zone
            </DialogTitle>
            <DialogDescription style={{ fontFamily: 'var(--font-mono)' }}>
              Êtes-vous sûr de vouloir supprimer la zone <strong>{deletingZoneId}</strong> ? Cette action est irréversible. Les capteurs ne seront pas supprimés.
            </DialogDescription>
          </DialogHeader>

          {deleteSubmitError && (
            <p
              className="text-xs text-red-500 tracking-wider"
              style={{ fontFamily: 'var(--font-mono)' }}
            >
              {deleteSubmitError}
            </p>
          )}

          <DialogFooter className="mt-4 gap-2">
            <Button
              variant="outline"
              onClick={() => setDeleteOpen(false)}
              disabled={deleteZone.isPending}
            >
              Annuler
            </Button>
            <Button
              onClick={handleDeleteConfirm}
              disabled={deleteZone.isPending}
              className="bg-red-500 text-white hover:bg-red-600"
            >
              {deleteZone.isPending ? 'Suppression…' : 'Supprimer'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

export default ZonesPage