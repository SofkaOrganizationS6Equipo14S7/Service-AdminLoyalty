import React, { FormEvent, useState } from 'react';
import './LoginForm.module.css';

interface LoginFormProps {
  onSubmit: (username: string, password: string) => Promise<void>;
  isLoading?: boolean;
  error?: string | null;
}

const LoginForm: React.FC<LoginFormProps> = ({ onSubmit, isLoading = false, error = null }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [validationErrors, setValidationErrors] = useState<{ [key: string]: string }>({});

  const validateForm = (): boolean => {
    const errors: { [key: string]: string } = {};

    if (!username.trim()) {
      errors.username = 'username es obligatorio';
    }

    if (!password.trim()) {
      errors.password = 'password es obligatorio';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    try {
      await onSubmit(username, password);
      // Limpiar formulario al éxito
      setUsername('');
      setPassword('');
    } catch {
      // Los errores se manejan en el componente padre
    }
  };

  return (
    <form onSubmit={handleSubmit} className="login-form">
      <div className="form-group">
        <label htmlFor="username">Usuario</label>
        <input
          id="username"
          type="text"
          value={username}
          onChange={(e) => {
            setUsername(e.target.value);
            if (validationErrors.username) {
              setValidationErrors({ ...validationErrors, username: '' });
            }
          }}
          disabled={isLoading}
          placeholder="Ingresa tu usuario"
          className={validationErrors.username ? 'input-error' : ''}
        />
        {validationErrors.username && (
          <span className="error-message">{validationErrors.username}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="password">Contraseña</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => {
            setPassword(e.target.value);
            if (validationErrors.password) {
              setValidationErrors({ ...validationErrors, password: '' });
            }
          }}
          disabled={isLoading}
          placeholder="Ingresa tu contraseña"
          className={validationErrors.password ? 'input-error' : ''}
        />
        {validationErrors.password && (
          <span className="error-message">{validationErrors.password}</span>
        )}
      </div>

      {error && (
        <div className="error-alert">
          <p>{error}</p>
        </div>
      )}

      <button
        type="submit"
        disabled={isLoading}
        className="btn-submit"
      >
        {isLoading ? 'Iniciando sesión...' : 'Iniciar Sesión'}
      </button>
    </form>
  );
};

export default LoginForm;
