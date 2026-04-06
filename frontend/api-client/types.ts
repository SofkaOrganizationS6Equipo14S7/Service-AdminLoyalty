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

export interface PageResponseDto<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface EcommerceResponseDto {
  uid: UUID;
  name: string;
  slug: string;
  status: "ACTIVE" | "INACTIVE" | string;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export interface EcommerceCreateRequestDto {
  name: string;
  slug: string;
}

export interface EcommerceUpdateStatusRequestDto {
  status: "ACTIVE" | "INACTIVE";
}

export interface ApiKeyCreatedResponseDto {
  uid: UUID;
  key: string;
  expiresAt: ISODateTime;
  ecommerceId: UUID;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export interface ApiKeyListResponseDto {
  uid: UUID;
  maskedKey: string;
  expiresAt: ISODateTime;
  isActive: boolean;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export type RoundingRule = "HALF_UP" | "DOWN" | "UP";
export type CapType = "PERCENTAGE";
export type CapAppliesTo = "SUBTOTAL" | "TOTAL" | "BEFORE_TAX" | "AFTER_TAX";

export interface ConfigurationCapRequestDto {
  type: CapType;
  value: number;
  appliesTo: CapAppliesTo;
}

export interface ConfigurationPriorityRequestDto {
  type: string;
  order: number;
}

export interface ConfigurationCreateRequestDto {
  ecommerceId: UUID;
  currency: string;
  roundingRule: RoundingRule;
  cap: ConfigurationCapRequestDto;
  priority: ConfigurationPriorityRequestDto[];
}

export interface ConfigurationPatchRequestDto {
  currency?: string;
  roundingRule?: RoundingRule;
  cap?: ConfigurationCapRequestDto;
  priority?: ConfigurationPriorityRequestDto[];
}

export interface ConfigurationWriteDataDto {
  configId: UUID;
  version: number;
}

export interface ApiResponseDto<T> {
  success: boolean;
  data: T;
}

export interface DiscountConfigCreateRequestDto {
  ecommerceId: UUID;
  maxDiscountCap: number;
  currencyCode: string;
  allowStacking?: boolean;
  roundingRule?: string;
}

export interface DiscountConfigResponseDto {
  uid: UUID;
  ecommerceId: UUID;
  maxDiscountCap: number;
  currencyCode: string;
  allowStacking: boolean;
  roundingRule: string;
  isActive: boolean;
  version: number;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export interface DiscountLimitPriorityEntryRequestDto {
  discountTypeId: UUID;
  priorityLevel: number;
}

export interface DiscountLimitPriorityRequestDto {
  discountSettingId: UUID;
  priorities: DiscountLimitPriorityEntryRequestDto[];
}

export interface DiscountLimitPriorityEntryResponseDto {
  discountTypeId: UUID;
  priorityLevel: number;
  createdAt: ISODateTime;
}

export interface DiscountLimitPriorityResponseDto {
  uid: UUID;
  discountSettingId: UUID;
  priorities: DiscountLimitPriorityEntryResponseDto[];
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}
