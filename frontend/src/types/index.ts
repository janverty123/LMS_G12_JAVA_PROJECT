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

// Extended in Phase 2 once auth/login exists.
export type Role = "TEACHER" | "STUDENT";
