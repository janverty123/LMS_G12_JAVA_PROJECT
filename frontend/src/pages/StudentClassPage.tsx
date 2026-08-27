import { useCallback, useEffect, useState } from "react";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { LoadingState } from "@/components/shared/LoadingState";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { JoinClassForm } from "@/components/student/JoinClassForm";
import { StudentClassSection } from "@/components/student/StudentClassSection";
import { classSectionService } from "@/services/classSection.service";
import { getErrorMessage, hasStatus } from "@/services/errors";
import type { ClassEnrollmentRequestResponse, StudentClassSectionResponse } from "@/types";

export function StudentClassPage() {
  const [section, setSection] = useState<StudentClassSectionResponse | null>(null);
  const [requests, setRequests] = useState<ClassEnrollmentRequestResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    const [requestsResult, sectionResult] = await Promise.allSettled([
      classSectionService.listOwnEnrollmentRequests(),
      classSectionService.getOwnClassSection(),
    ]);

    if (requestsResult.status === "fulfilled") setRequests(requestsResult.value);
    else setError(getErrorMessage(requestsResult.reason));

    if (sectionResult.status === "fulfilled") setSection(sectionResult.value);
    else if (hasStatus(sectionResult.reason, 404)) setSection(null);
    else setError(getErrorMessage(sectionResult.reason));
    setLoading(false);
  }, []);

  useEffect(() => { void load(); }, [load]);

  const submitted = (request: ClassEnrollmentRequestResponse) => {
    setRequests((current) => [request, ...current]);
    setSuccess("Your request was sent to the class adviser.");
  };

  return (
    <div>
      <h1 className="text-3xl font-semibold tracking-tight">My class section</h1>
      <p className="mt-1 text-slate-500">Join a class and track your enrollment request.</p>
      {error && <div className="mt-5"><ApiAlert message={error} /></div>}
      {success && <div className="mt-5"><ApiAlert message={success} tone="success" /></div>}
      <div className="mt-6">{loading ? <LoadingState label="Loading your class…" /> : <StudentClassSection section={section} />}</div>
      {!loading && !section && <div className="mt-6"><JoinClassForm onSubmitted={submitted} /></div>}

      {!loading && (
        <section className="mt-8">
          <h2 className="text-lg font-semibold">Request history</h2>
          {!requests.length ? (
            <p className="mt-2 text-sm text-slate-500">You have not submitted a class request.</p>
          ) : (
            <div className="mt-3 divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white">
              {requests.map((request) => (
                <div key={request.id} className="flex flex-wrap items-center justify-between gap-3 p-4">
                  <div><p className="font-medium">{request.classSectionName}</p><p className="text-sm text-slate-500">{request.schoolYear} · updated {new Date(request.updatedAt).toLocaleDateString()}</p></div>
                  <StatusBadge status={request.status} />
                </div>
              ))}
            </div>
          )}
        </section>
      )}
    </div>
  );
}
