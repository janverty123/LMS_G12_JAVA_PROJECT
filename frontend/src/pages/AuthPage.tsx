import { useState, type FormEvent } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { LearningIllustration } from "@/components/shared/LearningIllustration";
import { useAuth } from "@/hooks/useAuth";
import { authService } from "@/services/auth.service";
import { getErrorMessage } from "@/services/errors";
import type { Role } from "@/types";

export function AuthPage({
  mode,
  registrationRole,
}: {
  mode: "login" | "register";
  registrationRole?: Role;
}) {
  const { user, saveSession } = useAuth();
  const navigate = useNavigate();
  const role: Role = registrationRole ?? "TEACHER";
  const [name, setName] = useState("");
  const [lrn, setLrn] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  if (user) {
    return (
      <Navigate
        to={
          user.role === "TEACHER" ? "/teacher/dashboard" : "/student/dashboard"
        }
        replace
      />
    );
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    if (mode === "register" && password !== confirmPassword) {
      setError("Passwords do not match.");
      setBusy(false);
      return;
    }
    try {
      const response =
        mode === "login"
          ? await authService.login({ email, password })
          : role === "TEACHER"
            ? await authService.registerTeacher({ name, email, password })
            : await authService.registerStudent({ name, lrn, email, password });
      saveSession(response);
      navigate(
        response.role === "TEACHER"
          ? "/teacher/dashboard"
          : "/student/dashboard",
      );
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="relative grid min-h-screen bg-[var(--page)] lg:grid-cols-[1.05fr_0.95fr]">
      <Link className="auth-back" to="/" aria-label="Back to home">
        <span aria-hidden="true">←</span> Back
      </Link>
      <div className="relative hidden overflow-hidden border-r border-[var(--border)] bg-[var(--surface)] lg:block">
        <Link className="brand absolute left-10 top-8" to="/">
          Classi<span>fy</span>
        </Link>
        <LearningIllustration className="absolute inset-16 top-28" />
      </div>
      <div className="grid place-items-center px-5 py-10">
        <div className="w-full max-w-md">
          <Link className="brand mb-8 block lg:hidden" to="/">
            Classi<span>fy</span>
          </Link>
          <h1 className="text-4xl font-normal tracking-tight">
            {mode === "login" ? "Welcome to Classify" : "Create Account"}
          </h1>
          <p className="mt-2 text-sm text-[var(--text-muted)]">
            {mode === "login"
              ? "Log in to continue to your dashboard."
              : "Sign-up to get started with your dashboard."}
          </p>

          <form className="mt-6 space-y-4" onSubmit={submit}>
            {error && <ApiAlert message={error} />}
            {mode === "register" && (
              <>
                <label className="block text-sm font-medium">
                  Full Name
                  <input
                    required
                    className="mt-1 w-full rounded-lg border px-3 py-2.5"
                    placeholder="Last Name, First Name, Middle Name"
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                  />
                </label>
                {role === "STUDENT" && (
                  <label className="block text-sm font-medium">
                    LRN (Learner Reference Number)
                    <input
                      required
                      className="mt-1 w-full rounded-lg border px-3 py-2.5"
                      value={lrn}
                      onChange={(event) => setLrn(event.target.value)}
                    />
                  </label>
                )}
              </>
            )}
            <label className="block text-sm font-medium">
              Email address
              <input
                required
                type="email"
                className="mt-1 w-full rounded-lg border px-3 py-2.5"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </label>
            <label className="block text-sm font-medium">
              Password
              <input
                required
                minLength={mode === "register" ? 8 : undefined}
                type={showPassword ? "text" : "password"}
                className="mt-1 w-full rounded-lg border px-3 py-2.5"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </label>
            {mode === "register" && (
              <label className="block text-sm font-medium">
                Confirm Password
                <input
                  required
                  minLength={8}
                  type={showPassword ? "text" : "password"}
                  className="mt-1 w-full rounded-lg border px-3 py-2.5"
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                />
              </label>
            )}
            <label className="flex items-center gap-2 text-xs text-[var(--text-muted)]">
              <input
                type="checkbox"
                checked={showPassword}
                onChange={(event) => setShowPassword(event.target.checked)}
              />
              Show Password
            </label>
            {mode === "login" && (
              <p className="text-xs text-[var(--text-muted)]">
                By logging in, I agree and accept the Terms and agreement.
              </p>
            )}
            <button
              disabled={busy}
              className="button button--primary w-full disabled:opacity-60"
            >
              {busy
                ? "Please wait…"
                : mode === "login"
                  ? "Login"
                  : "Create Account"}
            </button>
          </form>
          {mode === "login" ? (
            <div className="mt-5 text-center text-sm text-[var(--text-muted)]">
              <p>No Account Yet?</p>
              <div className="mt-2">
                <Link
                  className="font-semibold text-[var(--accent-strong)] underline"
                  to="/register"
                >
                  Choose an account type
                </Link>
              </div>
            </div>
          ) : (
            <p className="mt-5 text-center text-sm text-[var(--text-muted)]">
              Already have an account?{" "}
              <Link
                className="font-semibold text-[var(--accent-strong)] underline"
                to="/login"
              >
                Log in
              </Link>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
