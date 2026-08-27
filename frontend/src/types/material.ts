export interface InitMaterialUploadRequest {
  title: string;
  description?: string;
  fileName: string;
  totalSizeBytes: number;
  contentType: string;
}

export interface PresignedPartUrl {
  partNumber: number;
  url: string;
}

export interface InitMaterialUploadResponse {
  materialId: string;
  uploadId: string;
  fileKey: string;
  chunkSize: number;
  totalParts: number;
  presignedUrls: PresignedPartUrl[];
}

export interface CompletedMaterialPart {
  partNumber: number;
  eTag: string;
}

export interface CompleteMaterialUploadRequest {
  uploadId: string;
  fileKey: string;
  parts: CompletedMaterialPart[];
}

export interface LearningMaterialResponse {
  id: string;
  classSectionId: string;
  subjectId: string;
  subjectName: string;
  title: string;
  description: string | null;
  fileName: string;
  contentType: string;
  fileSizeBytes: number;
  uploadedBy: string;
  createdAt: string;
  completedAt: string;
}

export interface MaterialDownloadResponse {
  materialId: string;
  fileName: string;
  contentType: string;
  fileSizeBytes: number;
  url: string;
  expiresAt: string;
}

export interface MaterialUploadProgress {
  uploadedBytes: number;
  totalBytes: number;
  percentage: number;
  currentPart: number;
  totalParts: number;
}

export interface UploadLearningMaterialOptions {
  classSectionId: string;
  subjectId: string;
  title: string;
  description?: string;
  file: File;
  signal?: AbortSignal;
  onProgress?: (progress: MaterialUploadProgress) => void;
}
