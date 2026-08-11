import axios, { AxiosError } from "axios";
import type { ApiErrorResponse } from "@/types";

// Same-origin "/api" — Vite's dev proxy (see vite.config.ts) forwards this to
// the Spring Boot backend, so no absolute URL/env var is needed in dev.
// In production this is served behind the same reverse proxy as the SPA.
export const api = axios.create({
  baseURL: "/api",
  headers: {
    "Content-Type": "application/json",
  },
});

// Phase 2 will populate this from the auth store once login/JWT exists.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("apptitle_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Normalizes Axios errors into the backend's ApiErrorResponse shape so
// calling code can do `catch (err) { err.message }` without re-parsing.
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    const apiError = error.response?.data;
    return Promise.reject(
      apiError ?? {
        timestamp: new Date().toISOString(),
        status: error.response?.status ?? 0,
        error: "Network Error",
        message: error.message || "Unable to reach the server.",
        path: error.config?.url ?? "",
      }
    );
  }
);
