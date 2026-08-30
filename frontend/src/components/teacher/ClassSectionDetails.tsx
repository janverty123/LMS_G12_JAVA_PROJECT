import { useCallback, useEffect, useState, type FormEvent } from "react";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { EmptyState } from "@/components/shared/EmptyState";
import { LoadingState } from "@/components/shared/LoadingState";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { ClassEnrollmentRequestList } from "./ClassEnrollmentRequestList";
import { ClassSectionMaterialsTab } from "./ClassSectionMaterialsTab";
import { ClassSectionActivitiesTab } from "./ClassSectionActivitiesTab";
import { ClassSectionGradesTab } from "./ClassSectionGradesTab";
import { ClassSectionProgressTab } from "./ClassSectionProgressTab";
import { ClassSectionAnnouncementsTab } from "./ClassSectionAnnouncementsTab";
import { classSectionService } from "@/services/classSection.service";
import { getErrorMessage } from "@/services/errors";
import { subjectService } from "@/services/subject.service";
import type {
  ClassEnrollmentRequestResponse,
  ClassSectionResponse,
  ClassSubjectLinkResponse,
  ClassSectionMemberResponse,
} from "@/types";

export function ClassSectionDetails({ section }: { section: ClassSectionResponse }) {
  const [requests, setRequests] = useState<ClassEnrollmentRequestResponse[]>([]);
  const [members, setMembers] = useState<ClassSectionMemberResponse[]>([]);
  const [links, setLinks] = useState<ClassSubjectLinkResponse[]>([]);
  const [subjectCode, setSubjectCode] = useState("");
  const [removeCandidate, setRemoveCandidate] = useState<ClassSectionMemberResponse | null>(null);
  const [activeTab, setActiveTab] = useState<"overview" | "materials" | "activities" | "grades" | "progress" | "announcements">("overview");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const refresh = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [nextRequests, nextMembers, nextLinks] = await Promise.all([
        classSectionService.listEnrollmentRequests(section.id),
        classSectionService.listMembers(section.id),
        subjectService.listLinksForClassSection(section.id),
      ]);
      setRequests(nextRequests);
      setMembers(nextMembers);
      setLinks(nextLinks);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [section.id]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const decideRequest = async (id: string, decision: "approve" | "decline") => {
    setError("");
    try {
      if (decision === "approve") {
        await classSectionService.approveEnrollmentRequest(id);
      } else {
        await classSectionService.declineEnrollmentRequest(id);
      }
      await refresh();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  };

  const submitLink = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      await subjectService.createLinkRequest(section.id, subjectCode.trim().toUpperCase());
      setSubjectCode("");
      setSuccess("Subject link request submitted.");
      await refresh();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  const removeMember = async () => {
    if (!removeCandidate) return;
    setBusy(true);
    setError("");
    try {
      await classSectionService.removeMember(section.id, removeCandidate.studentId);
      setRemoveCandidate(null);
      await refresh();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="mt-8 space-y-7 border-t border-slate-200 pt-8">
      <div>
        <p className="text-sm font-semibold uppercase tracking-wide text-amber-700">Selected class</p>
        <h2 className="mt-1 text-2xl font-semibold">{section.gradeLevel} · {section.section}</h2>
        <p className="mt-1 text-sm text-slate-500">Share class code <span className="font-mono font-bold tracking-widest text-slate-800">{section.classCode}</span> with students.</p>
      </div>
      {error && <ApiAlert message={error} />}
      {success && <ApiAlert message={success} tone="success" />}
      <div className="flex gap-2 overflow-x-auto border-b border-slate-200" role="tablist">
        {(["overview", "materials", "activities", "grades", "progress", "announcements"] as const).map((tab) => (
          <button
            key={tab}
            type="button"
            role="tab"
            aria-selected={activeTab === tab}
            onClick={() => setActiveTab(tab)}
            className={`shrink-0 border-b-2 px-3 py-2 text-sm font-semibold capitalize sm:px-4 ${
              activeTab === tab
                ? "border-amber-600 text-amber-700"
                : "border-transparent text-slate-500"
            }`}
          >
            {tab}
          </button>
        ))}
      </div>
      {loading ? (
        <LoadingState label="Loading class details…" />
      ) : activeTab === "materials" ? (
        <ClassSectionMaterialsTab classSectionId={section.id} links={links} />
      ) : activeTab === "activities" ? (
        <ClassSectionActivitiesTab classSectionId={section.id} links={links} />
      ) : activeTab === "grades" ? (
        <ClassSectionGradesTab classSectionId={section.id} links={links} />
      ) : activeTab === "progress" ? (
        <ClassSectionProgressTab classSectionId={section.id} links={links} />
      ) : activeTab === "announcements" ? (
        <ClassSectionAnnouncementsTab classSectionId={section.id} />
      ) : (
        <>
          <div>
            <h3 className="mb-3 text-lg font-semibold">Pending student requests</h3>
            <ClassEnrollmentRequestList
              requests={requests}
              onApprove={(id) => decideRequest(id, "approve")}
              onDecline={(id) => decideRequest(id, "decline")}
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold">Enrolled students</h3>
            {!members.length ? (
              <EmptyState title="No enrolled students" detail="Approved students will appear here." />
            ) : (
              <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
                <table className="w-full text-left text-sm">
                  <thead className="bg-slate-50 text-slate-600"><tr><th className="px-4 py-3">Student</th><th className="px-4 py-3">LRN</th><th className="px-4 py-3">Email</th><th className="px-4 py-3"><span className="sr-only">Actions</span></th></tr></thead>
                  <tbody className="divide-y divide-slate-100">
                    {members.map((member) => (
                      <tr key={member.studentId}>
                        <td className="px-4 py-3 font-medium">{member.studentName}</td>
                        <td className="px-4 py-3 text-slate-600">{member.studentLrn}</td>
                        <td className="px-4 py-3 text-slate-600">{member.studentEmail}</td>
                        <td className="px-4 py-3 text-right"><button className="font-medium text-rose-600" onClick={() => setRemoveCandidate(member)}>Remove</button></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          <div>
            <h3 className="text-lg font-semibold">Class subjects</h3>
            <form className="mt-3 flex max-w-lg gap-3" onSubmit={submitLink}>
              <input required minLength={7} maxLength={7} className="min-w-0 flex-1 rounded-lg border border-slate-300 px-3 py-2 font-mono uppercase tracking-widest" placeholder="7-character subject code" value={subjectCode} onChange={(event) => setSubjectCode(event.target.value.toUpperCase())} />
              <button disabled={busy} className="rounded-lg bg-amber-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">Request link</button>
            </form>
            {!links.length ? (
              <div className="mt-3"><EmptyState title="No linked subjects" detail="Enter a subject teacher's code to request a link." /></div>
            ) : (
              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                {links.map((link) => (
                  <div key={link.id} className="flex items-center justify-between rounded-xl border border-slate-200 bg-white p-4">
                    <div><p className="font-medium">{link.subjectName}</p><p className="text-sm text-slate-500">Updated {new Date(link.updatedAt).toLocaleDateString()}</p></div>
                    <StatusBadge status={link.status} />
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}
      <ConfirmDialog
        open={Boolean(removeCandidate)}
        title="Remove student?"
        message={`${removeCandidate?.studentName ?? "This student"} will lose access to this class and its approved subjects.`}
        confirmLabel="Remove student"
        busy={busy}
        onCancel={() => setRemoveCandidate(null)}
        onConfirm={removeMember}
      />
    </section>
  );
}
