import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { ApiError } from "../../api-client";
import { SessionManager, SessionState, LocalStorageSessionStorage } from "../../auth-core";

interface AuthContextValue {
  state: SessionState;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  bootstrapReady: boolean;
  error: string | null;
  clearError: () => void;
  manager: SessionManager;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const BASE_URL = "http://localhost:8081";

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<SessionState>({
    token: null,
    user: null,
    isBootstrapped: false,
  });
  const [error, setError] = useState<string | null>(null);

  const manager = useMemo(
    () =>
      new SessionManager({
        baseUrl: BASE_URL,
        storage: new LocalStorageSessionStorage("loyalty_admin_jwt"),
      }),
    []
  );

  useEffect(() => {
    const unsubscribe = manager.subscribe((nextState) => setState(nextState));
    manager.bootstrap().catch(() => {
      setError("No fue posible inicializar la sesión.");
    });
    return unsubscribe;
  }, [manager]);

  const value: AuthContextValue = {
    state,
    bootstrapReady: state.isBootstrapped,
    manager,
    error,
    clearError: () => setError(null),
    login: async (username, password) => {
      setError(null);
      try {
        await manager.login({ username, password });
      } catch (e) {
        if (e instanceof ApiError) {
          setError(e.message);
          return;
        }
        setError("Error inesperado en login.");
      }
    },
    logout: async () => {
      await manager.logout();
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth debe usarse dentro de AuthProvider");
  }
  return ctx;
}

