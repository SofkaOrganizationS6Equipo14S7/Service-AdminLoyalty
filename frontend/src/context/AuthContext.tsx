import React, { createContext, ReactNode, useEffect, useState } from 'react';
import authService from '../services/authService';

interface User {
  username: string;
  role: string;
}

interface AuthContextType {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  validateToken: () => Promise<boolean>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Inicializar estado desde localStorage al montar
  useEffect(() => {
    const initializeAuth = async () => {
      const savedToken = authService.getToken();
      const savedUser = authService.getUser();

      if (savedToken && savedUser) {
        setToken(savedToken);
        setUser(savedUser);
        
        // Validar token con el backend
        try {
          await authService.getCurrentUser(savedToken);
        } catch {
          // Token expirado o inválido
          setToken(null);
          setUser(null);
        }
      }

      setIsLoading(false);
    };

    initializeAuth();
  }, []);

  const login = async (username: string, password: string): Promise<void> => {
    setIsLoading(true);
    try {
      const response = await authService.login(username, password);
      setToken(response.token);
      setUser({
        username: response.username,
        role: response.role,
      });
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async (): Promise<void> => {
    setIsLoading(true);
    try {
      await authService.logout();
      setToken(null);
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  };

  const validateToken = async (): Promise<boolean> => {
    const currentToken = authService.getToken();
    if (!currentToken) return false;

    try {
      await authService.getCurrentUser(currentToken);
      return true;
    } catch {
      setToken(null);
      setUser(null);
      return false;
    }
  };

  const value: AuthContextType = {
    token,
    user,
    isAuthenticated: !!token,
    isLoading,
    login,
    logout,
    validateToken,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
