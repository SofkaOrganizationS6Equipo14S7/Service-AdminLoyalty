import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import './Dashboard.module.css';

const Dashboard: React.FC = () => {
  const { user, logout, isLoading } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await logout();
      navigate('/login', { replace: true });
    } catch (err) {
      console.error('Error al cerrar sesión:', err);
    }
  };

  return (
    <div className="dashboard-page">
      <header className="dashboard-header">
        <div className="header-content">
          <h1>LOYALTY Dashboard</h1>
          <div className="user-section">
            <span className="user-info">
              {user?.username && (
                <>
                  Hola, <strong>{user.username}</strong> ({user.role})
                </>
              )}
            </span>
            <button
              onClick={handleLogout}
              disabled={isLoading}
              className="btn-logout"
            >
              {isLoading ? 'Cerrando sesión...' : 'Cerrar Sesión'}
            </button>
          </div>
        </div>
      </header>

      <main className="dashboard-content">
        <div className="welcome-card">
          <h2>Bienvenido al Dashboard</h2>
          <p>
            Esta es la página principal del sistema LOYALTY. Aquí podrás acceder a todas
            las funcionalidades disponibles para tu rol.
          </p>
          <div className="user-details">
            <h3>Información del Usuario</h3>
            <dl>
              <dt>Usuario:</dt>
              <dd>{user?.username}</dd>
              <dt>Rol:</dt>
              <dd>{user?.role}</dd>
            </dl>
          </div>
        </div>

        <div className="features-grid">
          <div className="feature-card">
            <h3>Gestión de Datos</h3>
            <p>Administra y consulta tus datos principales</p>
          </div>
          <div className="feature-card">
            <h3>Reportes</h3>
            <p>Genera reportes detallados de tus actividades</p>
          </div>
          <div className="feature-card">
            <h3>Configuración</h3>
            <p>Actualiza tu perfil y preferencias</p>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Dashboard;
