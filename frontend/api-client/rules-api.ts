import { HttpClient } from "./http-client";
import {
  AssignCustomerTiersRequestDto,
  DiscountPriorityDto,
  DiscountTypeDto,
  PageResponseDto,
  RuleAttributeMetadataDto,
  RuleCreateRequestDto,
  RuleCustomerTierDto,
  RuleResponseDto,
  RuleResponseWithTiersDto,
  UUID,
} from "./types";

export class RulesApi {
  private readonly http: HttpClient;

  constructor(http: HttpClient) {
    this.http = http;
  }

  public listDiscountTypes(): Promise<DiscountTypeDto[]> {
    return this.http.get<DiscountTypeDto[]>("/api/v1/rules/discount-types");
  }

  public listAttributes(discountTypeId: UUID): Promise<RuleAttributeMetadataDto[]> {
    return this.http.get<RuleAttributeMetadataDto[]>("/api/v1/rules/attributes", { discountTypeId });
  }

  public listDiscountPriorities(discountTypeId: UUID): Promise<DiscountPriorityDto[]> {
    return this.http.get<DiscountPriorityDto[]>("/api/v1/rules/discount-priorities", { discountTypeId });
  }

  public create(request: RuleCreateRequestDto): Promise<RuleResponseDto> {
    return this.http.post<RuleResponseDto>("/api/v1/rules", request);
  }

  public list(params?: { page?: number; size?: number; active?: boolean }): Promise<PageResponseDto<RuleResponseDto>> {
    return this.http.get<PageResponseDto<RuleResponseDto>>("/api/v1/rules", {
      page: params?.page ?? 0,
      size: params?.size ?? 20,
      active: params?.active,
    });
  }

  public getById(ruleId: UUID): Promise<RuleResponseWithTiersDto> {
    return this.http.get<RuleResponseWithTiersDto>(`/api/v1/rules/${ruleId}`);
  }

  public update(ruleId: UUID, request: RuleCreateRequestDto): Promise<RuleResponseDto> {
    return this.http.put<RuleResponseDto>(`/api/v1/rules/${ruleId}`, request);
  }

  public delete(ruleId: UUID): Promise<void> {
    return this.http.delete(`/api/v1/rules/${ruleId}`);
  }

  public assignTiers(ruleId: UUID, request: AssignCustomerTiersRequestDto): Promise<RuleResponseWithTiersDto> {
    return this.http.post<RuleResponseWithTiersDto>(`/api/v1/rules/${ruleId}/tiers`, request);
  }

  public listAssignedTiers(ruleId: UUID): Promise<RuleCustomerTierDto[]> {
    return this.http.get<RuleCustomerTierDto[]>(`/api/v1/rules/${ruleId}/tiers`);
  }

  public deleteTierFromRule(ruleId: UUID, tierId: UUID): Promise<void> {
    return this.http.delete(`/api/v1/rules/${ruleId}/tiers/${tierId}`);
  }
}

