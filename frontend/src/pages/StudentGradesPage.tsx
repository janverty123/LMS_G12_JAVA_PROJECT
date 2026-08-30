import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { LoadingState } from "@/components/shared/LoadingState";
import { getErrorMessage } from "@/services/errors";
import { gradeService } from "@/services/grade.service";
import type { StudentGrade } from "@/types";

export function StudentGradesPage() {
  const { subjectId = "" } = useParams();
  const [grade, setGrade] = useState<StudentGrade>();
  const [error, setError] = useState("");
  useEffect(() => { gradeService.studentGrades(subjectId).then(setGrade).catch((value) => setError(getErrorMessage(value))); }, [subjectId]);
  if (error) return <ApiAlert message={error} />;
  if (!grade) return <LoadingState label="Loading grades…" />;
  return <div><h1 className="text-3xl font-semibold">My grades</h1><div className="mt-6 grid gap-4 sm:grid-cols-4">{[["Written Activity", grade.writtenActivityAverage], ["Performance Task", grade.performanceTaskAverage], ["Test", grade.testAverage], ["Final Grade", grade.finalGrade]].map(([label, value]) => <div key={label as string} className="rounded-xl border bg-white p-5"><p className="text-sm text-slate-500">{label}</p><p className="mt-2 text-3xl font-bold">{value as number}</p></div>)}</div></div>;
}
