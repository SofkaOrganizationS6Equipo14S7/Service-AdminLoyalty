import React, { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import LoginForm from '../components/LoginForm';
import './LoginPage.module.css';

const LoginPage: React.FC = () => {
  const { login, isAuthenticated, isLoading } = useAuth();
  const [error, setError] = useState<string | null>(null);

  // Si ya está autenticado, redirigir al dashboard
  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleSubmit = async (username: string, password: string) => {
    setError(null);
    try {
      await login(username, password);
      // La redirección ocurre automáticamente al cambiar isAuthenticated
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Error desconocido';
      setError(message);
    }
  };

  if (isLoading) {
    return (
      <div className="login-page">
        <div className="login-container">
          <p>Cargando...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="login-page">
      <div className="login-container">
        <h1>LOYALTY</h1>
        <h2>Inicio de Sesión</h2>
        <LoginForm
          onSubmit={handleSubmit}
          isLoading={isLoading}
          error={error}
        />
        <p className="demo-info">
          Demo: usuario <strong>admin</strong>, contraseña <strong>admin123</strong>
        </p>
      </div>
    </div>
  );
};

export default LoginPage;
