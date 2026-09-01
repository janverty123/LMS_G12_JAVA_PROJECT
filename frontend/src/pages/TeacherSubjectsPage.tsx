import { useCallback, useEffect, useState, type FormEvent } from "react";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { EmptyState } from "@/components/shared/EmptyState";
import { LoadingState } from "@/components/shared/LoadingState";
import { SubjectLinkRequestList } from "@/components/teacher/SubjectLinkRequestList";
import { TeacherMaterialsPanel } from "@/components/teacher/TeacherMaterialsPanel";
import { ClassSectionActivitiesTab } from "@/components/teacher/ClassSectionActivitiesTab";
import { ClassSectionGradesTab } from "@/components/teacher/ClassSectionGradesTab";
import { ClassSectionProgressTab } from "@/components/teacher/ClassSectionProgressTab";
import { getErrorMessage } from "@/services/errors";
import { subjectService } from "@/services/subject.service";
import type { ClassSubjectLinkResponse, SubjectResponse } from "@/types";

export function TeacherSubjectsPage() {
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [selected, setSelected] = useState<SubjectResponse>();
  const [editing, setEditing] = useState<SubjectResponse>();
  const [deleteCandidate, setDeleteCandidate] = useState<SubjectResponse | null>(null);
  const [requests, setRequests] = useState<ClassSubjectLinkResponse[]>([]);
  const [activeTab, setActiveTab] = useState<"materials" | "requirements" | "grades" | "progress" | "requests">("materials");
  const [classSectionId, setClassSectionId] = useState("");
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
      const links = await subjectService.listLinksForSubject(selected.id);
      setRequests(links);
      const approved = links.filter((link) => link.status === "APPROVED");
      setClassSectionId((current) => approved.some((link) => link.classSectionId === current) ? current : approved[0]?.classSectionId ?? "");
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
      <div className="page-heading"><h1>{selected?.name ?? "Subjects"}</h1><p>{selected ? selected.subjectTeacherName : "Create subjects and review requests from class advisers."}</p></div>
      {error && <div className="mt-5"><ApiAlert message={error} /></div>}

      {!selected && <form className="mt-6 flex max-w-xl flex-wrap gap-3 rounded-xl border border-slate-200 bg-white p-5 shadow-sm" onSubmit={submit}>
        <label className="w-full min-w-0 flex-1 text-sm font-medium text-slate-700 sm:min-w-64">
          {editing ? "Subject name" : "New subject name"}
          <input required className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" placeholder="General Mathematics" value={name} onChange={(event) => setName(event.target.value)} />
        </label>
        <div className="flex items-end gap-2">
          <button disabled={busy} className="rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">{editing ? "Save" : "Create"}</button>
          {editing && <button type="button" onClick={() => { setEditing(undefined); setName(""); }} className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium">Cancel</button>}
        </div>
      </form>}

      <div className={selected ? "mt-7" : "mt-7"}>
        {loading ? <LoadingState label="Loading subjects…" /> : !subjects.length ? (
          <EmptyState title="No subjects yet" detail="Create a subject to generate its seven-character code." />
        ) : (
          <div className={`grid gap-3 md:grid-cols-2 ${selected ? "hidden" : ""}`}>
            {subjects.map((subject) => (
              <article key={subject.id} className={`rounded-xl border bg-white p-5 shadow-sm ${selected?.id === subject.id ? "border-[var(--accent)] ring-1 ring-[var(--accent)]" : "border-slate-200"}`}>
                <button className="w-full text-left" type="button" onClick={() => setSelected(subject)}>
                  <h2 className="font-semibold">{subject.name}</h2>
                  <p className="mt-2 font-mono text-sm font-bold tracking-widest text-[var(--accent-strong)]">{subject.subjectCode}</p>
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

      {selected && <SubjectWorkspace
        subject={selected}
        links={requests}
        activeTab={activeTab}
        classSectionId={classSectionId}
        onTab={setActiveTab}
        onClassSection={setClassSectionId}
        onApprove={(id) => decideLink(id, "approve")}
        onDecline={(id) => decideLink(id, "decline")}
        onBack={() => { setSelected(undefined); setActiveTab("materials"); }}
        onEdit={() => { startEdit(selected); setSelected(undefined); }}
      />}

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

function SubjectWorkspace({
  subject,
  links,
  activeTab,
  classSectionId,
  onTab,
  onClassSection,
  onApprove,
  onDecline,
  onBack,
  onEdit,
}: {
  subject: SubjectResponse;
  links: ClassSubjectLinkResponse[];
  activeTab: "materials" | "requirements" | "grades" | "progress" | "requests";
  classSectionId: string;
  onTab: (tab: "materials" | "requirements" | "grades" | "progress" | "requests") => void;
  onClassSection: (id: string) => void;
  onApprove: (id: string) => Promise<void>;
  onDecline: (id: string) => Promise<void>;
  onBack: () => void;
  onEdit: () => void;
}) {
  const approved = links.filter((link) => link.status === "APPROVED");
  const pending = links.filter((link) => link.status === "PENDING");
  const selectedLink = approved.find((link) => link.classSectionId === classSectionId);
  const scopedLinks = selectedLink ? [selectedLink] : [];

  return (
    <section>
      <div className="mt-3 flex flex-wrap items-center gap-3">
        <button type="button" className="button button--outline" onClick={onBack}>← All Subjects</button>
        <button type="button" className="text-sm text-[var(--text-muted)]" onClick={onEdit}>Edit subject</button>
        <span className="ml-auto font-mono text-sm font-bold tracking-widest text-[var(--accent-strong)]">Code: {subject.subjectCode}</span>
      </div>
      <div className="workspace-tabs" role="tablist">
        {(["materials", "requirements", "grades", "progress", "requests"] as const).map((tab) => <button key={tab} type="button" role="tab" aria-selected={activeTab === tab} className="workspace-tab" onClick={() => onTab(tab)}>{tab[0].toUpperCase() + tab.slice(1)}</button>)}
      </div>
      {activeTab !== "requests" && approved.length > 0 && <label className="mb-5 block max-w-md text-sm font-medium">Linked Class Section<select className="mt-1 w-full rounded-lg border px-3 py-2" value={classSectionId} onChange={(event) => onClassSection(event.target.value)}>{approved.map((link) => <option key={link.id} value={link.classSectionId}>{link.classSectionName} · {link.schoolYear}</option>)}</select></label>}
      {activeTab === "requests" ? <div><h2 className="mb-3 text-xl font-semibold">Pending Request</h2><SubjectLinkRequestList requests={pending} onApprove={onApprove} onDecline={onDecline} /></div>
        : !selectedLink ? <EmptyState title="No linked class sections" detail="Teaching content becomes available after a class adviser sends a request and you approve it from Requests." />
          : activeTab === "materials" ? <TeacherMaterialsPanel classSectionId={classSectionId} subjectId={subject.id} />
            : activeTab === "requirements" ? <ClassSectionActivitiesTab classSectionId={classSectionId} links={scopedLinks} />
              : activeTab === "grades" ? <ClassSectionGradesTab classSectionId={classSectionId} links={scopedLinks} />
                : <ClassSectionProgressTab classSectionId={classSectionId} links={scopedLinks} />}
    </section>
  );
}
