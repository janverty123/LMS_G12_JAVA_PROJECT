import { useState } from "react";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { EmptyState } from "@/components/shared/EmptyState";
import { LoadingState } from "@/components/shared/LoadingState";
import type { ClassSectionResponse } from "@/types";

export function ClassSectionList({
  sections,
  selectedId,
  loading,
  onSelect,
  onEdit,
  onDelete,
}: {
  sections: ClassSectionResponse[];
  selectedId?: string;
  loading: boolean;
  onSelect: (section: ClassSectionResponse) => void;
  onEdit: (section: ClassSectionResponse) => void;
  onDelete: (section: ClassSectionResponse) => Promise<void>;
}) {
  const [candidate, setCandidate] = useState<ClassSectionResponse | null>(null);
  const [busy, setBusy] = useState(false);

  const confirmDelete = async () => {
    if (!candidate) return;
    setBusy(true);
    try {
      await onDelete(candidate);
      setCandidate(null);
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <LoadingState label="Loading class sections…" />;
  if (!sections.length) {
    return <EmptyState title="No class sections yet" detail="Create your first class section using the form above." />;
  }

  return (
    <>
      <div className="grid gap-3 md:grid-cols-2">
        {sections.map((section) => (
          <article key={section.id} className={`rounded-xl border bg-white p-5 shadow-sm ${selectedId === section.id ? "border-amber-500 ring-1 ring-amber-500" : "border-slate-200"}`}>
            <button type="button" className="w-full text-left" onClick={() => onSelect(section)}>
              <h3 className="font-semibold">{section.gradeLevel} · {section.section}</h3>
              <p className="mt-1 text-sm text-slate-500">School year {section.schoolYear}</p>
              <p className="mt-3 font-mono text-sm font-semibold tracking-widest text-amber-800">{section.classCode}</p>
            </button>
            <div className="mt-4 flex gap-2 border-t border-slate-100 pt-3">
              <button type="button" className="text-sm font-medium text-slate-600 hover:text-slate-900" onClick={() => onEdit(section)}>Edit</button>
              <button type="button" className="text-sm font-medium text-rose-600 hover:text-rose-800" onClick={() => setCandidate(section)}>Delete</button>
            </div>
          </article>
        ))}
      </div>
      <ConfirmDialog
        open={Boolean(candidate)}
        title="Delete class section?"
        message={`This will delete ${candidate?.gradeLevel ?? "the class"} · ${candidate?.section ?? "section"} and its enrollment/link records.`}
        confirmLabel="Delete section"
        busy={busy}
        onCancel={() => setCandidate(null)}
        onConfirm={confirmDelete}
      />
    </>
  );
}
