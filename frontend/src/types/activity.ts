export type ActivityType = "WRITTEN_ACTIVITY" | "PERFORMANCE_TASK" | "TEST";

export interface SaveActivityRequest {
  type: ActivityType;
  title: string;
  perfectScore: number;
  deadline: string;
  instructions: string;
  allowStudentSelfSubmissionScore: boolean;
}

export interface ActivityResponse extends SaveActivityRequest {
  id: string;
  classSectionId: string;
  subjectId: string;
  subjectName: string;
  typeLabel: string;
  createdAt: string;
  updatedAt: string;
}

export type SubmissionStatus = "NOT_SUBMITTED" | "SUBMITTED" | "LATE" | "GRADED";

export interface ActivityFileUploadResponse {
  fileId: string;
  uploadId: string;
  fileKey: string;
  chunkSize: number;
  totalParts: number;
  presignedUrls: { partNumber: number; url: string }[];
}

export interface ActivityFileResponse {
  id: string;
  fileName: string;
  contentType: string;
  fileSizeBytes: number;
  completedAt: string;
}

export interface ActivitySubmissionResponse {
  submissionId: string | null;
  studentId: string;
  studentName: string;
  studentLrn: string;
  status: SubmissionStatus;
  submittedAt: string | null;
  file: ActivityFileResponse | null;
  score: number | null;
  perfectScore: number;
}

export interface ActivityFileDownloadResponse {
  fileId: string;
  fileName: string;
  contentType: string;
  fileSizeBytes: number;
  url: string;
  expiresAt: string;
}

export type ScoreProposalStatus = "PENDING" | "APPROVED" | "REJECTED" | "EDITED_APPROVED";

export interface ScoreProposalResponse {
  id: string;
  activityId: string;
  studentId: string;
  studentName: string;
  studentLrn: string;
  reportedScore: number;
  approvedScore: number | null;
  perfectScore: number;
  status: ScoreProposalStatus;
  proofFile: ActivityFileResponse;
  submittedAt: string;
  reviewedAt: string | null;
}
