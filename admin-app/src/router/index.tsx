import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import App from '../App'
import AdminLoginPage from '../pages/AdminLoginPage'
import AuditPage from '../pages/AuditPage'
import DashboardPage from '../pages/DashboardPage'

function AdminGuard() {
  return localStorage.getItem('admin_token') ? <Outlet /> : <Navigate to="/admin/login" replace />
}

export default function AdminRouter() {
  return <Routes>
    <Route path="/admin/login" element={<AdminLoginPage />} />
    <Route element={<AdminGuard />}>
      <Route path="/admin" element={<App />}>
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="audit" element={<AuditPage />} />
      </Route>
    </Route>
    <Route path="*" element={<Navigate to="/admin/dashboard" replace />} />
  </Routes>
}
