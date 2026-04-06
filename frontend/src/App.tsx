import { Navigate, Route, Routes } from "react-router-dom";
import { ApiKeysPage } from "./views/ApiKeysPage";
import { DiscountSetupPage } from "./views/DiscountSetupPage";
import { DashboardPage } from "./views/DashboardPage";
import { EcommercesPage } from "./views/EcommercesPage";
import { LoginPage } from "./views/LoginPage";
import { RulesTiersPage } from "./views/RulesTiersPage";
import { RequireAuth } from "./session/RequireAuth";
import { RequireRole } from "./session/RequireRole";
import { ProfilePage } from "./views/ProfilePage";
import { UsersPage } from "./views/UsersPage";

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
      <Route
        path="/profile"
        element={
          <RequireAuth>
            <ProfilePage />
          </RequireAuth>
        }
      />
      <Route
        path="/users"
        element={
          <RequireAuth>
            <RequireRole roles={["SUPER_ADMIN", "STORE_ADMIN"]}>
              <UsersPage />
            </RequireRole>
          </RequireAuth>
        }
      />
      <Route
        path="/ecommerces"
        element={
          <RequireAuth>
            <RequireRole roles={["SUPER_ADMIN"]}>
              <EcommercesPage />
            </RequireRole>
          </RequireAuth>
        }
      />
      <Route
        path="/api-keys"
        element={
          <RequireAuth>
            <RequireRole roles={["SUPER_ADMIN", "STORE_ADMIN"]}>
              <ApiKeysPage />
            </RequireRole>
          </RequireAuth>
        }
      />
      <Route
        path="/discount-setup"
        element={
          <RequireAuth>
            <RequireRole roles={["ADMIN", "SUPER_ADMIN", "STORE_ADMIN"]}>
              <DiscountSetupPage />
            </RequireRole>
          </RequireAuth>
        }
      />
      <Route
        path="/rules-tiers"
        element={
          <RequireAuth>
            <RequireRole roles={["SUPER_ADMIN", "STORE_ADMIN", "ADMIN"]}>
              <RulesTiersPage />
            </RequireRole>
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
