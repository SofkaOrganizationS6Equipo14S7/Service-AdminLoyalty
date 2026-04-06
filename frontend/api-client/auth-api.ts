import { HttpClient } from "./http-client";
import { LoginRequestDto, LoginResponseDto, UserResponseDto } from "./types";

export class AuthApi {
  private readonly http: HttpClient;

  constructor(http: HttpClient) {
    this.http = http;
  }

  public login(request: LoginRequestDto): Promise<LoginResponseDto> {
    return this.http.post<LoginResponseDto>("/api/v1/auth/login", request, false);
  }

  public logout(): Promise<void> {
    return this.http.post<void>("/api/v1/auth/logout");
  }

  public me(): Promise<UserResponseDto> {
    return this.http.get<UserResponseDto>("/api/v1/auth/me");
  }
}

