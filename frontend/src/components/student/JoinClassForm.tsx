import { useState, type FormEvent } from "react";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { classSectionService } from "@/services/classSection.service";
import { getErrorMessage } from "@/services/errors";
import type { ClassEnrollmentRequestResponse } from "@/types";

export function JoinClassForm({
  onSubmitted,
}: {
  onSubmitted: (request: ClassEnrollmentRequestResponse) => void;
}) {
  const [classCode, setClassCode] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const request = await classSectionService.createEnrollmentRequest(
        classCode.trim().toUpperCase(),
      );
      setClassCode("");
      onSubmitted(request);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form
      className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
      onSubmit={submit}
    >
      <h2 className="font-semibold">Join a class section</h2>
      <p className="mt-1 text-sm text-slate-500">
        Ask your adviser for the six-character class code.
      </p>
      {error && (
        <div className="mt-4">
          <ApiAlert message={error} />
        </div>
      )}
      <div className="responsive-form-row mt-4">
        <input
          required
          minLength={6}
          maxLength={6}
          aria-label="Class code"
          className="min-w-0 flex-1 rounded-lg border border-slate-300 px-3 py-2 font-mono uppercase tracking-widest"
          placeholder="ABC123"
          value={classCode}
          onChange={(event) => setClassCode(event.target.value.toUpperCase())}
        />
        <button
          disabled={busy}
          className="rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
        >
          {busy ? "Sending…" : "Request to join"}
        </button>
      </div>
    </form>
  );
}
