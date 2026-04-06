import { HttpClient } from "./http-client";
import {
  ApiResponseDto,
  ConfigurationCreateRequestDto,
  ConfigurationPatchRequestDto,
  ConfigurationWriteDataDto,
  UUID,
} from "./types";

export class ConfigurationApi {
  private readonly http: HttpClient;

  constructor(http: HttpClient) {
    this.http = http;
  }

  public create(
    request: ConfigurationCreateRequestDto
  ): Promise<ApiResponseDto<ConfigurationWriteDataDto>> {
    return this.http.post<ApiResponseDto<ConfigurationWriteDataDto>>("/api/v1/configurations", request);
  }

  public patch(
    ecommerceId: UUID,
    request: ConfigurationPatchRequestDto
  ): Promise<ApiResponseDto<ConfigurationWriteDataDto>> {
    return this.http.patch<ApiResponseDto<ConfigurationWriteDataDto>>(
      `/api/v1/configurations/${ecommerceId}`,
      request
    );
  }
}

