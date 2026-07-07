import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { X } from 'lucide-react'
import { useMeasures } from '../queries/measureQueries'
import ComparisonChart from '../components/ComparisonChart'
import type { Sensor } from '../types'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Combobox } from '@/components/ui/combobox'
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select'
import { Breadcrumb } from '@/components/ui/breadcrumb'

type Period = '1h' | '24h' | '1w' | '1m' | '90d' | '1y'
const PERIOD_LABELS: Record<Period, string> = {
  '1h': 'Dernière heure',
  '24h': '24 heures',
  '1w': '1 semaine',
  '1m': '1 mois',
  '90d': '90 jours',
  '1y': '1 an',
}

const msOf = (p: Period): number => {
  switch (p) {
    case '1h': return 60 * 60 * 1000
    case '24h': return 24 * 60 * 60 * 1000
    case '1w': return 7 * 24 * 60 * 60 * 1000
    case '1m': return 30 * 24 * 60 * 60 * 1000
    case '90d': return 90 * 24 * 60 * 60 * 1000
    case '1y': return 365 * 24 * 60 * 60 * 1000
  }
}

const ComparisonPage = () => {
  const { data } = useMeasures()
  const [searchParams] = useSearchParams()
  const initialSelected = searchParams.getAll('sensor_id')

  const [selectedSensors, setSelectedSensors] = useState<string[]>(initialSelected)
  const [userSelectedType, setUserSelectedType] = useState<string>('all')
  const [period, setPeriod] = useState<Period>('24h')

  const sensors = useMemo<Sensor[]>(() => {
    if (!data) return []
    const map = new Map<string, Sensor>()
    data.forEach((m) => {
      if (!map.has(m.sensorId)) {
        map.set(m.sensorId, {
          uuid: '',
          sensorId: m.sensorId,
          latitude: m.latitude,
          longitude: m.longitude,
          status: m.sensorStatus,
          zoneId: m.zoneId,
          sensorTypeId: m.sensorTypeId,
        })
      }
    })
    return Array.from(map.values())
  }, [data])

  const uniqueTypes = useMemo(() => {
    return Array.from(new Set(sensors.map((s) => s.sensorTypeId)))
  }, [sensors])

  const activeType = useMemo(() => {
    if (selectedSensors.length > 0) {
      const sensor = sensors.find((s) => s.sensorId === selectedSensors[0])
      return sensor?.sensorTypeId || 'all'
    }
    return userSelectedType
  }, [selectedSensors, sensors, userSelectedType])

  const selectableSensors = useMemo(() => {
    if (activeType === 'all' || !activeType) return sensors
    return sensors.filter((s) => s.sensorTypeId === activeType)
  }, [sensors, activeType])

  const selectedMeasurements = useMemo(() => {
    if (!data || selectedSensors.length === 0) return []
    const cutoff = Date.now() - msOf(period)
    return data.filter((m) =>
      selectedSensors.includes(m.sensorId) &&
      new Date(m.timestamp).getTime() >= cutoff
    )
  }, [data, selectedSensors, period])

  const measurementUnit = useMemo(() => {
    if (selectedMeasurements.length === 0) return ''
    return selectedMeasurements[0].unit
  }, [selectedMeasurements])

  const sensorOptions = useMemo(() => {
    return selectableSensors
      .filter((s) => !selectedSensors.includes(s.sensorId))
      .map((s) => ({ value: s.sensorId, label: s.sensorId }))
  }, [selectableSensors, selectedSensors])

  const addSensor = (sensorId: string) => {
    if (selectedSensors.length >= 10) return
    if (!selectedSensors.includes(sensorId)) {
      setSelectedSensors([...selectedSensors, sensorId])
    }
  }

  const removeSensor = (sensorId: string) => {
    setSelectedSensors(selectedSensors.filter((id) => id !== sensorId))
  }

  const handleTypeChange = (val: string) => {
    setUserSelectedType(val)
  }

  return (
    <div>
      <Breadcrumb items={[{ label: 'Comparer' }]} className="mb-6" />
      <header className="mb-10">
        <p className="text-[12px] text-[#00b07d] tracking-[0.2em] uppercase mb-2" style={{ fontFamily: 'var(--font-mono)' }}>
          Analyse comparative
        </p>
        <h1 className="text-5xl font-bold tracking-wider text-[#0d0f14] uppercase" style={{ fontFamily: 'var(--font-display)' }}>
          Comparer
        </h1>
        <div className="mt-4 h-1 w-20 bg-[#00e5a0]" />
      </header>

      <div className="mb-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="flex flex-col gap-2">
          <label className="text-[11px] text-[#64748b] tracking-[0.1em] uppercase font-medium" style={{ fontFamily: 'var(--font-mono)' }}>
            Type
          </label>
          <Select disabled={selectedSensors.length > 0} value={activeType} onValueChange={handleTypeChange}>
            <SelectTrigger>
              <SelectValue placeholder="Tous les types" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tous les types</SelectItem>
              {uniqueTypes.map((t) => (
                <SelectItem key={t} value={t}>{t}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="flex flex-col gap-2">
          <label className="text-[11px] text-[#64748b] tracking-[0.1em] uppercase font-medium" style={{ fontFamily: 'var(--font-mono)' }}>
            Capteur
          </label>
          <Combobox
            options={sensorOptions}
            value=""
            onChange={addSensor}
            placeholder={activeType !== 'all' ? 'Choisir un capteur...' : 'Sélectionner un capteur...'}
            emptyMessage="Aucun capteur disponible"
            disabled={activeType !== 'all' && selectableSensors.length === 0}
          />
        </div>

        <div className="flex flex-col gap-2">
          <label className="text-[11px] text-[#64748b] tracking-[0.1em] uppercase font-medium" style={{ fontFamily: 'var(--font-mono)' }}>
            Période
          </label>
          <Select value={period} onValueChange={(v) => setPeriod(v as Period)}>
            <SelectTrigger className="w-full">
              <SelectValue>{PERIOD_LABELS[period]}</SelectValue>
            </SelectTrigger>
            <SelectContent>
              {(Object.keys(PERIOD_LABELS) as Period[]).map((p) => (
                <SelectItem key={p} value={p}>{PERIOD_LABELS[p]}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {selectedSensors.length > 0 && (
        <div className="mb-6 flex flex-wrap gap-2">
          {selectedSensors.map((sensorId) => {
            const sensor = sensors.find((s) => s.sensorId === sensorId)
            return (
              <Badge key={sensorId} className="bg-[#00e5a0]/10 text-[#00b07d] border border-[#00e5a0]/30 px-3 py-2">
                <span className={`w-1.5 h-1.5 rounded-full mr-1.5 ${sensor?.status ? 'bg-[#00e5a0]' : 'bg-[#94a3b8]'}`} />
                {sensorId}
                <button
                  onClick={() => removeSensor(sensorId)}
                  className="ml-1.5 hover:text-red-500"
                >
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            )
          })}
          {selectedSensors.length > 0 && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setSelectedSensors([])}
              className="text-xs text-[#94a3b8] hover:text-red-500 ml-2"
            >
              Effacer
            </Button>
          )}
        </div>
      )}

      {selectedSensors.length > 0 ? (
        <Card>
          <CardContent className="p-6">
            <ComparisonChart data={selectedMeasurements} unit={measurementUnit} height={450} />
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="p-12 text-center">
            <p className="text-xl font-semibold tracking-wider text-[#94a3b8] uppercase mb-2" style={{ fontFamily: 'var(--font-display)' }}>
              Aucun capteur sélectionné
            </p>
            <p className="text-sm text-[#94a3b8] tracking-wide" style={{ fontFamily: 'var(--font-mono)' }}>
              Sélectionnez un type puis jusqu'à 10 capteurs pour comparer leurs mesures
            </p>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

export default ComparisonPage