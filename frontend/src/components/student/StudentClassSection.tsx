import { EmptyState } from "@/components/shared/EmptyState";
import type { StudentClassSectionResponse } from "@/types";

export function StudentClassSection({ section }: { section: StudentClassSectionResponse | null }) {
  if (!section) {
    return <EmptyState title="No approved class section" detail="Submit a class code below and wait for the adviser to approve it." />;
  }

  return (
    <article className="rounded-xl border border-emerald-200 bg-emerald-50 p-5">
      <p className="text-sm font-semibold uppercase tracking-wide text-emerald-700">Approved class</p>
      <h2 className="mt-2 text-xl font-semibold text-emerald-950">{section.classSectionName}</h2>
      <p className="mt-1 text-sm text-emerald-800">School year {section.schoolYear} · Adviser {section.adviserName}</p>
    </article>
  );
}
