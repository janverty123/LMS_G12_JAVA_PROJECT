import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";

function navClass({ isActive }: { isActive: boolean }) {
  return `rounded-lg px-3 py-2 text-sm font-medium transition ${
    isActive
      ? "bg-amber-100 text-amber-900"
      : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
  }`;
}

export function AppShell() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-5 py-4">
          <div>
            <p className="font-semibold tracking-tight">Classify</p>
            <p className="text-xs text-slate-500">{user?.name} · {user?.role.toLowerCase()}</p>
          </div>
          <nav className="flex w-full items-center gap-1 overflow-x-auto pb-1 sm:w-auto sm:pb-0" aria-label="Primary navigation">
            {user?.role === "TEACHER" ? (
              <>
                <NavLink className={navClass} to="/teacher/class-sections">Class sections</NavLink>
                <NavLink className={navClass} to="/teacher/subjects">Subjects</NavLink>
                <NavLink className={navClass} to="/teacher/notifications">Notifications</NavLink>
              </>
            ) : (
              <>
                <NavLink className={navClass} to="/student/class-section">My class</NavLink>
                <NavLink className={navClass} to="/student/subjects">My subjects</NavLink>
                <NavLink className={navClass} to="/student/announcements">Announcements</NavLink>
                <NavLink className={navClass} to="/student/notifications">Notifications</NavLink>
              </>
            )}
            <button
              type="button"
              onClick={handleLogout}
              className="ml-2 rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100"
            >
              Sign out
            </button>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6 sm:px-5 sm:py-8">
        <Outlet />
      </main>
    </div>
  );
}
