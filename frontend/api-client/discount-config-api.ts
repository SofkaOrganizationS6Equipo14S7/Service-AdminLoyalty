import { HttpClient } from "./http-client";
import {
  DiscountConfigCreateRequestDto,
  DiscountConfigResponseDto,
  DiscountLimitPriorityRequestDto,
  DiscountLimitPriorityResponseDto,
  UUID,
} from "./types";

export class DiscountConfigApi {
  private readonly http: HttpClient;

  constructor(http: HttpClient) {
    this.http = http;
  }

  public createOrUpdate(request: DiscountConfigCreateRequestDto): Promise<DiscountConfigResponseDto> {
    return this.http.post<DiscountConfigResponseDto>("/api/v1/discount-config", request);
  }

  public getByEcommerce(ecommerceId: UUID): Promise<DiscountConfigResponseDto> {
    return this.http.get<DiscountConfigResponseDto>("/api/v1/discount-config", { ecommerceId });
  }

  public savePriorities(request: DiscountLimitPriorityRequestDto): Promise<DiscountLimitPriorityResponseDto> {
    return this.http.post<DiscountLimitPriorityResponseDto>("/api/v1/discount-priority", request);
  }

  public getPriorities(discountSettingId: UUID): Promise<DiscountLimitPriorityResponseDto> {
    return this.http.get<DiscountLimitPriorityResponseDto>("/api/v1/discount-priority", {
      discountSettingId,
    });
  }
}

