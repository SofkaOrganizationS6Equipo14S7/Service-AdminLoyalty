import { createServiceAdminClient, LoginRequestDto, UserResponseDto } from "../api-client";
import { SessionStoragePort } from "./storage";

export interface SessionManagerConfig {
  baseUrl: string;
  storage: SessionStoragePort;
}

export interface SessionState {
  token: string | null;
  user: UserResponseDto | null;
  isBootstrapped: boolean;
}

type SessionListener = (state: SessionState) => void;

export class SessionManager {
  private readonly storage: SessionStoragePort;
  private readonly listeners = new Set<SessionListener>();

  private state: SessionState = {
    token: null,
    user: null,
    isBootstrapped: false,
  };

  private readonly client;

  constructor(config: SessionManagerConfig) {
    this.storage = config.storage;
    this.state.token = this.storage.getToken();

    this.client = createServiceAdminClient({
      baseUrl: config.baseUrl,
      getToken: () => this.state.token,
      onUnauthorized: () => this.clearSession(),
    });
  }

  public getState(): SessionState {
    return { ...this.state };
  }

  public subscribe(listener: SessionListener): () => void {
    this.listeners.add(listener);
    listener(this.getState());
    return () => {
      this.listeners.delete(listener);
    };
  }

  public async bootstrap(): Promise<SessionState> {
    if (!this.state.token) {
      this.state = { ...this.state, isBootstrapped: true };
      this.emit();
      return this.getState();
    }

    try {
      const user = await this.client.auth.me();
      this.state = {
        token: this.state.token,
        user,
        isBootstrapped: true,
      };
    } catch {
      this.clearSession();
      this.state = { ...this.state, isBootstrapped: true };
    }

    this.emit();
    return this.getState();
  }

  public async login(payload: LoginRequestDto): Promise<SessionState> {
    const login = await this.client.auth.login(payload);
    this.storage.setToken(login.token);
    this.state = {
      ...this.state,
      token: login.token,
    };

    const user = await this.client.auth.me();
    this.state = {
      token: login.token,
      user,
      isBootstrapped: true,
    };

    this.emit();
    return this.getState();
  }

  public async logout(): Promise<SessionState> {
    try {
      if (this.state.token) {
        await this.client.auth.logout();
      }
    } finally {
      this.clearSession();
      this.emit();
    }

    return this.getState();
  }

  public async refreshCurrentUser(): Promise<UserResponseDto | null> {
    if (!this.state.token) {
      return null;
    }

    const user = await this.client.auth.me();
    this.state = {
      ...this.state,
      user,
    };
    this.emit();
    return user;
  }

  public setToken(token: string): void {
    this.storage.setToken(token);
    this.state = {
      ...this.state,
      token,
    };
    this.emit();
  }

  public getApiClient() {
    return this.client;
  }

  private clearSession(): void {
    this.storage.clearToken();
    this.state = {
      ...this.state,
      token: null,
      user: null,
    };
  }

  private emit(): void {
    const snapshot = this.getState();
    this.listeners.forEach((listener) => listener(snapshot));
  }
}
