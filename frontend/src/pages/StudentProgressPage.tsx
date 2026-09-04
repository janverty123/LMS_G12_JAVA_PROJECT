import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { LoadingState } from "@/components/shared/LoadingState";
import { getErrorMessage } from "@/services/errors";
import { progressService } from "@/services/progress.service";
import type { StudentProgress } from "@/types";
import { StudentSubjectHeader } from "@/components/student/StudentSubjectHeader";

export function StudentProgressPage() {
  const { subjectId = "" } = useParams();
  const [value, setValue] = useState<StudentProgress>();
  const [error, setError] = useState("");
  useEffect(() => {
    progressService
      .own(subjectId)
      .then(setValue)
      .catch((reason) => setError(getErrorMessage(reason)));
  }, [subjectId]);
  if (error) return <ApiAlert message={error} />;
  if (!value) return <LoadingState label="Loading progress…" />;
  return (
    <div>
      <StudentSubjectHeader />
      <h2 className="text-2xl font-semibold">Progress</h2>
      <div className="mt-6 grid gap-4 sm:grid-cols-4">
        {[
          ["Completion", `${value.completionPercentage}%`],
          ["Missing", value.missingActivityCount],
          ["Current grade", value.currentGrade],
          ["Status", value.statusLabel],
        ].map(([label, item]) => (
          <div key={label as string} className="panel">
            <p className="text-sm text-[var(--text-muted)]">{label}</p>
            <p className="mt-2 text-2xl font-bold">{item}</p>
          </div>
        ))}
      </div>
      {value.missingActivities.length > 0 && (
        <div className="panel mt-6">
          <h2 className="font-semibold">Missing Activities</h2>
          <ul className="mt-2 list-disc pl-5 text-sm">
            {value.missingActivities.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
