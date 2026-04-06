import { HttpClient } from "./http-client";
import {
  PermissionResponseDto,
  RolePermissionsAssignRequestDto,
  RoleResponseDto,
  RoleWithPermissionsResponseDto,
  UUID,
} from "./types";

export class RolesApi {
  private readonly http: HttpClient;

  constructor(http: HttpClient) {
    this.http = http;
  }

  public listRoles(): Promise<RoleResponseDto[]> {
    return this.http.get<RoleResponseDto[]>("/api/v1/roles");
  }

  public getRole(roleId: UUID): Promise<RoleWithPermissionsResponseDto> {
    return this.http.get<RoleWithPermissionsResponseDto>(`/api/v1/roles/${roleId}`);
  }

  public listPermissions(module?: string): Promise<PermissionResponseDto[]> {
    return this.http.get<PermissionResponseDto[]>("/api/v1/permissions", { module });
  }

  public assignPermissions(
    roleId: UUID,
    request: RolePermissionsAssignRequestDto
  ): Promise<RoleWithPermissionsResponseDto> {
    return this.http.post<RoleWithPermissionsResponseDto>(`/api/v1/roles/${roleId}/permissions`, request);
  }
}

