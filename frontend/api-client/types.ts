export type UUID = string;
export type ISODateTime = string;

export interface ApiClientConfig {
  baseUrl: string;
  getToken?: () => string | null;
  onUnauthorized?: () => void;
}

export interface ApiErrorPayload {
  error?: string;
  message?: string;
  timestamp?: string;
  path?: string;
  [key: string]: unknown;
}

export class ApiError extends Error {
  public readonly status: number;
  public readonly payload: ApiErrorPayload | null;

  constructor(status: number, message: string, payload: ApiErrorPayload | null = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.payload = payload;
  }
}

export interface LoginRequestDto {
  username: string;
  password: string;
}

export interface LoginResponseDto {
  token: string;
  tipo: string;
  username: string;
  role: string;
}

export interface UserResponseDto {
  uid: UUID;
  username: string;
  roleId: UUID;
  roleName: string;
  email: string;
  ecommerceId: UUID | null;
  isActive: boolean;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export interface UserCreateRequestDto {
  username: string;
  email: string;
  password: string;
  roleId: UUID;
  ecommerceId?: UUID | null;
}

export interface UserUpdateRequestDto {
  username?: string;
  email?: string;
  password?: string;
  ecommerceId?: UUID;
  active?: boolean;
}

export interface UpdateProfileRequestDto {
  email: string;
}

export interface ChangePasswordRequestDto {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

