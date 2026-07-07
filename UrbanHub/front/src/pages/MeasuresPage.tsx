import { useMeasures } from '../queries/measureQueries'
import DataGraph from '../components/DataGraph'
import { Card, CardContent } from '@/components/ui/card'
import { Breadcrumb } from '@/components/ui/breadcrumb'
import { Skeleton } from '@/components/ui/skeleton'

const MeasuresPage = () => {
  const { data, isLoading, isError } = useMeasures()

  return (
    <div>
      <Breadcrumb items={[{ label: 'Mesures' }]} className="mb-6" />
      <header className="mb-8">
        <p className="text-[10px] text-[#00e5a0] tracking-[0.2em] uppercase mb-1" style={{ fontFamily: 'var(--font-mono)' }}>
          Données temps réel
        </p>
        <h1 className="text-4xl font-bold tracking-wider text-white uppercase" style={{ fontFamily: 'var(--font-display)' }}>
          Mesures
        </h1>
        <div className="mt-3 h-px w-16 bg-[#00e5a0]" />
      </header>

      <Card className="bg-[#111318] border-[#1e2230]">
        <CardContent className="p-6">
          {isLoading ? (
            <div className="space-y-4">
              <Skeleton className="h-64 w-full" />
              <Skeleton className="h-4 w-48" />
            </div>
          ) : isError ? (
            <p className="text-xs text-red-500/70 tracking-widest" style={{ fontFamily: 'var(--font-mono)' }}>
              Erreur de connexion au backend.
            </p>
          ) : data ? (
            <DataGraph data={data} />
          ) : null}
        </CardContent>
      </Card>
    </div>
  )
}

export default MeasuresPage