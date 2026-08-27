import { useCallback, useEffect, useState } from "react";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { LoadingState } from "@/components/shared/LoadingState";
import { MaterialList } from "@/components/shared/MaterialList";
import { MaterialUploadForm } from "./MaterialUploadForm";
import { getErrorMessage } from "@/services/errors";
import { materialService } from "@/services/material.service";
import type { LearningMaterialResponse } from "@/types";

export function TeacherMaterialsPanel({
  classSectionId,
  subjectId,
}: {
  classSectionId: string;
  subjectId: string;
}) {
  const [materials, setMaterials] = useState<LearningMaterialResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setMaterials(await materialService.listTeacherMaterials(
        classSectionId,
        subjectId
      ));
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [classSectionId, subjectId]);

  useEffect(() => { void load(); }, [load]);

  return (
    <div className="space-y-5">
      <MaterialUploadForm
        classSectionId={classSectionId}
        subjectId={subjectId}
        onUploaded={(material) => setMaterials((current) => [material, ...current])}
      />
      {error && <ApiAlert message={error} />}
      {loading ? (
        <LoadingState label="Loading learning materials…" />
      ) : (
        <MaterialList
          materials={materials}
          getDownload={materialService.getTeacherDownload}
        />
      )}
    </div>
  );
}
