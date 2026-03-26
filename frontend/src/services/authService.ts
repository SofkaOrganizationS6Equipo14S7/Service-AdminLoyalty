import axios, { AxiosInstance } from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081';

interface LoginRequest {
  username: string;
  password: string;
}

interface LoginResponse {
  token: string;
  tipo: string;
  username: string;
  role: string;
}

interface UserResponse {
  id: number;
  username: string;
  role: string;
  active: boolean;
}

class AuthService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: `${API_URL}/api/v1`,
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }

  /**
   * Inicia sesión con credenciales
   * @param username - Nombre de usuario
   * @param password - Contraseña
   * @returns Token JWT y datos del usuario
   */
  async login(username: string, password: string): Promise<LoginResponse> {
    try {
      const response = await this.api.post<LoginResponse>(
        '/auth/login',
        { username, password } as LoginRequest
      );
      
      // Guardar token en localStorage
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('user', JSON.stringify({
        username: response.data.username,
        role: response.data.role,
      }));
      
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(
          error.response?.data?.message || 'Error al iniciar sesión'
        );
      }
      throw error;
    }
  }

  /**
   * Cierra sesión del usuario
   */
  async logout(): Promise<void> {
    try {
      const token = localStorage.getItem('token');
      if (token) {
        // Notificar al backend
        await this.api.post('/auth/logout', {}, {
          headers: {
            'Authorization': `Bearer ${token}`,
          },
        });
      }
    } catch (error) {
      // Logout es tolerante: limpia localStorage aunque falle la llamada
      console.warn('Error al notificar logout al backend:', error);
    } finally {
      // Siempre limpiar datos locales
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  }

  /**
   * Obtiene datos del usuario autenticado
   * @param token - Token JWT
   * @returns Datos del usuario
   */
  async getCurrentUser(token: string): Promise<UserResponse> {
    try {
      const response = await this.api.get<UserResponse>(
        '/auth/me',
        {
          headers: {
            'Authorization': `Bearer ${token}`,
          },
        }
      );
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 401) {
        // Token expirado o inválido
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
      throw error;
    }
  }

  /**
   * Obtiene el token del localStorage
   */
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  /**
   * Obtiene los datos del usuario del localStorage
   */
  getUser(): { username: string; role: string } | null {
    const userStr = localStorage.getItem('user');
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  }

  /**
   * Verifica si el usuario está autenticado
   */
  isAuthenticated(): boolean {
    return !!this.getToken();
  }
}

export default new AuthService();
