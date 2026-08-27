import { Link, useParams } from "react-router-dom";
import { TeacherMaterialsPanel } from "@/components/teacher/TeacherMaterialsPanel";

export function TeacherMaterialsPage() {
  const { classSectionId, subjectId } = useParams();
  if (!classSectionId || !subjectId) {
    return <p className="text-sm text-rose-700">Class section or subject is missing.</p>;
  }

  return (
    <div>
      <Link to="/teacher/class-sections" className="text-sm font-medium text-amber-700">
        ← Back to class sections
      </Link>
      <h1 className="mt-3 text-3xl font-semibold tracking-tight">Learning materials</h1>
      <p className="mt-1 text-slate-500">Upload resources and provide approved students with read-only access.</p>
      <div className="mt-6">
        <TeacherMaterialsPanel classSectionId={classSectionId} subjectId={subjectId} />
      </div>
    </div>
  );
}
