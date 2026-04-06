import { HttpClient } from "./http-client";
import { ApiKeyCreatedResponseDto, ApiKeyListResponseDto, UUID } from "./types";

export class ApiKeysApi {
  private readonly http: HttpClient;

  constructor(http: HttpClient) {
    this.http = http;
  }

  public create(ecommerceId: UUID): Promise<ApiKeyCreatedResponseDto> {
    return this.http.post<ApiKeyCreatedResponseDto>(`/api/v1/ecommerces/${ecommerceId}/api-keys`, {});
  }

  public list(ecommerceId: UUID): Promise<ApiKeyListResponseDto[]> {
    return this.http.get<ApiKeyListResponseDto[]>(`/api/v1/ecommerces/${ecommerceId}/api-keys`);
  }

  public delete(ecommerceId: UUID, keyId: UUID): Promise<void> {
    return this.http.delete(`/api/v1/ecommerces/${ecommerceId}/api-keys/${keyId}`);
  }
}

