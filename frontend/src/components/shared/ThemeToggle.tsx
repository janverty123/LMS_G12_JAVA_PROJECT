import { useTheme } from "@/hooks/useTheme";

export function ThemeToggle({ compact = false }: { compact?: boolean }) {
  const { theme, toggleTheme } = useTheme();
  const dark = theme === "dark";

  return (
    <button
      type="button"
      className="theme-toggle"
      role="switch"
      aria-checked={dark}
      aria-label={`Switch to ${dark ? "light" : "dark"} mode`}
      onClick={toggleTheme}
    >
      <span className="theme-toggle__track" aria-hidden="true">
        <span className="theme-toggle__thumb">{dark ? "☾" : "☀"}</span>
      </span>
      {!compact && <span>{dark ? "Dark mode" : "Light mode"}</span>}
    </button>
  );
}
