import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { Layout } from './components/Layout'
import { AuthPage } from './pages/AuthPage'
import { CountryPage } from './pages/CountryPage'
import { NewStampPage } from './pages/NewStampPage'
import { PassportPage } from './pages/PassportPage'
import { StampDetailPage } from './pages/StampDetailPage'

/** URL → screen. Everything inside <Layout> requires being logged in. */
export function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<AuthPage mode="login" />} />
          <Route path="/register" element={<AuthPage mode="register" />} />
          <Route element={<Layout />}>
            <Route index element={<PassportPage />} />
            <Route path="/countries/:code" element={<CountryPage />} />
            <Route path="/countries/:code/stamp" element={<NewStampPage />} />
            <Route path="/stamps/:id" element={<StampDetailPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
