import { EmptyState } from "@/components/shared/EmptyState";
import type { StudentClassSectionResponse } from "@/types";

export function StudentClassSection({
  section,
}: {
  section: StudentClassSectionResponse | null;
}) {
  if (!section) {
    return (
      <EmptyState
        title="No approved class section"
        detail="Submit a class code below and wait for the adviser to approve it."
      />
    );
  }

  return (
    <article className="approved-card rounded-xl border p-5">
      <p className="text-sm font-semibold uppercase tracking-wide">
        Approved class
      </p>
      <h2 className="approved-card__title mt-2 text-xl font-semibold">
        {section.classSectionName}
      </h2>
      <p className="approved-card__detail mt-1 text-sm">
        School year {section.schoolYear} · Adviser {section.adviserName}
      </p>
    </article>
  );
}
