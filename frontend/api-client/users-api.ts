import { HttpClient } from "./http-client";
import {
  ChangePasswordRequestDto,
  LoginResponseDto,
  UUID,
  UpdateProfileRequestDto,
  UserCreateRequestDto,
  UserResponseDto,
  UserUpdateRequestDto,
} from "./types";

export class UsersApi {
  private readonly http: HttpClient;

  constructor(http: HttpClient) {
    this.http = http;
  }

  public create(request: UserCreateRequestDto): Promise<UserResponseDto> {
    return this.http.post<UserResponseDto>("/api/v1/users", request);
  }

  public list(ecommerceId?: UUID): Promise<UserResponseDto[]> {
    return this.http.get<UserResponseDto[]>("/api/v1/users", {
      ecommerceId,
    });
  }

  public getByUid(uid: UUID): Promise<UserResponseDto> {
    return this.http.get<UserResponseDto>(`/api/v1/users/${uid}`);
  }

  public update(uid: UUID, request: UserUpdateRequestDto): Promise<UserResponseDto> {
    return this.http.put<UserResponseDto>(`/api/v1/users/${uid}`, request);
  }

  public delete(uid: UUID): Promise<void> {
    return this.http.delete(`/api/v1/users/${uid}`);
  }

  public updateProfile(request: UpdateProfileRequestDto): Promise<UserResponseDto> {
    return this.http.put<UserResponseDto>("/api/v1/users/me", request);
  }

  public changePassword(request: ChangePasswordRequestDto): Promise<LoginResponseDto> {
    return this.http.put<LoginResponseDto>("/api/v1/users/me/password", request);
  }
}

