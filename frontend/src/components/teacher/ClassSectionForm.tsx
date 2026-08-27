import { useEffect, useState, type FormEvent } from "react";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { classSectionService } from "@/services/classSection.service";
import { getErrorMessage } from "@/services/errors";
import type { ClassSectionResponse } from "@/types";

export function ClassSectionForm({
  section,
  onSaved,
  onCancel,
}: {
  section?: ClassSectionResponse;
  onSaved: (saved: ClassSectionResponse) => void;
  onCancel?: () => void;
}) {
  const [gradeLevel, setGradeLevel] = useState("");
  const [sectionName, setSectionName] = useState("");
  const [schoolYear, setSchoolYear] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setGradeLevel(section?.gradeLevel ?? "");
    setSectionName(section?.section ?? "");
    setSchoolYear(section?.schoolYear ?? "");
    setError("");
  }, [section]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const request = { gradeLevel, section: sectionName, schoolYear };
      const saved = section
        ? await classSectionService.updateClassSection(section.id, request)
        : await classSectionService.createClassSection(request);
      onSaved(saved);
      if (!section) {
        setGradeLevel("");
        setSectionName("");
        setSchoolYear("");
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm" onSubmit={submit}>
      <h2 className="font-semibold">{section ? "Edit class section" : "Create a class section"}</h2>
      <div className="mt-4 grid gap-4 sm:grid-cols-3">
        <label className="text-sm font-medium text-slate-700">
          Grade level
          <input required className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" placeholder="Grade 12" value={gradeLevel} onChange={(event) => setGradeLevel(event.target.value)} />
        </label>
        <label className="text-sm font-medium text-slate-700">
          Section
          <input required className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" placeholder="Integrity" value={sectionName} onChange={(event) => setSectionName(event.target.value)} />
        </label>
        <label className="text-sm font-medium text-slate-700">
          School year
          <input required pattern="\d{4}-\d{4}" className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" placeholder="2026-2027" value={schoolYear} onChange={(event) => setSchoolYear(event.target.value)} />
        </label>
      </div>
      {error && <div className="mt-4"><ApiAlert message={error} /></div>}
      <div className="mt-4 flex gap-3">
        <button disabled={busy} className="rounded-lg bg-amber-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">
          {busy ? "Saving…" : section ? "Save changes" : "Create section"}
        </button>
        {section && onCancel && (
          <button type="button" onClick={onCancel} className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium">Cancel</button>
        )}
      </div>
    </form>
  );
}
