import { HttpClient } from "./http-client";
import {
  EcommerceCreateRequestDto,
  EcommerceResponseDto,
  EcommerceUpdateStatusRequestDto,
  PageResponseDto,
  UUID,
} from "./types";

export class EcommerceApi {
  private readonly http: HttpClient;

  constructor(http: HttpClient) {
    this.http = http;
  }

  public create(request: EcommerceCreateRequestDto): Promise<EcommerceResponseDto> {
    return this.http.post<EcommerceResponseDto>("/api/v1/ecommerces", request);
  }

  public list(params?: {
    status?: "ACTIVE" | "INACTIVE";
    page?: number;
    size?: number;
  }): Promise<PageResponseDto<EcommerceResponseDto>> {
    return this.http.get<PageResponseDto<EcommerceResponseDto>>("/api/v1/ecommerces", {
      status: params?.status,
      page: params?.page ?? 0,
      size: params?.size ?? 50,
    });
  }

  public getByUid(uid: UUID): Promise<EcommerceResponseDto> {
    return this.http.get<EcommerceResponseDto>(`/api/v1/ecommerces/${uid}`);
  }

  public updateStatus(uid: UUID, request: EcommerceUpdateStatusRequestDto): Promise<EcommerceResponseDto> {
    return this.http.put<EcommerceResponseDto>(`/api/v1/ecommerces/${uid}/status`, request);
  }
}

