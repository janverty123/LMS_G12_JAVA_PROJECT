import { useState, type FormEvent } from "react";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { getErrorMessage } from "@/services/errors";
import { progressService } from "@/services/progress.service";
import type { ClassSubjectLinkResponse, StudentProgress } from "@/types";

export function ClassSectionProgressTab({ classSectionId, links }: { classSectionId: string; links: ClassSubjectLinkResponse[] }) {
  const approved = links.filter((link) => link.status === "APPROVED");
  const [subjectId, setSubjectId] = useState(approved[0]?.subjectId ?? "");
  const [onTrack, setOnTrack] = useState("");
  const [attention, setAttention] = useState("");
  const [rows, setRows] = useState<StudentProgress[]>([]);
  const [error, setError] = useState("");
  const configure = async (event: FormEvent) => { event.preventDefault(); try { await progressService.configure(classSectionId, subjectId, Number(onTrack), Number(attention)); setRows(await progressService.dashboard(classSectionId, subjectId)); } catch (value) { setError(getErrorMessage(value)); } };
  const load = async () => { try { setRows(await progressService.dashboard(classSectionId, subjectId)); } catch (value) { setError(getErrorMessage(value)); } };
  if (!approved.length) return <ApiAlert message="Progress requires an approved subject link." />;
  return <div className="space-y-5">{error && <ApiAlert message={error} />}<label className="block max-w-md text-sm font-medium">Subject<select className="mt-1 w-full rounded border px-3 py-2" value={subjectId} onChange={(event) => setSubjectId(event.target.value)}>{approved.map((link) => <option key={link.subjectId} value={link.subjectId}>{link.subjectName}</option>)}</select></label><form className="flex flex-wrap items-end gap-3 rounded-xl border bg-white p-5" onSubmit={configure}><label className="text-sm">On Track minimum %<input required type="number" min="0" max="100" className="mt-1 block w-full sm:w-40 rounded border px-3 py-2" value={onTrack} onChange={(event) => setOnTrack(event.target.value)} /></label><label className="text-sm">Needs Attention minimum %<input required type="number" min="0" max="100" className="mt-1 block w-full sm:w-48 rounded border px-3 py-2" value={attention} onChange={(event) => setAttention(event.target.value)} /></label><button className="rounded bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white">Save thresholds</button><button type="button" className="rounded border px-4 py-2 text-sm font-semibold" onClick={() => void load()}>Load progress</button></form>{rows.length > 0 && <div className="scroll-table rounded-xl border bg-white"><table className="w-full text-left text-sm"><thead><tr><th className="px-3 py-2">Student</th><th className="px-3 py-2">Completion</th><th className="px-3 py-2">Missing</th><th className="px-3 py-2">Grade</th><th className="px-3 py-2">Status</th></tr></thead><tbody>{rows.map((row) => <tr key={row.studentId} className="border-t"><td className="px-3 py-2">{row.studentName}</td><td className="px-3 py-2">{row.completionPercentage}%</td><td className="px-3 py-2" title={row.missingActivities.join(", ")}>{row.missingActivityCount}</td><td className="px-3 py-2">{row.currentGrade}</td><td className="px-3 py-2 font-medium">{row.statusLabel}</td></tr>)}</tbody></table></div>}</div>;
}
