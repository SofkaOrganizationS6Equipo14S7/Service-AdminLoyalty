export interface SessionStoragePort {
  getToken(): string | null;
  setToken(token: string): void;
  clearToken(): void;
}

export class MemorySessionStorage implements SessionStoragePort {
  private token: string | null = null;

  getToken(): string | null {
    return this.token;
  }

  setToken(token: string): void {
    this.token = token;
  }

  clearToken(): void {
    this.token = null;
  }
}

export class LocalStorageSessionStorage implements SessionStoragePort {
  private readonly key: string;
  private readonly storage: Storage;

  constructor(key = "loyalty_admin_jwt", storage: Storage = globalThis.localStorage) {
    this.key = key;
    this.storage = storage;
  }

  getToken(): string | null {
    return this.storage.getItem(this.key);
  }

  setToken(token: string): void {
    this.storage.setItem(this.key, token);
  }

  clearToken(): void {
    this.storage.removeItem(this.key);
  }
}

