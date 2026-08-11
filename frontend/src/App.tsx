import { useEffect, useState } from "react";
import { api } from "@/services/api";
import type { HealthResponse, ApiErrorResponse } from "@/types";

type ConnectionState =
  | { phase: "checking" }
  | { phase: "connected"; data: HealthResponse }
  | { phase: "error"; error: ApiErrorResponse };

export default function App() {
  const [state, setState] = useState<ConnectionState>({ phase: "checking" });

  useEffect(() => {
    api
      .get<HealthResponse>("/health")
      .then((res) => setState({ phase: "connected", data: res.data }))
      .catch((err: ApiErrorResponse) => setState({ phase: "error", error: err }));
  }, []);

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 flex items-center justify-center px-6">
      <div className="max-w-lg w-full">
        <div className="mb-8">
          <p className="text-sm font-medium tracking-wide text-amber-700 uppercase">
            Phase 1 — Foundation
          </p>
          <h1 className="mt-1 text-3xl font-semibold tracking-tight">
            APPTITLE
          </h1>
          <p className="mt-2 text-slate-600">
            Learning Management with Student Progress Monitoring System
          </p>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="text-sm font-semibold text-slate-700 mb-3">
            System connectivity
          </h2>

          {state.phase === "checking" && (
            <StatusRow label="Backend API" tone="pending" detail="Checking…" />
          )}

          {state.phase === "connected" && (
            <>
              <StatusRow
                label="Backend API"
                tone="ok"
                detail={`Reachable at /api — ${state.data.service}`}
              />
              <p className="mt-3 text-xs text-slate-400">
                Last checked: {new Date(state.data.timestamp).toLocaleString()}
              </p>
            </>
          )}

          {state.phase === "error" && (
            <>
              <StatusRow
                label="Backend API"
                tone="error"
                detail={state.error.message}
              />
              <p className="mt-3 text-xs text-slate-400">
                Make sure the Spring Boot backend is running on port 8080 and
                Postgres is up (<code>docker compose up -d</code>).
              </p>
            </>
          )}
        </div>

        <p className="mt-6 text-xs text-slate-400">
          Auth, sections, and everything else arrive in the phases that follow.
        </p>
      </div>
    </div>
  );
}

function StatusRow({
  label,
  tone,
  detail,
}: {
  label: string;
  tone: "ok" | "error" | "pending";
  detail: string;
}) {
  const dotColor =
    tone === "ok" ? "bg-emerald-500" : tone === "error" ? "bg-rose-500" : "bg-amber-400";

  return (
    <div className="flex items-start gap-3">
      <span className={`mt-1.5 h-2 w-2 rounded-full ${dotColor}`} />
      <div>
        <p className="text-sm font-medium text-slate-800">{label}</p>
        <p className="text-sm text-slate-500">{detail}</p>
      </div>
    </div>
  );
}
