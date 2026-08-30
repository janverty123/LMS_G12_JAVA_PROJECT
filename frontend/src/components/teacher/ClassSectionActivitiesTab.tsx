import { useCallback, useEffect, useState, type FormEvent } from "react";
import { ActivityList } from "@/components/shared/ActivityList";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { LoadingState } from "@/components/shared/LoadingState";
import { activityService } from "@/services/activity.service";
import { getErrorMessage } from "@/services/errors";
import type { ActivityFileResponse, ActivityResponse, ActivitySubmissionResponse, ActivityType, ClassSubjectLinkResponse, ScoreProposalResponse } from "@/types";

const initialDeadline = () => {
  const date = new Date(Date.now() + 24 * 60 * 60 * 1000);
  date.setSeconds(0, 0);
  return date.toISOString().slice(0, 16);
};

export function ClassSectionActivitiesTab({
  classSectionId,
  links,
}: {
  classSectionId: string;
  links: ClassSubjectLinkResponse[];
}) {
  const approved = links.filter((link) => link.status === "APPROVED");
  const [subjectId, setSubjectId] = useState(approved[0]?.subjectId ?? "");
  const [activities, setActivities] = useState<ActivityResponse[]>([]);
  const [editing, setEditing] = useState<ActivityResponse>();
  const [deleting, setDeleting] = useState<ActivityResponse | null>(null);
  const [selected, setSelected] = useState<ActivityResponse>();
  const [attachments, setAttachments] = useState<ActivityFileResponse[]>([]);
  const [submissions, setSubmissions] = useState<ActivitySubmissionResponse[]>([]);
  const [attachmentFile, setAttachmentFile] = useState<File>();
  const [uploadProgress, setUploadProgress] = useState(0);
  const [scores, setScores] = useState<Record<string, string>>({});
  const [proposals, setProposals] = useState<ScoreProposalResponse[]>([]);
  const [proposalScores, setProposalScores] = useState<Record<string, string>>({});
  const [type, setType] = useState<ActivityType>("WRITTEN_ACTIVITY");
  const [title, setTitle] = useState("");
  const [perfectScore, setPerfectScore] = useState("20");
  const [deadline, setDeadline] = useState(initialDeadline);
  const [instructions, setInstructions] = useState("");
  const [allowScore, setAllowScore] = useState(false);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    if (!subjectId) return setActivities([]);
    setLoading(true);
    try {
      setActivities(await activityService.listTeacherActivities(classSectionId, subjectId));
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [classSectionId, subjectId]);

  useEffect(() => { void load(); }, [load]);

  const reset = () => {
    setEditing(undefined);
    setType("WRITTEN_ACTIVITY");
    setTitle("");
    setPerfectScore("20");
    setDeadline(initialDeadline());
    setInstructions("");
    setAllowScore(false);
  };

  const edit = (activity: ActivityResponse) => {
    setEditing(activity);
    setType(activity.type);
    setTitle(activity.title);
    setPerfectScore(String(activity.perfectScore));
    setDeadline(new Date(activity.deadline).toISOString().slice(0, 16));
    setInstructions(activity.instructions);
    setAllowScore(activity.allowStudentSelfSubmissionScore);
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!subjectId) return;
    setBusy(true);
    setError("");
    const request = {
      type,
      title,
      perfectScore: Number(perfectScore),
      deadline: new Date(deadline).toISOString(),
      instructions,
      allowStudentSelfSubmissionScore: allowScore,
    };
    try {
      if (editing) await activityService.updateActivity(editing.id, request);
      else await activityService.createActivity(classSectionId, subjectId, request);
      reset();
      await load();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    if (!deleting) return;
    setBusy(true);
    try {
      await activityService.deleteActivity(deleting.id);
      setDeleting(null);
      await load();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  const openActivity = async (activity: ActivityResponse) => {
    setSelected(activity);
    setError("");
    try {
      const [nextAttachments, nextSubmissions, nextProposals] = await Promise.all([
        activityService.listTeacherAttachments(activity.id),
        activityService.listSubmissions(activity.id),
        activityService.listScoreProposals(activity.id),
      ]);
      setAttachments(nextAttachments);
      setSubmissions(nextSubmissions);
      setProposals(nextProposals);
      setProposalScores(Object.fromEntries(nextProposals.map((item) => [item.id, String(item.reportedScore)])));
      setScores(Object.fromEntries(nextSubmissions.map((item) => [
        item.submissionId ?? item.studentId, item.score == null ? "" : String(item.score)
      ])));
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  };

  const uploadAttachment = async () => {
    if (!selected || !attachmentFile) return;
    setBusy(true);
    try {
      await activityService.uploadTeacherAttachment(selected.id, attachmentFile,
        (progress) => setUploadProgress(progress.percentage));
      setAttachmentFile(undefined);
      setUploadProgress(0);
      await openActivity(selected);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  const saveScore = async (submission: ActivitySubmissionResponse) => {
    if (!submission.submissionId) return;
    const value = scores[submission.submissionId];
    setBusy(true);
    try {
      await activityService.scoreSubmission(submission.submissionId, Number(value));
      if (selected) await openActivity(selected);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  const reviewProposal = async (proposal: ScoreProposalResponse, decision: "approve" | "reject") => {
    setBusy(true);
    try {
      if (decision === "reject") await activityService.rejectScoreProposal(proposal.id);
      else {
        const edited = Number(proposalScores[proposal.id]);
        await activityService.approveScoreProposal(proposal.id,
          edited === proposal.reportedScore ? undefined : edited);
      }
      if (selected) await openActivity(selected);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  if (!approved.length) return <ApiAlert message="Activities require an approved subject link." />;

  return (
    <div className="space-y-6">
      {error && <ApiAlert message={error} />}
      <label className="block max-w-md text-sm font-medium">Subject
        <select className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" value={subjectId} onChange={(event) => { setSubjectId(event.target.value); reset(); }}>
          {approved.map((link) => <option key={link.subjectId} value={link.subjectId}>{link.subjectName}</option>)}
        </select>
      </label>
      <form className="grid gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm md:grid-cols-2" onSubmit={submit}>
        <label className="text-sm font-medium">Activity type<select className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" value={type} onChange={(event) => setType(event.target.value as ActivityType)}><option value="WRITTEN_ACTIVITY">Written Activity</option><option value="PERFORMANCE_TASK">Performance Task</option><option value="TEST">Test</option></select></label>
        <label className="text-sm font-medium">Title<input required maxLength={150} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" value={title} onChange={(event) => setTitle(event.target.value)} /></label>
        <label className="text-sm font-medium">Perfect score<input required min="0.01" step="0.01" type="number" className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" value={perfectScore} onChange={(event) => setPerfectScore(event.target.value)} /></label>
        <label className="text-sm font-medium">Deadline<input required type="datetime-local" className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" value={deadline} onChange={(event) => setDeadline(event.target.value)} /></label>
        <label className="text-sm font-medium md:col-span-2">Instructions<textarea required maxLength={5000} rows={4} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" value={instructions} onChange={(event) => setInstructions(event.target.value)} /></label>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={allowScore} onChange={(event) => setAllowScore(event.target.checked)} />Allow student self-submitted score</label>
        <div className="flex justify-end gap-2"><button disabled={busy} className="rounded-lg bg-amber-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">{editing ? "Save activity" : "Create activity"}</button>{editing && <button type="button" className="rounded-lg border border-slate-300 px-4 py-2 text-sm" onClick={reset}>Cancel</button>}</div>
      </form>
      {loading ? <LoadingState label="Loading activities…" /> : <ActivityList activities={activities} onEdit={edit} onDelete={setDeleting} onOpen={(activity) => void openActivity(activity)} />}
      {selected && <section className="space-y-4 rounded-xl border border-slate-200 bg-slate-50 p-5"><h3 className="text-xl font-semibold">{selected.title}: files and submissions</h3><div className="flex flex-wrap items-center gap-3"><input type="file" onChange={(event) => setAttachmentFile(event.target.files?.[0])} /><button type="button" disabled={!attachmentFile || busy} onClick={() => void uploadAttachment()} className="rounded-lg bg-amber-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">Upload teacher attachment</button>{uploadProgress > 0 && <span className="text-sm">{uploadProgress}%</span>}</div>{attachments.length > 0 && <div className="flex flex-wrap gap-2">{attachments.map((file) => <button key={file.id} type="button" className="text-sm font-medium text-amber-700" onClick={() => void activityService.downloadFile(file.id, false)}>{file.fileName} ↓</button>)}</div>}<div className="overflow-x-auto rounded-lg border border-slate-200 bg-white"><table className="w-full text-left text-sm"><thead className="bg-slate-50"><tr><th className="px-3 py-2">Student</th><th className="px-3 py-2">Status</th><th className="px-3 py-2">Submitted</th><th className="px-3 py-2">Work</th><th className="px-3 py-2">Score</th></tr></thead><tbody className="divide-y">{submissions.map((submission) => <tr key={submission.studentId}><td className="px-3 py-2">{submission.studentName}<br /><span className="text-xs text-slate-500">{submission.studentLrn}</span></td><td className="px-3 py-2">{submission.status}</td><td className="px-3 py-2">{submission.submittedAt ? new Date(submission.submittedAt).toLocaleString() : "—"}</td><td className="px-3 py-2">{submission.file ? <button className="text-amber-700" onClick={() => void activityService.downloadFile(submission.file!.id, false)}>{submission.file.fileName}</button> : "—"}</td><td className="px-3 py-2">{submission.submissionId ? <div className="flex gap-2"><input aria-label={`Score for ${submission.studentName}`} className="w-20 rounded border px-2 py-1" type="number" min="0" max={submission.perfectScore} step="0.01" value={scores[submission.submissionId] ?? ""} onChange={(event) => setScores((current) => ({ ...current, [submission.submissionId!]: event.target.value }))} /><button type="button" disabled={busy || scores[submission.submissionId] === ""} className="font-medium text-amber-700" onClick={() => void saveScore(submission)}>Save</button></div> : "—"}</td></tr>)}</tbody></table></div>{selected.allowStudentSelfSubmissionScore && <div><h4 className="font-semibold">Student score proposals</h4>{!proposals.length ? <p className="mt-2 text-sm text-slate-500">No proposals.</p> : <div className="mt-2 space-y-2">{proposals.map((proposal) => <div key={proposal.id} className="flex flex-wrap items-center gap-3 rounded-lg border bg-white p-3 text-sm"><span className="font-medium">{proposal.studentName}</span><span>{proposal.status}</span><button className="text-amber-700" onClick={() => void activityService.downloadFile(proposal.proofFile.id, false)}>Proof photo</button>{proposal.status === "PENDING" && <><input className="w-24 rounded border px-2 py-1" type="number" min="0" max={proposal.perfectScore} value={proposalScores[proposal.id] ?? ""} onChange={(event) => setProposalScores((current) => ({ ...current, [proposal.id]: event.target.value }))} /><button className="font-medium text-emerald-700" onClick={() => void reviewProposal(proposal, "approve")}>Approve</button><button className="font-medium text-rose-700" onClick={() => void reviewProposal(proposal, "reject")}>Reject</button></>}</div>)}</div>}</div>}</section>}
      <ConfirmDialog open={Boolean(deleting)} title="Delete activity?" message={`Delete ${deleting?.title ?? "this activity"}?`} confirmLabel="Delete activity" busy={busy} onCancel={() => setDeleting(null)} onConfirm={remove} />
    </div>
  );
}
