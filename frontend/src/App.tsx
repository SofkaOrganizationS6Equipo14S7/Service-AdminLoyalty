import { Navigate, Route, Routes } from "react-router-dom";
import { DashboardPage } from "./views/DashboardPage";
import { LoginPage } from "./views/LoginPage";
import { RequireAuth } from "./session/RequireAuth";

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/dashboard"
        element={
          <RequireAuth>
            <DashboardPage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

