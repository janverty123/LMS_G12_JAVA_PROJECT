import { api } from "./api";
import type {
  CompleteMaterialUploadRequest,
  InitMaterialUploadRequest,
  InitMaterialUploadResponse,
  LearningMaterialResponse,
  MaterialDownloadResponse,
  MaterialUploadProgress,
  UploadLearningMaterialOptions,
} from "@/types";

export const MATERIAL_CHUNK_SIZE_BYTES = 5 * 1024 * 1024;
const MAX_PART_ATTEMPTS = 3;
const CONTENT_TYPES_BY_EXTENSION: Record<string, string> = {
  doc: "application/msword",
  docx: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  pdf: "application/pdf",
  ppt: "application/vnd.ms-powerpoint",
  pptx: "application/vnd.openxmlformats-officedocument.presentationml.presentation",
};

class PartUploadError extends Error {
  constructor(
    message: string,
    readonly status: number
  ) {
    super(message);
  }
}

export function splitFileIntoChunks(
  file: Blob,
  chunkSize = MATERIAL_CHUNK_SIZE_BYTES
) {
  if (chunkSize <= 0) {
    throw new Error("Chunk size must be greater than zero.");
  }

  const chunks: Blob[] = [];
  for (let offset = 0; offset < file.size; offset += chunkSize) {
    chunks.push(file.slice(offset, Math.min(offset + chunkSize, file.size)));
  }
  return chunks;
}

function resolveContentType(file: File) {
  if (file.type) return file.type;
  const extension = file.name.split(".").pop()?.toLowerCase() ?? "";
  return CONTENT_TYPES_BY_EXTENSION[extension] ?? "application/octet-stream";
}

function uploadPartOnce(
  url: string,
  chunk: Blob,
  onProgress: (uploadedBytes: number) => void,
  signal?: AbortSignal
) {
  return new Promise<string>((resolve, reject) => {
    const request = new XMLHttpRequest();
    let settled = false;

    const cleanup = () => signal?.removeEventListener("abort", abort);
    const finish = (callback: () => void) => {
      if (settled) return;
      settled = true;
      cleanup();
      callback();
    };
    const abort = () => {
      request.abort();
      finish(() => reject(new DOMException("Upload cancelled.", "AbortError")));
    };

    if (signal?.aborted) {
      abort();
      return;
    }
    signal?.addEventListener("abort", abort, { once: true });

    request.open("PUT", url);
    request.upload.onprogress = (event) => {
      if (event.lengthComputable) onProgress(event.loaded);
    };
    request.onload = () => {
      if (request.status < 200 || request.status >= 300) {
        finish(() => reject(new PartUploadError(
          `Part upload failed with status ${request.status}.`,
          request.status
        )));
        return;
      }

      const eTag = request.getResponseHeader("ETag");
      if (!eTag) {
        finish(() => reject(new PartUploadError(
          "MinIO did not expose an ETag for the uploaded part.",
          request.status
        )));
        return;
      }
      finish(() => resolve(eTag));
    };
    request.onerror = () => finish(() => reject(
      new PartUploadError("A network error interrupted the part upload.", 0)
    ));
    request.onabort = () => finish(() => reject(
      new DOMException("Upload cancelled.", "AbortError")
    ));
    request.send(chunk);
  });
}

function isRetryable(error: unknown) {
  return error instanceof PartUploadError
    && (error.status === 0
      || error.status === 408
      || error.status === 429
      || error.status >= 500);
}

function waitBeforeRetry(attempt: number, signal?: AbortSignal) {
  return new Promise<void>((resolve, reject) => {
    if (signal?.aborted) {
      reject(new DOMException("Upload cancelled.", "AbortError"));
      return;
    }

    const abort = () => {
      window.clearTimeout(timeout);
      reject(new DOMException("Upload cancelled.", "AbortError"));
    };
    const timeout = window.setTimeout(() => {
      signal?.removeEventListener("abort", abort);
      resolve();
    }, 250 * 2 ** (attempt - 1));
    signal?.addEventListener("abort", abort, { once: true });
  });
}

async function uploadPartWithRetry(
  url: string,
  chunk: Blob,
  onProgress: (uploadedBytes: number) => void,
  signal?: AbortSignal
) {
  let lastError: unknown;
  for (let attempt = 1; attempt <= MAX_PART_ATTEMPTS; attempt++) {
    try {
      return await uploadPartOnce(url, chunk, onProgress, signal);
    } catch (error) {
      lastError = error;
      if (error instanceof DOMException && error.name === "AbortError") throw error;
      if (!isRetryable(error) || attempt === MAX_PART_ATTEMPTS) throw error;
      await waitBeforeRetry(attempt, signal);
    }
  }
  throw lastError;
}

function reportProgress(
  callback: UploadLearningMaterialOptions["onProgress"],
  uploadedBytes: number,
  totalBytes: number,
  currentPart: number,
  totalParts: number
) {
  const progress: MaterialUploadProgress = {
    uploadedBytes,
    totalBytes,
    percentage: totalBytes === 0
      ? 0
      : Math.min(100, Math.round((uploadedBytes / totalBytes) * 100)),
    currentPart,
    totalParts,
  };
  callback?.(progress);
}

export const materialService = {
  async initUpload(
    classSectionId: string,
    subjectId: string,
    request: InitMaterialUploadRequest
  ) {
    return (
      await api.post<InitMaterialUploadResponse>(
        `/teacher/class-sections/${classSectionId}/subjects/${subjectId}/materials/uploads/initialize`,
        request
      )
    ).data;
  },

  async completeUpload(
    materialId: string,
    request: CompleteMaterialUploadRequest
  ) {
    return (
      await api.post<LearningMaterialResponse>(
        `/teacher/materials/${materialId}/uploads/complete`,
        request
      )
    ).data;
  },

  async uploadLearningMaterial(options: UploadLearningMaterialOptions) {
    const initialized = await this.initUpload(
      options.classSectionId,
      options.subjectId,
      {
        title: options.title,
        description: options.description,
        fileName: options.file.name,
        totalSizeBytes: options.file.size,
        contentType: resolveContentType(options.file),
      }
    );

    const chunks = splitFileIntoChunks(options.file, initialized.chunkSize);
    const urls = [...initialized.presignedUrls]
      .sort((left, right) => left.partNumber - right.partNumber);
    if (chunks.length !== initialized.totalParts || urls.length !== chunks.length) {
      throw new Error("Upload initialization returned an invalid part plan.");
    }

    let completedBytes = 0;
    const parts = [];
    reportProgress(options.onProgress, 0, options.file.size, 1, chunks.length);

    for (let index = 0; index < chunks.length; index++) {
      const chunk = chunks[index];
      const part = urls[index];
      if (part.partNumber !== index + 1) {
        throw new Error("Upload URLs are not numbered consecutively.");
      }

      const eTag = await uploadPartWithRetry(
        part.url,
        chunk,
        (partBytes) => reportProgress(
          options.onProgress,
          completedBytes + partBytes,
          options.file.size,
          part.partNumber,
          chunks.length
        ),
        options.signal
      );
      completedBytes += chunk.size;
      parts.push({ partNumber: part.partNumber, eTag });
      reportProgress(
        options.onProgress,
        completedBytes,
        options.file.size,
        part.partNumber,
        chunks.length
      );
    }

    return this.completeUpload(initialized.materialId, {
      uploadId: initialized.uploadId,
      fileKey: initialized.fileKey,
      parts,
    });
  },

  async listTeacherMaterials(classSectionId: string, subjectId: string) {
    return (
      await api.get<LearningMaterialResponse[]>(
        `/teacher/class-sections/${classSectionId}/subjects/${subjectId}/materials`
      )
    ).data;
  },

  async getTeacherDownload(materialId: string) {
    return (
      await api.get<MaterialDownloadResponse>(
        `/teacher/materials/${materialId}/download`
      )
    ).data;
  },

  async listStudentMaterials(subjectId: string) {
    return (
      await api.get<LearningMaterialResponse[]>(
        `/students/me/subjects/${subjectId}/materials`
      )
    ).data;
  },

  async getStudentDownload(materialId: string) {
    return (
      await api.get<MaterialDownloadResponse>(
        `/students/me/materials/${materialId}/download`
      )
    ).data;
  },
};
