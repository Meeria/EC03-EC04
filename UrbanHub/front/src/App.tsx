import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { Sidebar } from './components/Sidebar'
import { Sonner } from './components/ui/sonner'
import DashboardPage from './pages/DashboardPage'
import ZonesPage from './pages/ZonesPage'
import SensorsPage from './pages/SensorsPage'
import SensorDetailPage from './pages/SensorDetailPage'
import SensorTypesPage from './pages/SensorTypesPage'
import ComparisonPage from './pages/ComparisonPage'
import MapPage from './pages/MapPage'
import KpiPage from './pages/KpiPage'

function App() {
  return (
    <BrowserRouter>
      <div className="flex h-screen w-full overflow-hidden">
        <Sidebar />
        <main className="flex-1 bg-[#f8fafc] overflow-y-auto">
          <div className="p-6 md:p-8">
            <Routes>
              <Route path="/" element={<DashboardPage />} />
              <Route path="/zones" element={<ZonesPage />} />
              <Route path="/capteurs" element={<SensorsPage />} />
              <Route path="/capteurs/:id" element={<SensorDetailPage />} />
              <Route path="/comparer" element={<ComparisonPage />} />
              <Route path="/carte" element={<MapPage />} />
              <Route path="/types-capteur" element={<SensorTypesPage />} />
              <Route path="/kpis" element={<KpiPage />} />
            </Routes>
          </div>
        </main>
        <Sonner />
      </div>
    </BrowserRouter>
  )
}

export default App
