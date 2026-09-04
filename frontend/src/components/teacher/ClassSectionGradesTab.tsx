import { useState, type FormEvent } from "react";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { gradeService } from "@/services/grade.service";
import { getErrorMessage } from "@/services/errors";
import type { ClassSubjectLinkResponse, GradebookResponse } from "@/types";

export function ClassSectionGradesTab({ classSectionId, links }: {
  classSectionId: string;
  links: ClassSubjectLinkResponse[];
}) {
  const approved = links.filter((link) => link.status === "APPROVED");
  const [subjectId, setSubjectId] = useState(approved[0]?.subjectId ?? "");
  const [written, setWritten] = useState("40");
  const [performance, setPerformance] = useState("40");
  const [test, setTest] = useState("20");
  const [gradebook, setGradebook] = useState<GradebookResponse>();
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const load = async () => {
    if (!subjectId) return;
    try {
      const result = await gradeService.gradebook(classSectionId, subjectId);
      setGradebook(result);
      setWritten(String(result.configuration.writtenActivityWeight));
      setPerformance(String(result.configuration.performanceTaskWeight));
      setTest(String(result.configuration.testWeight));
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  };

  const configure = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await gradeService.configure(classSectionId, subjectId,
        Number(written), Number(performance), Number(test));
      await load();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  if (!approved.length) return <ApiAlert message="Grades require an approved subject link." />;
  return <div className="space-y-5">{error && <ApiAlert message={error} />}<label className="block max-w-md text-sm font-medium">Subject<select className="mt-1 w-full rounded-lg border px-3 py-2" value={subjectId} onChange={(event) => { setSubjectId(event.target.value); setGradebook(undefined); }}><option value="" disabled>Select subject</option>{approved.map((link) => <option key={link.subjectId} value={link.subjectId}>{link.subjectName}</option>)}</select></label><form className="flex flex-wrap items-end gap-3 rounded-xl border bg-white p-5" onSubmit={configure}>{[["Written Activity %", written, setWritten], ["Performance Task %", performance, setPerformance], ["Test %", test, setTest]].map(([label, value, setter]) => <label key={label as string} className="text-sm font-medium">{label as string}<input required min="0" max="100" step="0.01" type="number" className="mt-1 block w-full sm:w-36 rounded-lg border px-3 py-2" value={value as string} onChange={(event) => (setter as (value: string) => void)(event.target.value)} /></label>)}<button disabled={busy || !subjectId} className="rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">Save weights</button><button type="button" disabled={!subjectId} onClick={() => void load()} className="rounded-lg border px-4 py-2 text-sm font-semibold">Load gradebook</button>{gradebook && <button type="button" onClick={() => void gradeService.exportGradebook(classSectionId, subjectId)} className="rounded-lg border px-4 py-2 text-sm font-semibold">Export .xlsx</button>}</form>{gradebook && <div className="scroll-table rounded-xl border bg-white"><table className="w-full text-left text-sm"><thead className="bg-slate-50"><tr><th className="px-3 py-2">Student</th>{gradebook.activities.map((activity) => <th key={activity.id} className="px-3 py-2">{activity.title}<br />/{activity.perfectScore}</th>)}<th className="px-3 py-2">Written</th><th className="px-3 py-2">Performance</th><th className="px-3 py-2">Test</th><th className="px-3 py-2">Final</th></tr></thead><tbody className="divide-y">{gradebook.students.map((student) => <tr key={student.studentId}><td className="px-3 py-2 font-medium">{student.studentName}<br /><span className="text-xs text-slate-500">{student.studentLrn}</span></td>{student.scores.map((score) => <td key={score.activityId} className="px-3 py-2">{score.score ?? "—"}</td>)}<td className="px-3 py-2">{student.writtenActivityAverage}%</td><td className="px-3 py-2">{student.performanceTaskAverage}%</td><td className="px-3 py-2">{student.testAverage}%</td><td className="px-3 py-2 font-bold">{student.finalGrade}</td></tr>)}</tbody></table></div>}</div>;
}
