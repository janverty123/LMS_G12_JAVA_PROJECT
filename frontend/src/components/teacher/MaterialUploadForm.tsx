import { useRef, useState, type FormEvent } from "react";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { getErrorMessage } from "@/services/errors";
import { materialService } from "@/services/material.service";
import type {
  LearningMaterialResponse,
  MaterialUploadProgress,
} from "@/types";

const ACCEPTED_TYPES = ".pdf,.doc,.docx,.ppt,.pptx";

export function MaterialUploadForm({
  classSectionId,
  subjectId,
  onUploaded,
}: {
  classSectionId: string;
  subjectId: string;
  onUploaded: (material: LearningMaterialResponse) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const abortRef = useRef<AbortController | undefined>(undefined);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [file, setFile] = useState<File>();
  const [progress, setProgress] = useState<MaterialUploadProgress>();
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!file) {
      setError("Choose a PDF, Word, or PowerPoint file.");
      return;
    }

    const controller = new AbortController();
    abortRef.current = controller;
    setBusy(true);
    setError("");
    setSuccess("");
    setProgress(undefined);
    try {
      const material = await materialService.uploadLearningMaterial({
        classSectionId,
        subjectId,
        title,
        description: description || undefined,
        file,
        signal: controller.signal,
        onProgress: setProgress,
      });
      onUploaded(material);
      setTitle("");
      setDescription("");
      setFile(undefined);
      setSuccess("Learning material uploaded successfully.");
      if (inputRef.current) inputRef.current.value = "";
    } catch (requestError) {
      setError(
        requestError instanceof DOMException && requestError.name === "AbortError"
          ? "Upload cancelled."
          : getErrorMessage(requestError)
      );
    } finally {
      abortRef.current = undefined;
      setBusy(false);
    }
  };

  return (
    <form
      onSubmit={submit}
      className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
    >
      <h3 className="font-semibold">Upload learning material</h3>
      <p className="mt-1 text-sm text-slate-500">
        Files are sent directly to MinIO in resilient 5 MiB parts. Each part gets up to three attempts.
      </p>

      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <label className="text-sm font-medium text-slate-700">
          Title
          <input
            required
            maxLength={150}
            disabled={busy}
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />
        </label>
        <label className="text-sm font-medium text-slate-700">
          File
          <input
            ref={inputRef}
            required
            type="file"
            accept={ACCEPTED_TYPES}
            disabled={busy}
            className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
            onChange={(event) => setFile(event.target.files?.[0])}
          />
        </label>
      </div>
      <label className="mt-4 block text-sm font-medium text-slate-700">
        Description <span className="font-normal text-slate-400">(optional)</span>
        <textarea
          maxLength={2000}
          rows={3}
          disabled={busy}
          className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
        />
      </label>

      {progress && (
        <div className="mt-4" aria-live="polite">
          <div className="flex justify-between text-sm text-slate-600">
            <span>Part {progress.currentPart} of {progress.totalParts}</span>
            <span>{progress.percentage}%</span>
          </div>
          <div className="mt-2 h-2 overflow-hidden rounded-full bg-slate-200">
            <div
              className="h-full rounded-full bg-[var(--accent)] transition-all"
              style={{ width: `${progress.percentage}%` }}
            />
          </div>
        </div>
      )}

      {error && <div className="mt-4"><ApiAlert message={error} /></div>}
      {success && <div className="mt-4"><ApiAlert message={success} tone="success" /></div>}

      <div className="mt-4 flex gap-3">
        <button
          disabled={busy}
          className="rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
        >
          {busy ? "Uploading…" : "Upload material"}
        </button>
        {busy && (
          <button
            type="button"
            onClick={() => abortRef.current?.abort()}
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
