import { useState, type ReactNode } from "react";
import { AuthContext } from "./auth-context";
import type { AuthResponse } from "@/types";

const TOKEN_KEY = "apptitle_token";
const USER_KEY = "apptitle_user";

function loadStoredUser(): AuthResponse | null {
  const token = localStorage.getItem(TOKEN_KEY);
  const storedUser = localStorage.getItem(USER_KEY);
  if (!token || !storedUser) return null;

  try {
    return JSON.parse(storedUser) as AuthResponse;
  } catch {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(loadStoredUser);

  const saveSession = (response: AuthResponse) => {
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(response));
    setUser(response);
  };

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, saveSession, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
