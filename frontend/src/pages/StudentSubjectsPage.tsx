import { useEffect, useState } from "react";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { LoadingState } from "@/components/shared/LoadingState";
import { StudentSubjects } from "@/components/student/StudentSubjects";
import { getErrorMessage } from "@/services/errors";
import { subjectService } from "@/services/subject.service";
import type { SubjectResponse } from "@/types";

export function StudentSubjectsPage() {
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const load = async () => {
      try {
        setSubjects(await subjectService.listStudentSubjects());
      } catch (requestError) {
        setError(getErrorMessage(requestError));
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, []);

  return (
    <div>
      <h1 className="text-3xl font-semibold tracking-tight">My subjects</h1>
      <p className="mt-1 text-slate-500">Subjects approved for your current class section.</p>
      {error && <div className="mt-5"><ApiAlert message={error} /></div>}
      <div className="mt-6">{loading ? <LoadingState label="Loading subjects…" /> : <StudentSubjects subjects={subjects} />}</div>
    </div>
  );
}
