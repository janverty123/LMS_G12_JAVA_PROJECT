import { useCallback, useEffect, useState, type FormEvent } from "react";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { EmptyState } from "@/components/shared/EmptyState";
import { LoadingState } from "@/components/shared/LoadingState";
import { SubjectLinkRequestList } from "@/components/teacher/SubjectLinkRequestList";
import { getErrorMessage } from "@/services/errors";
import { subjectService } from "@/services/subject.service";
import type { ClassSubjectLinkResponse, SubjectResponse } from "@/types";

export function TeacherSubjectsPage() {
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [selected, setSelected] = useState<SubjectResponse>();
  const [editing, setEditing] = useState<SubjectResponse>();
  const [deleteCandidate, setDeleteCandidate] = useState<SubjectResponse | null>(null);
  const [requests, setRequests] = useState<ClassSubjectLinkResponse[]>([]);
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const loadSubjects = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setSubjects(await subjectService.listTeacherSubjects());
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  const loadRequests = useCallback(async () => {
    if (!selected) {
      setRequests([]);
      return;
    }
    try {
      setRequests(await subjectService.listPendingLinks(selected.id));
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  }, [selected]);

  useEffect(() => { void loadSubjects(); }, [loadSubjects]);
  useEffect(() => { void loadRequests(); }, [loadRequests]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const saved = editing
        ? await subjectService.updateSubject(editing.id, name)
        : await subjectService.createSubject(name);
      setSubjects((current) => editing
        ? current.map((subject) => subject.id === saved.id ? saved : subject)
        : [saved, ...current]);
      setSelected(saved);
      setEditing(undefined);
      setName("");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  const startEdit = (subject: SubjectResponse) => {
    setEditing(subject);
    setName(subject.name);
  };

  const deleteSubject = async () => {
    if (!deleteCandidate) return;
    setBusy(true);
    setError("");
    try {
      await subjectService.deleteSubject(deleteCandidate.id);
      setSubjects((current) => current.filter((subject) => subject.id !== deleteCandidate.id));
      if (selected?.id === deleteCandidate.id) setSelected(undefined);
      if (editing?.id === deleteCandidate.id) {
        setEditing(undefined);
        setName("");
      }
      setDeleteCandidate(null);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  const decideLink = async (id: string, decision: "approve" | "decline") => {
    setError("");
    try {
      if (decision === "approve") await subjectService.approveLink(id);
      else await subjectService.declineLink(id);
      await loadRequests();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  };

  return (
    <div>
      <h1 className="text-3xl font-semibold tracking-tight">Subjects</h1>
      <p className="mt-1 text-slate-500">Create subjects and review requests from class advisers.</p>
      {error && <div className="mt-5"><ApiAlert message={error} /></div>}

      <form className="mt-6 flex max-w-xl flex-wrap gap-3 rounded-xl border border-slate-200 bg-white p-5 shadow-sm" onSubmit={submit}>
        <label className="min-w-64 flex-1 text-sm font-medium text-slate-700">
          {editing ? "Subject name" : "New subject name"}
          <input required className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" placeholder="General Mathematics" value={name} onChange={(event) => setName(event.target.value)} />
        </label>
        <div className="flex items-end gap-2">
          <button disabled={busy} className="rounded-lg bg-amber-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">{editing ? "Save" : "Create"}</button>
          {editing && <button type="button" onClick={() => { setEditing(undefined); setName(""); }} className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium">Cancel</button>}
        </div>
      </form>

      <div className="mt-7">
        {loading ? <LoadingState label="Loading subjects…" /> : !subjects.length ? (
          <EmptyState title="No subjects yet" detail="Create a subject to generate its seven-character code." />
        ) : (
          <div className="grid gap-3 md:grid-cols-2">
            {subjects.map((subject) => (
              <article key={subject.id} className={`rounded-xl border bg-white p-5 shadow-sm ${selected?.id === subject.id ? "border-amber-500 ring-1 ring-amber-500" : "border-slate-200"}`}>
                <button className="w-full text-left" type="button" onClick={() => setSelected(subject)}>
                  <h2 className="font-semibold">{subject.name}</h2>
                  <p className="mt-2 font-mono text-sm font-bold tracking-widest text-amber-800">{subject.subjectCode}</p>
                </button>
                <div className="mt-4 flex gap-3 border-t border-slate-100 pt-3">
                  <button type="button" className="text-sm font-medium text-slate-600" onClick={() => startEdit(subject)}>Edit</button>
                  <button type="button" className="text-sm font-medium text-rose-600" onClick={() => setDeleteCandidate(subject)}>Delete</button>
                </div>
              </article>
            ))}
          </div>
        )}
      </div>

      {selected && (
        <section className="mt-8 border-t border-slate-200 pt-8">
          <h2 className="text-xl font-semibold">Pending links for {selected.name}</h2>
          <div className="mt-3">
            <SubjectLinkRequestList requests={requests} onApprove={(id) => decideLink(id, "approve")} onDecline={(id) => decideLink(id, "decline")} />
          </div>
        </section>
      )}

      <ConfirmDialog
        open={Boolean(deleteCandidate)}
        title="Delete subject?"
        message={`This will delete ${deleteCandidate?.name ?? "the subject"} and all of its class links.`}
        confirmLabel="Delete subject"
        busy={busy}
        onCancel={() => setDeleteCandidate(null)}
        onConfirm={deleteSubject}
      />
    </div>
  );
}
