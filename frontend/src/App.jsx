import { Routes, Route, Navigate } from "react-router-dom";
import LiveDisplay from "./pages/LiveDisplay.jsx";
import AdminLogin from "./pages/AdminLogin.jsx";
import AdminDashboard from "./pages/AdminDashboard.jsx";
import PeoplePage from "./pages/PeoplePage.jsx";
import AttendanceHistory from "./pages/AttendanceHistory.jsx";
import DevicesPage from "./pages/DevicesPage.jsx";
import RequireAuth from "./components/RequireAuth.jsx";
import AdminLayout from "./components/AdminLayout.jsx";

export default function App() {
  return (
    <Routes>
      {/* Public — the big screen mounted in Lab Room 204 */}
      <Route path="/" element={<LiveDisplay />} />

      {/* Admin */}
      <Route path="/admin/login" element={<AdminLogin />} />
      <Route
        path="/admin"
        element={
          <RequireAuth>
            <AdminLayout />
          </RequireAuth>
        }
      >
        <Route index element={<AdminDashboard />} />
        <Route path="people" element={<PeoplePage />} />
        <Route path="attendance" element={<AttendanceHistory />} />
        <Route path="devices" element={<DevicesPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
