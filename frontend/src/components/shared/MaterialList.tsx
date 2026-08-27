import { useState } from "react";
import { ApiAlert } from "./ApiAlert";
import { EmptyState } from "./EmptyState";
import { getErrorMessage } from "@/services/errors";
import type {
  LearningMaterialResponse,
  MaterialDownloadResponse,
} from "@/types";

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function openDownload(download: MaterialDownloadResponse) {
  const link = document.createElement("a");
  link.href = download.url;
  link.target = "_blank";
  link.rel = "noopener noreferrer";
  link.download = download.fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
}

export function MaterialList({
  materials,
  getDownload,
}: {
  materials: LearningMaterialResponse[];
  getDownload: (materialId: string) => Promise<MaterialDownloadResponse>;
}) {
  const [downloadingId, setDownloadingId] = useState("");
  const [error, setError] = useState("");

  const download = async (materialId: string) => {
    setDownloadingId(materialId);
    setError("");
    try {
      openDownload(await getDownload(materialId));
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setDownloadingId("");
    }
  };

  if (!materials.length) {
    return (
      <EmptyState
        title="No learning materials"
        detail="Completed uploads will appear here."
      />
    );
  }

  return (
    <div>
      {error && <div className="mb-3"><ApiAlert message={error} /></div>}
      <div className="grid gap-3 sm:grid-cols-2">
        {materials.map((material) => (
          <article
            key={material.id}
            className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
          >
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <h3 className="font-semibold text-slate-900">{material.title}</h3>
                <p className="mt-1 truncate text-sm text-slate-500">
                  {material.fileName} · {formatBytes(material.fileSizeBytes)}
                </p>
              </div>
              <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium uppercase text-slate-600">
                {material.fileName.split(".").pop() ?? "file"}
              </span>
            </div>
            {material.description && (
              <p className="mt-3 text-sm text-slate-600">{material.description}</p>
            )}
            <p className="mt-3 text-xs text-slate-400">
              Uploaded by {material.uploadedBy} · {new Date(material.completedAt).toLocaleDateString()}
            </p>
            <button
              type="button"
              disabled={Boolean(downloadingId)}
              onClick={() => download(material.id)}
              className="mt-4 rounded-lg bg-slate-900 px-3 py-2 text-sm font-semibold text-white disabled:opacity-60"
            >
              {downloadingId === material.id ? "Preparing…" : "Download"}
            </button>
          </article>
        ))}
      </div>
    </div>
  );
}
