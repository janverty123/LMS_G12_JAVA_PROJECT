import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { ActivityList } from "@/components/shared/ActivityList";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { LoadingState } from "@/components/shared/LoadingState";
import { StudentSubjectHeader } from "@/components/student/StudentSubjectHeader";
import { activityService } from "@/services/activity.service";
import { getErrorMessage } from "@/services/errors";
import type {
  ActivityFileResponse,
  ActivityResponse,
  ActivitySubmissionResponse,
  ScoreProposalResponse,
} from "@/types";

export function StudentActivitiesPage() {
  const { subjectId = "" } = useParams();
  const [activities, setActivities] = useState<ActivityResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selected, setSelected] = useState<ActivityResponse>();
  const [submission, setSubmission] = useState<ActivitySubmissionResponse>();
  const [attachments, setAttachments] = useState<ActivityFileResponse[]>([]);
  const [file, setFile] = useState<File>();
  const [progress, setProgress] = useState(0);
  const [busy, setBusy] = useState(false);
  const [proposal, setProposal] = useState<ScoreProposalResponse>();
  const [reportedScore, setReportedScore] = useState("");
  const [proof, setProof] = useState<File>();

  useEffect(() => {
    activityService
      .listStudentActivities(subjectId)
      .then(setActivities)
      .catch((requestError) => setError(getErrorMessage(requestError)))
      .finally(() => setLoading(false));
  }, [subjectId]);

  const open = async (activity: ActivityResponse) => {
    setSelected(activity);
    try {
      const [own, files, ownProposal] = await Promise.all([
        activityService.getOwnSubmission(activity.id),
        activityService.listStudentAttachments(activity.id),
        activity.allowStudentSelfSubmissionScore
          ? activityService.getOwnScoreProposal(activity.id)
          : Promise.resolve(undefined),
      ]);
      setSubmission(own);
      setAttachments(files);
      setProposal(ownProposal);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  };

  const submit = async () => {
    if (!selected || !file) return;
    setBusy(true);
    try {
      setSubmission(
        await activityService.uploadStudentSubmission(
          selected.id,
          file,
          (value) => setProgress(value.percentage),
        ),
      );
      setFile(undefined);
      setProgress(0);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  const submitProposal = async () => {
    if (!selected || !proof) return;
    setBusy(true);
    try {
      setProposal(
        await activityService.uploadScoreProposal(
          selected.id,
          Number(reportedScore),
          proof,
          (value) => setProgress(value.percentage),
        ),
      );
      setProof(undefined);
      setProgress(0);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <StudentSubjectHeader />
      <h2 className="text-2xl font-semibold">Academic Requirements</h2>
      <p className="mt-1 text-slate-500">
        Requirements and deadlines for this subject.
      </p>
      {error && (
        <div className="mt-5">
          <ApiAlert message={error} />
        </div>
      )}
      <div className="mt-6">
        {loading ? (
          <LoadingState label="Loading activities…" />
        ) : (
          <ActivityList
            activities={activities}
            onOpen={(activity) => void open(activity)}
          />
        )}
      </div>
      {selected && (
        <section className="mt-6 space-y-4 rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="text-xl font-semibold">
            Submit work for {selected.title}
          </h2>
          {attachments.length > 0 && (
            <div>
              <p className="text-sm font-medium">Teacher attachments</p>
              {attachments.map((attachment) => (
                <button
                  key={attachment.id}
                  className="mr-3 mt-2 text-sm text-[var(--accent-strong)]"
                  onClick={() =>
                    void activityService.downloadFile(attachment.id, true)
                  }
                >
                  {attachment.fileName} ↓
                </button>
              ))}
            </div>
          )}
          <p className="text-sm">
            Status: <strong>{submission?.status ?? "NOT_SUBMITTED"}</strong>
            {submission?.submittedAt
              ? ` · ${new Date(submission.submittedAt).toLocaleString()}`
              : ""}
            {submission?.score != null
              ? ` · Score ${submission.score}/${submission.perfectScore}`
              : ""}
          </p>
          {submission?.file && (
            <button
              className="text-sm font-medium text-[var(--accent-strong)]"
              onClick={() =>
                void activityService.downloadFile(submission.file!.id, true)
              }
            >
              Download current submission: {submission.file.fileName}
            </button>
          )}
          <div className="flex flex-wrap items-center gap-3">
            <input
              type="file"
              onChange={(event) => setFile(event.target.files?.[0])}
            />
            <button
              disabled={!file || busy}
              className="rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              onClick={() => void submit()}
            >
              {submission?.submissionId ? "Resubmit work" : "Submit work"}
            </button>
            {progress > 0 && <span className="text-sm">{progress}%</span>}
          </div>
          {selected.allowStudentSelfSubmissionScore && (
            <div className="border-t pt-4">
              <h3 className="font-semibold">Report a score with proof</h3>
              {proposal && proposal.status !== "REJECTED" ? (
                <p className="mt-2 text-sm">
                  Proposal: <strong>{proposal.status}</strong> · Reported{" "}
                  {proposal.reportedScore}/{proposal.perfectScore}
                  {proposal.approvedScore != null
                    ? ` · Approved ${proposal.approvedScore}`
                    : ""}
                </p>
              ) : (
                <div className="mt-3 flex flex-wrap items-end gap-3">
                  <label className="text-sm">
                    Reported score
                    <input
                      type="number"
                      min="0"
                      max={selected.perfectScore}
                      step="0.01"
                      className="mt-1 block w-32 rounded border px-2 py-1"
                      value={reportedScore}
                      onChange={(event) => setReportedScore(event.target.value)}
                    />
                  </label>
                  <label className="text-sm">
                    Proof photo
                    <input
                      type="file"
                      accept="image/*"
                      className="mt-1 block"
                      onChange={(event) => setProof(event.target.files?.[0])}
                    />
                  </label>
                  <button
                    disabled={!proof || reportedScore === "" || busy}
                    onClick={() => void submitProposal()}
                    className="rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                  >
                    Submit score proposal
                  </button>
                </div>
              )}
            </div>
          )}
        </section>
      )}
    </div>
  );
}
