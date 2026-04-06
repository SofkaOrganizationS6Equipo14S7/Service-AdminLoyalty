import { AuthApi } from "./auth-api";
import { EcommerceApi } from "./ecommerce-api";
import { HttpClient } from "./http-client";
import { UsersApi } from "./users-api";
import { ApiClientConfig } from "./types";

export * from "./types";
export { ApiError } from "./types";

export interface ServiceAdminClient {
  auth: AuthApi;
  users: UsersApi;
  ecommerce: EcommerceApi;
}

export function createServiceAdminClient(config: ApiClientConfig): ServiceAdminClient {
  const http = new HttpClient(config);
  return {
    auth: new AuthApi(http),
    users: new UsersApi(http),
    ecommerce: new EcommerceApi(http),
  };
}
