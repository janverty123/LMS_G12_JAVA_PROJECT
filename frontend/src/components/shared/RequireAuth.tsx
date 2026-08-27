import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import type { Role } from "@/types";

export function RequireAuth({ role }: { role: Role }) {
  const { user } = useAuth();

  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== role) {
    return <Navigate to={user.role === "TEACHER" ? "/teacher/class-sections" : "/student/class-section"} replace />;
  }
  return <Outlet />;
}
