import { useMemo, useState, useEffect, useRef } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ArrowUpDown, ArrowUp, ArrowDown, X, Search } from 'lucide-react'
import { useSensors } from '../queries/sensorQueries'
import type { Sensor } from '../types'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select'
import { Input } from '@/components/ui/input'
import { Breadcrumb } from '@/components/ui/breadcrumb'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { Skeleton } from '@/components/ui/skeleton'

type SortKey = 'sensorId' | 'type' | 'status'
type SortDir = 'asc' | 'desc'

const SensorsPage = () => {
  const { data, isLoading, isError, isFetching } = useSensors()
  const [searchParams, setSearchParams] = useSearchParams()
  const typeFilter = searchParams.get('type')

  const [sortKey, setSortKey] = useState<SortKey>('sensorId')
  const [sortDir, setSortDir] = useState<SortDir>('asc')
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(5)
  const [search, setSearch] = useState('')

  const sensors = useMemo<Sensor[]>(() => data ?? [], [data])

  // Benchmark : temps de chargement frontend
  const fetchStartRef = useRef<number | null>(null)
  const [fetchDuration, setFetchDuration] = useState<number | null>(null)

  useEffect(() => {
    if (isFetching) {
      fetchStartRef.current = performance.now()
    } else if (!isFetching && fetchStartRef.current !== null && sensors) {
      const elapsed = (performance.now() - fetchStartRef.current) / 1000
      setFetchDuration(elapsed)
      fetchStartRef.current = null
    }
  }, [isFetching, sensors])

  useEffect(() => {
    setCurrentPage(1)
  }, [search, typeFilter, sortKey, sortDir, pageSize])

  const filteredSensors = useMemo(() => {
    if (!sensors) return []
    let result = [...sensors]

    if (search.trim()) {
      const q = search.trim().toLowerCase()
      result = result.filter(
        (s) =>
          s.sensorId.toLowerCase().includes(q) ||
          s.sensorTypeId.toLowerCase().includes(q)
      )
    }

    if (typeFilter) {
      result = result.filter((s) => s.sensorTypeId === typeFilter)
    }

    result.sort((a, b) => {
      let cmp = 0
      if (sortKey === 'sensorId') {
        cmp = a.sensorId.localeCompare(b.sensorId)
      } else if (sortKey === 'type') {
        cmp = a.sensorTypeId.localeCompare(b.sensorTypeId)
      } else if (sortKey === 'status') {
        cmp = (a.status === b.status) ? 0 : a.status ? -1 : 1
      }
      return sortDir === 'asc' ? cmp : -cmp
    })

    return result
  }, [sensors, search, typeFilter, sortKey, sortDir])

  const totalPages = Math.max(1, Math.ceil(filteredSensors.length / pageSize))
  const paginatedSensors = filteredSensors.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize
  )

  const uniqueTypes = useMemo(() => {
    return Array.from(new Set((sensors ?? []).map((s) => s.sensorTypeId)))
  }, [sensors])

  const toggleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortKey(key)
      setSortDir('asc')
    }
  }

  const SortIcon = ({ active, dir }: { active: boolean; dir: SortDir }) => (
    active ? (dir === 'asc' ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />) : <ArrowUpDown className="h-3 w-3 text-[#94a3b8]" />
  )

  const handleTypeChange = (val: string) => {
    const params = new URLSearchParams(searchParams)
    if (val && val !== 'all') {
      params.set('type', val)
    } else {
      params.delete('type')
    }
    setSearchParams(params)
  }

  const clearFilters = () => {
    setSearch('')
    const params = new URLSearchParams(searchParams)
    params.delete('type')
    setSearchParams(params)
  }

  const handlePageSizeChange = (val: string) => {
    setPageSize(Number(val))
    setCurrentPage(1)
  }

  // Generate page numbers to display (max ~7 buttons)
  const pageNumbers = useMemo(() => {
    if (totalPages <= 7) return Array.from({ length: totalPages }, (_, i) => i + 1)
    const pages: (number | '...')[] = []
    if (currentPage <= 4) {
      pages.push(1, 2, 3, 4, 5, '...', totalPages)
    } else if (currentPage >= totalPages - 3) {
      pages.push(1, '...', totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1, totalPages)
    } else {
      pages.push(1, '...', currentPage - 1, currentPage, currentPage + 1, '...', totalPages)
    }
    return pages
  }, [totalPages, currentPage])

  return (
    <div>
      <Breadcrumb items={[{ label: 'Capteurs' }]} className="mb-6" />
      <header className="mb-8">
        <p className="text-[11px] text-[#00b07d] tracking-[0.2em] uppercase mb-2" style={{ fontFamily: 'var(--font-mono)' }}>
          Dispositifs de mesure
        </p>
        <h1 className="text-5xl font-bold tracking-wider text-[#0d0f14] uppercase" style={{ fontFamily: 'var(--font-display)' }}>
          Capteurs
        </h1>
        <div className="mt-4 h-1 w-20 bg-[#00e5a0]" />
      </header>

      <Card className="mb-6">
        <CardContent className="p-6">
          {/* Search bar */}
          <div className="relative mb-4">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#94a3b8] pointer-events-none" />
            <Input
              placeholder="Rechercher un capteur par nom ou type…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9 h-11"
            />
            {search && (
              <button
                onClick={() => setSearch('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[#94a3b8] hover:text-[#1e293b] transition-colors"
              >
                <X className="h-4 w-4" />
              </button>
            )}
          </div>

          <div className="flex flex-wrap items-end gap-4">
            <div className="flex flex-col gap-2">
              <label className="text-[11px] text-[#94a3b8] tracking-[0.1em] uppercase" style={{ fontFamily: 'var(--font-mono)' }}>
                Type
              </label>
              <ToggleGroup
                type="single"
                value={typeFilter || 'all'}
                onValueChange={(val) => { if (val) handleTypeChange(val === 'all' ? '' : val) }}
              >
                <ToggleGroupItem value="all" variant="outline" className="px-3 py-2">
                  Tous
                </ToggleGroupItem>
                {uniqueTypes.map((t) => (
                  <ToggleGroupItem key={t} value={t} variant="outline" className="px-3 py-2">
                    {t}
                  </ToggleGroupItem>
                ))}
              </ToggleGroup>
            </div>

            <div className="flex flex-col gap-2 ml-auto">
              <label className="text-[11px] text-[#94a3b8] tracking-[0.1em] uppercase" style={{ fontFamily: 'var(--font-mono)' }}>
                Trier par
              </label>
              <ToggleGroup
                type="single"
                value={sortKey}
                onValueChange={(val) => {
                  if (!val) {
                    // clicking the already selected item → toggle direction
                    setSortDir(d => d === 'asc' ? 'desc' : 'asc')
                  } else {
                    toggleSort(val as SortKey)
                  }
                }}
              >
                {(['sensorId', 'type', 'status'] as SortKey[]).map((key) => {
                  const labels: Record<SortKey, string> = { sensorId: 'Nom', type: 'Type', status: 'Statut' }
                  return (
                    <ToggleGroupItem
                      key={key}
                      value={key}
                      variant="default"
                      className="px-3 py-2 gap-1.5"
                    >
                      {labels[key]}
                      <SortIcon active={sortKey === key} dir={sortDir} />
                    </ToggleGroupItem>
                  )
                })}
              </ToggleGroup>
            </div>
          </div>

          {(typeFilter || search) && (
            <div className="mt-4 flex items-center gap-2 pt-4 border-t border-[#e2e8f0]">
              <span className="text-xs text-[#94a3b8] tracking-wider" style={{ fontFamily: 'var(--font-mono)' }}>
                Filtres actifs :
              </span>
              {typeFilter && (
                <Badge className="bg-[#00e5a0]/10 text-[#00b07d] border border-[#00e5a0]/30">
                  Type: {typeFilter}
                  <button onClick={() => handleTypeChange('all')} className="ml-1 hover:text-red-500">
                    <X className="h-3 w-3" />
                  </button>
                </Badge>
              )}
              <Button
                variant="ghost"
                size="sm"
                onClick={clearFilters}
                className="ml-auto text-xs text-[#94a3b8] hover:text-red-500"
              >
                Réinitialiser
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-6">
          {isLoading ? (
            <div className="space-y-4">
              <div className="flex gap-4">
                <Skeleton className="h-10 w-32" />
                <Skeleton className="h-10 w-32" />
                <Skeleton className="h-10 w-32" />
              </div>
              <Skeleton className="h-10 w-full" />
              <div className="space-y-3">
                {[1, 2, 3].map(i => (
                  <Skeleton key={i} className="h-16 w-full" />
                ))}
              </div>
            </div>
          ) : isError ? (
            <p className="text-sm text-red-500 tracking-wider" style={{ fontFamily: 'var(--font-mono)' }}>
              Erreur de connexion au backend.
            </p>
          ) : (
            <>
              {/* Count + page size selector */}
              <div className="flex items-center justify-between mb-4">
                <p className="text-xs text-[#94a3b8] tracking-wider" style={{ fontFamily: 'var(--font-mono)' }}>
                  {filteredSensors.length} capteur(s) trouvé(s)
                  {fetchDuration !== null && (
                    <span className="ml-1 text-[#00b07d]">en {fetchDuration.toFixed(2)}s</span>
                  )}
                </p>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-[#94a3b8] tracking-wider" style={{ fontFamily: 'var(--font-mono)' }}>
                    Afficher
                  </span>
                  <Select value={String(pageSize)} onValueChange={handlePageSizeChange}>
                    <SelectTrigger className="h-8 w-[72px] text-xs">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {[5, 10, 25, 50, 100].map((n) => (
                        <SelectItem key={n} value={String(n)} className="text-xs">
                          {n}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <span className="text-xs text-[#94a3b8] tracking-wider" style={{ fontFamily: 'var(--font-mono)' }}>
                    par page
                  </span>
                </div>
              </div>

              {/* Sensor list — scrollable container */}
              <div className="overflow-y-auto max-h-[50vh] pr-1">
                <ul className="flex flex-col gap-3">
                  {paginatedSensors.map((sensor) => (
                    <li key={sensor.sensorId}>
                      <Link
                        to={`/capteurs/${sensor.sensorId}`}
                        className="flex items-center justify-between px-5 py-4 rounded-lg border border-[#e2e8f0] hover:border-[#00e5a0]/50 hover:bg-[#f8fafc] transition-all duration-150 group"
                      >
                        <div className="flex items-center gap-4">
                          <span className={['w-2.5 h-2.5 rounded-full shrink-0', sensor.status ? 'bg-[#00e5a0]' : 'bg-[#94a3b8]'].join(' ')} />
                          <span className="text-base font-semibold tracking-wider uppercase" style={{ fontFamily: 'var(--font-display)', color: '#1e293b' }}>
                            {sensor.sensorId}
                          </span>
                        </div>
                        <div className="flex items-center gap-6">
                          <span className="text-sm text-[#94a3b8] tracking-wider uppercase" style={{ fontFamily: 'var(--font-mono)' }}>
                            {sensor.sensorTypeId}
                          </span>
                          <Badge className={sensor.status ? 'bg-[#00e5a0]/10 text-[#00b07d] border border-[#00e5a0]/30' : 'bg-[#f1f5f9] text-[#94a3b8]'}>
                            {sensor.status ? 'Actif' : 'Inactif'}
                          </Badge>
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-[#94a3b8] group-hover:text-[#00b07d] transition-colors">
                            <polyline points="9 18 15 12 9 6" />
                          </svg>
                        </div>
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Pagination controls */}
              {totalPages > 1 && (
                <div className="mt-6 flex items-center justify-center gap-1">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                    disabled={currentPage === 1}
                    className="h-8 px-3 text-xs"
                  >
                    ‹ Préc.
                  </Button>

                  {pageNumbers.map((p, i) =>
                    p === '...' ? (
                      <span
                        key={`ellipsis-${i}`}
                        className="w-8 text-center text-xs text-[#94a3b8]"
                        style={{ fontFamily: 'var(--font-mono)' }}
                      >
                        …
                      </span>
                    ) : (
                      <Button
                        key={p}
                        variant={currentPage === p ? 'default' : 'outline'}
                        size="sm"
                        onClick={() => setCurrentPage(p as number)}
                        className={[
                          'h-8 w-8 p-0 text-xs',
                          currentPage === p ? 'bg-[#00e5a0] text-[#0d0f14] hover:bg-[#00e5a0]/90' : '',
                        ].join(' ')}
                      >
                        {p}
                      </Button>
                    )
                  )}

                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                    disabled={currentPage === totalPages}
                    className="h-8 px-3 text-xs"
                  >
                    Suiv. ›
                  </Button>
                </div>
              )}

              {/* Page info */}
              {totalPages > 1 && (
                <p className="mt-3 text-center text-xs text-[#cbd5e1] tracking-wider" style={{ fontFamily: 'var(--font-mono)' }}>
                  Page {currentPage} sur {totalPages}
                </p>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

export default SensorsPage