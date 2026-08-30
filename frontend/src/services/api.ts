import axios, { AxiosError } from "axios";
import type { ApiErrorResponse } from "@/types";

// Same-origin by default. VITE_API_BASE_URL supports deployments where the
// frontend and API have different origins.
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  headers: {
    "Content-Type": "application/json",
  },
});

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
