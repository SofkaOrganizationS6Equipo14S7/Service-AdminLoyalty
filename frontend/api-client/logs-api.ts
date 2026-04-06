import { HttpClient } from "./http-client";
import {
  AuditLogResponseDto,
  DiscountApplicationLogResponseDto,
  PageResponseDto,
  UUID,
} from "./types";

export class LogsApi {
  private readonly http: HttpClient;

  constructor(http: HttpClient) {
    this.http = http;
  }

  public listAuditLogs(params?: {
    entityName?: string;
    ecommerceId?: UUID;
    page?: number;
    size?: number;
  }): Promise<PageResponseDto<AuditLogResponseDto>> {
    return this.http.get<PageResponseDto<AuditLogResponseDto>>("/api/v1/audit-logs", {
      entityName: params?.entityName,
      ecommerceId: params?.ecommerceId,
      page: params?.page ?? 0,
      size: params?.size ?? 50,
    });
  }

  public getAuditLogById(logId: UUID): Promise<AuditLogResponseDto> {
    return this.http.get<AuditLogResponseDto>(`/api/v1/audit-logs/${logId}`);
  }

  public listDiscountLogs(params?: {
    ecommerceId?: UUID;
    externalOrderId?: string;
    page?: number;
    size?: number;
  }): Promise<PageResponseDto<DiscountApplicationLogResponseDto>> {
    return this.http.get<PageResponseDto<DiscountApplicationLogResponseDto>>("/api/v1/discount-logs", {
      ecommerceId: params?.ecommerceId,
      externalOrderId: params?.externalOrderId,
      page: params?.page ?? 0,
      size: params?.size ?? 50,
    });
  }

  public getDiscountLogById(logId: UUID): Promise<DiscountApplicationLogResponseDto> {
    return this.http.get<DiscountApplicationLogResponseDto>(`/api/v1/discount-logs/${logId}`);
  }
}

