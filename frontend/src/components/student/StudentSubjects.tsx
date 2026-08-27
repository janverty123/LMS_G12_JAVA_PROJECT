import { Link } from "react-router-dom";
import { EmptyState } from "@/components/shared/EmptyState";
import type { SubjectResponse } from "@/types";

export function StudentSubjects({ subjects }: { subjects: SubjectResponse[] }) {
  if (!subjects.length) {
    return <EmptyState title="No approved subjects" detail="Subjects appear after your class adviser and subject teacher approve the link." />;
  }

  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {subjects.map((subject) => (
        <article key={subject.id} className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="font-semibold">{subject.name}</h2>
          <p className="mt-1 text-sm text-slate-500">Teacher {subject.subjectTeacherName}</p>
          <Link
            to={`/student/subjects/${subject.id}/materials`}
            className="mt-4 inline-block text-sm font-semibold text-amber-700"
          >
            View materials →
          </Link>
        </article>
      ))}
    </div>
  );
}
