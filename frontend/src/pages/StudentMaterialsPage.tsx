import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { LoadingState } from "@/components/shared/LoadingState";
import { MaterialList } from "@/components/shared/MaterialList";
import { getErrorMessage } from "@/services/errors";
import { materialService } from "@/services/material.service";
import type { LearningMaterialResponse } from "@/types";

export function StudentMaterialsPage() {
  const { subjectId } = useParams();
  const [materials, setMaterials] = useState<LearningMaterialResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const load = async () => {
      if (!subjectId) {
        setError("Subject is missing.");
        setLoading(false);
        return;
      }
      try {
        setMaterials(await materialService.listStudentMaterials(subjectId));
      } catch (requestError) {
        setError(getErrorMessage(requestError));
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [subjectId]);

  return (
    <div>
      <Link to="/student/subjects" className="text-sm font-medium text-amber-700">
        ← Back to subjects
      </Link>
      <h1 className="mt-3 text-3xl font-semibold tracking-tight">Learning materials</h1>
      <p className="mt-1 text-slate-500">View and download resources shared with your class.</p>
      {error && <div className="mt-5"><ApiAlert message={error} /></div>}
      <div className="mt-6">
        {loading ? (
          <LoadingState label="Loading learning materials…" />
        ) : (
          <MaterialList
            materials={materials}
            getDownload={materialService.getStudentDownload}
          />
        )}
      </div>
    </div>
  );
}
