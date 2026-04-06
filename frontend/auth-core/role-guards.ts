import { UserResponseDto } from "../api-client";

export type AppRole = "SUPER_ADMIN" | "STORE_ADMIN" | "STORE_USER" | "ADMIN" | string;

export function userRole(user: UserResponseDto | null): AppRole | null {
  if (!user) {
    return null;
  }
  return user.roleName ?? null;
}

export function hasAnyRole(user: UserResponseDto | null, roles: AppRole[]): boolean {
  const role = userRole(user);
  if (!role) {
    return false;
  }
  return roles.includes(role);
}

export function isAuthenticated(user: UserResponseDto | null): boolean {
  return !!user;
}

