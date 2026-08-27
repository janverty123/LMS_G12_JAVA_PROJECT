import type { ApiErrorResponse } from "@/types";

export function getErrorMessage(error: unknown) {
  if (typeof error === "object" && error !== null && "message" in error) {
    return String((error as ApiErrorResponse).message);
  }
  return "Something went wrong. Please try again.";
}

export function hasStatus(error: unknown, status: number) {
  return (
    typeof error === "object" &&
    error !== null &&
    "status" in error &&
    (error as ApiErrorResponse).status === status
  );
}
