import { createContext } from "react";
import type { AuthResponse } from "@/types";

export interface AuthContextValue {
  user: AuthResponse | null;
  saveSession: (response: AuthResponse) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
