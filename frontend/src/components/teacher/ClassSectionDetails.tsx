import { useCallback, useEffect, useState, type FormEvent } from "react";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { EmptyState } from "@/components/shared/EmptyState";
import { LoadingState } from "@/components/shared/LoadingState";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { ClassEnrollmentRequestList } from "./ClassEnrollmentRequestList";
import { classSectionService } from "@/services/classSection.service";
import { getErrorMessage } from "@/services/errors";
import { subjectService } from "@/services/subject.service";
import type {
  ClassEnrollmentRequestResponse,
  ClassSectionResponse,
  ClassSubjectLinkResponse,
  ClassSectionMemberResponse,
} from "@/types";

export function ClassSectionDetails({
  section,
}: {
  section: ClassSectionResponse;
}) {
  const [requests, setRequests] = useState<ClassEnrollmentRequestResponse[]>(
    [],
  );
  const [members, setMembers] = useState<ClassSectionMemberResponse[]>([]);
  const [links, setLinks] = useState<ClassSubjectLinkResponse[]>([]);
  const [subjectCode, setSubjectCode] = useState("");
  const [removeCandidate, setRemoveCandidate] =
    useState<ClassSectionMemberResponse | null>(null);
  const [activeTab, setActiveTab] = useState<"roster" | "requests" | "links">(
    "roster",
  );
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
      await subjectService.createLinkRequest(
        section.id,
        subjectCode.trim().toUpperCase(),
      );
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
      await classSectionService.removeMember(
        section.id,
        removeCandidate.studentId,
      );
      setRemoveCandidate(null);
      await refresh();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="mt-8 space-y-6 border-t border-[var(--border)] pt-8">
      <div className="page-heading">
        <h1>{section.section}</h1>
        <p>
          a.y {section.schoolYear} · {section.gradeLevel} · Code{" "}
          <strong>{section.classCode}</strong>
        </p>
      </div>
      {error && <ApiAlert message={error} />}
      {success && <ApiAlert message={success} tone="success" />}
      <div className="workspace-tabs" role="tablist">
        {(["roster", "requests", "links"] as const).map((tab) => (
          <button
            key={tab}
            type="button"
            role="tab"
            aria-selected={activeTab === tab}
            onClick={() => setActiveTab(tab)}
            className="workspace-tab"
          >
            {tab === "links"
              ? "Linked Subjects"
              : tab[0].toUpperCase() + tab.slice(1)}
          </button>
        ))}
      </div>
      {loading ? (
        <LoadingState label="Loading class details…" />
      ) : activeTab === "requests" ? (
        <div>
          <h3 className="mb-3 text-lg font-semibold">Pending Requests</h3>
          <ClassEnrollmentRequestList
            requests={requests}
            onApprove={(id) => decideRequest(id, "approve")}
            onDecline={(id) => decideRequest(id, "decline")}
          />
        </div>
      ) : activeTab === "links" ? (
        <div className="panel">
          <h3 className="text-lg font-semibold">Linked Subject</h3>
          <p className="mt-1 text-sm text-[var(--text-muted)]">
            Enter a subject code from a subject teacher to send a link request.
          </p>
          <form className="mt-4 flex max-w-2xl gap-3" onSubmit={submitLink}>
            <input
              required
              minLength={7}
              maxLength={7}
              className="min-w-0 flex-1 rounded-lg border px-3 py-2 font-mono uppercase tracking-widest"
              placeholder="Enter subject code, e.g. AKN7967"
              value={subjectCode}
              onChange={(event) =>
                setSubjectCode(event.target.value.toUpperCase())
              }
            />
            <button disabled={busy} className="button button--outline">
              Request to link
            </button>
          </form>
          <h4 className="mt-7 font-semibold">Linked Subjects</h4>
          {!links.length ? (
            <div className="mt-3">
              <EmptyState
                title="No linked subjects"
                detail="Requested and approved subjects will appear here."
              />
            </div>
          ) : (
            <div className="mt-3 overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr>
                    <th className="px-3 py-2">Subject</th>
                    <th className="px-3 py-2">Subject Teacher</th>
                    <th className="px-3 py-2">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {links.map((link) => (
                    <tr
                      key={link.id}
                      className="border-t border-[var(--border)]"
                    >
                      <td className="px-3 py-3">{link.subjectName}</td>
                      <td className="px-3 py-3">
                      —
                      </td>
                      <td className="px-3 py-3">
                        <StatusBadge status={link.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ) : (
        <>
          <div>
            <h3 className="mb-3 text-lg font-semibold">Students List</h3>
            {!members.length ? (
              <EmptyState
                title="No enrolled students"
                detail="Approved students will appear here."
              />
            ) : (
              <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
                <table className="w-full text-left text-sm">
                  <thead className="bg-slate-50 text-slate-600">
                    <tr>
                      <th className="px-4 py-3">Student</th>
                      <th className="px-4 py-3">LRN</th>
                      <th className="px-4 py-3">Email</th>
                      <th className="px-4 py-3">
                        <span className="sr-only">Actions</span>
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {members.map((member) => (
                      <tr key={member.studentId}>
                        <td className="px-4 py-3 font-medium">
                          {member.studentName}
                        </td>
                        <td className="px-4 py-3 text-slate-600">
                          {member.studentLrn}
                        </td>
                        <td className="px-4 py-3 text-slate-600">
                          {member.studentEmail}
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            className="font-medium text-rose-600"
                            onClick={() => setRemoveCandidate(member)}
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
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
