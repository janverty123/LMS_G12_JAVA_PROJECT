import { api } from "./api";
import type {
  ActivityFileDownloadResponse,
  ActivityFileResponse,
  ActivityFileUploadResponse,
  ActivityResponse,
  ActivitySubmissionResponse,
  MaterialUploadProgress,
  SaveActivityRequest,
  ScoreProposalResponse,
} from "@/types";
import { resolveContentType, uploadMultipartParts } from "./material.service";

export const activityService = {
  async listTeacherActivities(classSectionId: string, subjectId: string) {
    return (await api.get<ActivityResponse[]>(
      `/teacher/class-sections/${classSectionId}/subjects/${subjectId}/activities`
    )).data;
  },

  async createActivity(
    classSectionId: string,
    subjectId: string,
    request: SaveActivityRequest
  ) {
    return (await api.post<ActivityResponse>(
      `/teacher/class-sections/${classSectionId}/subjects/${subjectId}/activities`,
      request
    )).data;
  },

  async updateActivity(id: string, request: SaveActivityRequest) {
    return (await api.put<ActivityResponse>(`/teacher/activities/${id}`, request)).data;
  },

  async deleteActivity(id: string) {
    await api.delete(`/teacher/activities/${id}`);
  },

  async listStudentActivities(subjectId: string) {
    return (await api.get<ActivityResponse[]>(
      `/students/me/subjects/${subjectId}/activities`
    )).data;
  },

  async uploadTeacherAttachment(
    activityId: string,
    file: File,
    onProgress?: (progress: MaterialUploadProgress) => void
  ) {
    const initialized = (await api.post<ActivityFileUploadResponse>(
      `/teacher/activities/${activityId}/attachments/uploads/initialize`,
      { fileName: file.name, totalSizeBytes: file.size, contentType: resolveContentType(file) }
    )).data;
    const parts = await uploadMultipartParts(file, initialized, undefined, onProgress);
    return (await api.post<ActivityFileResponse>(
      `/teacher/activity-files/${initialized.fileId}/attachments/uploads/complete`,
      { uploadId: initialized.uploadId, fileKey: initialized.fileKey, parts }
    )).data;
  },

  async uploadStudentSubmission(
    activityId: string,
    file: File,
    onProgress?: (progress: MaterialUploadProgress) => void
  ) {
    const initialized = (await api.post<ActivityFileUploadResponse>(
      `/students/me/activities/${activityId}/submission/uploads/initialize`,
      { fileName: file.name, totalSizeBytes: file.size, contentType: resolveContentType(file) }
    )).data;
    const parts = await uploadMultipartParts(file, initialized, undefined, onProgress);
    return (await api.post<ActivitySubmissionResponse>(
      `/students/me/activity-files/${initialized.fileId}/submission/uploads/complete`,
      { uploadId: initialized.uploadId, fileKey: initialized.fileKey, parts }
    )).data;
  },

  async listTeacherAttachments(activityId: string) {
    return (await api.get<ActivityFileResponse[]>(
      `/teacher/activities/${activityId}/attachments`
    )).data;
  },

  async listStudentAttachments(activityId: string) {
    return (await api.get<ActivityFileResponse[]>(
      `/students/me/activities/${activityId}/attachments`
    )).data;
  },

  async getOwnSubmission(activityId: string) {
    return (await api.get<ActivitySubmissionResponse>(
      `/students/me/activities/${activityId}/submission`
    )).data;
  },

  async listSubmissions(activityId: string) {
    return (await api.get<ActivitySubmissionResponse[]>(
      `/teacher/activities/${activityId}/submissions`
    )).data;
  },

  async scoreSubmission(submissionId: string, score: number) {
    return (await api.put<ActivitySubmissionResponse>(
      `/teacher/activity-submissions/${submissionId}/score`, { score }
    )).data;
  },

  async downloadFile(fileId: string, student: boolean) {
    const prefix = student ? "/students/me" : "/teacher";
    const response = (await api.get<ActivityFileDownloadResponse>(
      `${prefix}/activity-files/${fileId}/download`
    )).data;
    window.open(response.url, "_blank", "noopener,noreferrer");
  },

  async uploadScoreProposal(activityId: string, reportedScore: number, file: File,
    onProgress?: (progress: MaterialUploadProgress) => void) {
    const initialized = (await api.post<ActivityFileUploadResponse>(
      `/students/me/activities/${activityId}/score-proposal/uploads/initialize`,
      { reportedScore, fileName: file.name, totalSizeBytes: file.size,
        contentType: resolveContentType(file) }
    )).data;
    const parts = await uploadMultipartParts(file, initialized, undefined, onProgress);
    return (await api.post<ScoreProposalResponse>(
      `/students/me/score-proposal-files/${initialized.fileId}/uploads/complete`,
      { uploadId: initialized.uploadId, fileKey: initialized.fileKey, parts }
    )).data;
  },

  async getOwnScoreProposal(activityId: string) {
    const response = await api.get<ScoreProposalResponse | undefined>(
      `/students/me/activities/${activityId}/score-proposal`
    );
    return response.data;
  },

  async listScoreProposals(activityId: string) {
    return (await api.get<ScoreProposalResponse[]>(
      `/teacher/activities/${activityId}/score-proposals`
    )).data;
  },

  async approveScoreProposal(id: string, approvedScore?: number) {
    return (await api.post<ScoreProposalResponse>(
      `/teacher/score-proposals/${id}/approve`, { approvedScore }
    )).data;
  },

  async rejectScoreProposal(id: string) {
    return (await api.post<ScoreProposalResponse>(
      `/teacher/score-proposals/${id}/reject`
    )).data;
  },
};
