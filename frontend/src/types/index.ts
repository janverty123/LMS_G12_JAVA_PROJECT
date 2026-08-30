// Mirrors backend's com.apptitle.common.dto.ApiErrorResponse.
// Every failed API call resolves to this shape (see services/api.ts interceptor).
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}

export interface HealthResponse {
  status: string;
  service: string;
  timestamp: string;
}

export * from "./auth";
export * from "./activity";
export * from "./classSection";
export * from "./grade";
export * from "./material";
export * from "./progress";
export * from "./notification";
export * from "./subject";
