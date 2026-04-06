import { HttpClient } from "./http-client";
import {
  CustomerTierCreateRequestDto,
  CustomerTierResponseDto,
  CustomerTierUpdateRequestDto,
  PageResponseDto,
  UUID,
} from "./types";

export class CustomerTiersApi {
  private readonly http: HttpClient;

  constructor(http: HttpClient) {
    this.http = http;
  }

  public create(request: CustomerTierCreateRequestDto): Promise<CustomerTierResponseDto> {
    return this.http.post<CustomerTierResponseDto>("/api/v1/customer-tiers", request);
  }

  public list(params?: {
    page?: number;
    size?: number;
    isActive?: boolean;
  }): Promise<PageResponseDto<CustomerTierResponseDto>> {
    return this.http.get<PageResponseDto<CustomerTierResponseDto>>("/api/v1/customer-tiers", {
      page: params?.page ?? 0,
      size: params?.size ?? 20,
      isActive: params?.isActive,
    });
  }

  public getById(tierId: UUID): Promise<CustomerTierResponseDto> {
    return this.http.get<CustomerTierResponseDto>(`/api/v1/customer-tiers/${tierId}`);
  }

  public update(tierId: UUID, request: CustomerTierUpdateRequestDto): Promise<CustomerTierResponseDto> {
    return this.http.put<CustomerTierResponseDto>(`/api/v1/customer-tiers/${tierId}`, request);
  }

  public delete(tierId: UUID): Promise<void> {
    return this.http.delete(`/api/v1/customer-tiers/${tierId}`);
  }

  public activate(tierId: UUID): Promise<CustomerTierResponseDto> {
    return this.http.put<CustomerTierResponseDto>(`/api/v1/customer-tiers/${tierId}/activate`);
  }
}

