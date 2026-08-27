import { useState, type FormEvent } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { useAuth } from "@/hooks/useAuth";
import { authService } from "@/services/auth.service";
import { getErrorMessage } from "@/services/errors";
import type { Role } from "@/types";

export function AuthPage({ mode }: { mode: "login" | "register" }) {
  const { user, saveSession } = useAuth();
  const navigate = useNavigate();
  const [role, setRole] = useState<Role>("TEACHER");
  const [name, setName] = useState("");
  const [lrn, setLrn] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  if (user) {
    return <Navigate to={user.role === "TEACHER" ? "/teacher/class-sections" : "/student/class-section"} replace />;
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const response = mode === "login"
        ? await authService.login({ email, password })
        : role === "TEACHER"
          ? await authService.registerTeacher({ name, email, password })
          : await authService.registerStudent({ name, lrn, email, password });
      saveSession(response);
      navigate(response.role === "TEACHER" ? "/teacher/class-sections" : "/student/class-section");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="grid min-h-screen place-items-center bg-slate-950 px-5 py-10">
      <div className="w-full max-w-md rounded-2xl bg-white p-7 shadow-2xl">
        <p className="text-sm font-semibold uppercase tracking-wider text-amber-700">APPTITLE</p>
        <h1 className="mt-2 text-2xl font-semibold">{mode === "login" ? "Welcome back" : "Create an account"}</h1>
        <p className="mt-1 text-sm text-slate-500">Learning management and progress monitoring</p>

        <form className="mt-6 space-y-4" onSubmit={submit}>
          {error && <ApiAlert message={error} />}
          {mode === "register" && (
            <>
              <label className="block text-sm font-medium text-slate-700">
                Account type
                <select className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" value={role} onChange={(event) => setRole(event.target.value as Role)}>
                  <option value="TEACHER">Teacher</option>
                  <option value="STUDENT">Student</option>
                </select>
              </label>
              <label className="block text-sm font-medium text-slate-700">
                Full name
                <input required className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" value={name} onChange={(event) => setName(event.target.value)} />
              </label>
              {role === "STUDENT" && (
                <label className="block text-sm font-medium text-slate-700">
                  Learner reference number
                  <input required className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" value={lrn} onChange={(event) => setLrn(event.target.value)} />
                </label>
              )}
            </>
          )}
          <label className="block text-sm font-medium text-slate-700">
            Email
            <input required type="email" className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" value={email} onChange={(event) => setEmail(event.target.value)} />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            Password
            <input required minLength={mode === "register" ? 8 : undefined} type="password" className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" value={password} onChange={(event) => setPassword(event.target.value)} />
          </label>
          <button disabled={busy} className="w-full rounded-lg bg-amber-600 px-4 py-2.5 font-semibold text-white hover:bg-amber-700 disabled:opacity-60">
            {busy ? "Please wait…" : mode === "login" ? "Sign in" : "Register"}
          </button>
        </form>
        <p className="mt-5 text-center text-sm text-slate-500">
          {mode === "login" ? "Need an account?" : "Already registered?"}{" "}
          <Link className="font-semibold text-amber-700" to={mode === "login" ? "/register" : "/login"}>
            {mode === "login" ? "Register" : "Sign in"}
          </Link>
        </p>
      </div>
    </div>
  );
}
