import { useEffect, useRef, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { EditProfileDialog } from "./EditProfileDialog";
import { ThemeToggle } from "./ThemeToggle";
import { useAuth } from "@/hooks/useAuth";

function navClass({ isActive }: { isActive: boolean }) {
  return `app-nav__link${isActive ? " app-nav__link--active" : ""}`;
}

export function AppShell() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [editingProfile, setEditingProfile] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const close = (event: MouseEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setMenuOpen(false);
    };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, []);

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const initial = user?.name.trim().charAt(0).toUpperCase() || "U";

  return (
    <div className="app-layout">
      <aside className="app-sidebar">
        <NavLink className="brand app-sidebar__brand" to={user?.role === "TEACHER" ? "/teacher/dashboard" : "/student/dashboard"}>
          Classi<span>fy</span>
        </NavLink>
        <nav className="app-nav" aria-label="Primary navigation">
          {user?.role === "TEACHER" ? (
            <>
              <NavLink className={navClass} to="/teacher/dashboard">Dashboard</NavLink>
              <NavLink className={navClass} to="/teacher/class-sections">Classes</NavLink>
              <NavLink className={navClass} to="/teacher/subjects">Subjects</NavLink>
              <NavLink className={navClass} to="/teacher/announcements">Announcements</NavLink>
              <NavLink className={navClass} to="/teacher/notifications">Notifications</NavLink>
            </>
          ) : (
            <>
              <NavLink className={navClass} to="/student/dashboard">Dashboard</NavLink>
              <NavLink className={navClass} to="/student/class-section">My Class</NavLink>
              <NavLink className={navClass} to="/student/subjects">My Subjects</NavLink>
              <NavLink className={navClass} to="/student/announcements">Announcements</NavLink>
              <NavLink className={navClass} to="/student/notifications">Notifications</NavLink>
            </>
          )}
        </nav>
      </aside>

      {editingProfile && <EditProfileDialog onClose={() => setEditingProfile(false)} />}
      <div className="app-stage">
        <header className="app-header">
          <div className="app-header__mobile-brand brand">Classi<span>fy</span></div>
          <div ref={menuRef} className="profile-menu">
            <button type="button" className="profile-menu__trigger" aria-expanded={menuOpen} aria-haspopup="menu" onClick={() => setMenuOpen((open) => !open)}>
              <span className="profile-menu__copy"><strong>{user?.name}</strong><small>{user?.role === "TEACHER" ? "Teacher" : "Student"}</small></span>
              <span className="profile-avatar" aria-hidden="true">{user?.profilePicture ? <img src={user.profilePicture} alt="" className="h-full w-full rounded-full object-cover" /> : initial}</span>
            </button>
            {menuOpen && (
              <div className="profile-menu__dropdown" role="menu">
                <p><strong>{user?.name}</strong><span>{user?.email}</span></p>
                <button type="button" role="menuitem" className="profile-menu__signout" onClick={() => { setMenuOpen(false); setEditingProfile(true); }}>Edit profile</button>
                <ThemeToggle />
                <button type="button" className="profile-menu__signout" onClick={handleLogout}>Sign out</button>
              </div>
            )}
          </div>
        </header>
        <main className="app-content"><Outlet /></main>
      </div>
    </div>
  );
}
