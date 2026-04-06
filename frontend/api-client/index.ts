import { ApiKeysApi } from "./api-keys-api";
import { AuthApi } from "./auth-api";
import { ConfigurationApi } from "./configuration-api";
import { CustomerTiersApi } from "./customer-tiers-api";
import { DiscountConfigApi } from "./discount-config-api";
import { EcommerceApi } from "./ecommerce-api";
import { HttpClient } from "./http-client";
import { LogsApi } from "./logs-api";
import { RolesApi } from "./roles-api";
import { RulesApi } from "./rules-api";
import { UsersApi } from "./users-api";
import { ApiClientConfig } from "./types";

export * from "./types";
export { ApiError } from "./types";

export interface ServiceAdminClient {
  auth: AuthApi;
  users: UsersApi;
  ecommerce: EcommerceApi;
  apiKeys: ApiKeysApi;
  configuration: ConfigurationApi;
  discountConfig: DiscountConfigApi;
  customerTiers: CustomerTiersApi;
  rules: RulesApi;
  roles: RolesApi;
  logs: LogsApi;
}

export function createServiceAdminClient(config: ApiClientConfig): ServiceAdminClient {
  const http = new HttpClient(config);
  return {
    auth: new AuthApi(http),
    users: new UsersApi(http),
    ecommerce: new EcommerceApi(http),
    apiKeys: new ApiKeysApi(http),
    configuration: new ConfigurationApi(http),
    discountConfig: new DiscountConfigApi(http),
    customerTiers: new CustomerTiersApi(http),
    rules: new RulesApi(http),
    roles: new RolesApi(http),
    logs: new LogsApi(http),
  };
}
