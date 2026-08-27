import { useState } from "react";
import { Link } from "react-router-dom";
import { EmptyState } from "@/components/shared/EmptyState";
import { TeacherMaterialsPanel } from "./TeacherMaterialsPanel";
import type { ClassSubjectLinkResponse } from "@/types";

export function ClassSectionMaterialsTab({
  classSectionId,
  links,
}: {
  classSectionId: string;
  links: ClassSubjectLinkResponse[];
}) {
  const approvedLinks = links.filter((link) => link.status === "APPROVED");
  const [firstLink] = approvedLinks;

  if (!firstLink) {
    return (
      <EmptyState
        title="No subjects available"
        detail="Materials can be uploaded after a subject-link request is approved."
      />
    );
  }

  return (
    <MaterialSubjectPicker
      classSectionId={classSectionId}
      approvedLinks={approvedLinks}
      initialSubjectId={firstLink.subjectId}
    />
  );
}

function MaterialSubjectPicker({
  classSectionId,
  approvedLinks,
  initialSubjectId,
}: {
  classSectionId: string;
  approvedLinks: ClassSubjectLinkResponse[];
  initialSubjectId: string;
}) {
  const [subjectId, setSubjectId] = useState(initialSubjectId);

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <label className="text-sm font-medium text-slate-700">
          Subject
          <select
            className="mt-1 block min-w-64 rounded-lg border border-slate-300 bg-white px-3 py-2"
            value={subjectId}
            onChange={(event) => setSubjectId(event.target.value)}
          >
            {approvedLinks.map((link) => (
              <option key={link.id} value={link.subjectId}>{link.subjectName}</option>
            ))}
          </select>
        </label>
        <Link
          to={`/teacher/class-sections/${classSectionId}/subjects/${subjectId}/materials`}
          className="text-sm font-medium text-amber-700"
        >
          Open full materials page →
        </Link>
      </div>
      <p className="text-sm text-slate-500">
        Uploads are restricted to the teacher who owns the selected subject.
      </p>
      <TeacherMaterialsPanel
        key={`${classSectionId}-${subjectId}`}
        classSectionId={classSectionId}
        subjectId={subjectId}
      />
    </div>
  );
}
