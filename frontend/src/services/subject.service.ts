import { api } from "./api";
import type {
  ClassSubjectLinkResponse,
  SubjectResponse,
} from "@/types";

export const subjectService = {
  async listTeacherSubjects() {
    return (await api.get<SubjectResponse[]>("/teacher/subjects")).data;
  },

  async createSubject(name: string) {
    return (await api.post<SubjectResponse>("/teacher/subjects", { name })).data;
  },

  async updateSubject(id: string, name: string) {
    return (await api.put<SubjectResponse>(`/teacher/subjects/${id}`, { name })).data;
  },

  async deleteSubject(id: string) {
    await api.delete(`/teacher/subjects/${id}`);
  },

  async createLinkRequest(classSectionId: string, subjectCode: string) {
    return (
      await api.post<ClassSubjectLinkResponse>(
        `/teacher/class-sections/${classSectionId}/subject-links`,
        { subjectCode }
      )
    ).data;
  },

  async listLinksForClassSection(classSectionId: string) {
    return (
      await api.get<ClassSubjectLinkResponse[]>(
        `/teacher/class-sections/${classSectionId}/subjects`
      )
    ).data;
  },

  async listPendingLinks(subjectId: string) {
    return (
      await api.get<ClassSubjectLinkResponse[]>(
        `/teacher/subjects/${subjectId}/link-requests`
      )
    ).data;
  },

  async listLinksForSubject(subjectId: string) {
    return (
      await api.get<ClassSubjectLinkResponse[]>(
        `/teacher/subjects/${subjectId}/links`
      )
    ).data;
  },

  async approveLink(id: string) {
    return (
      await api.put<ClassSubjectLinkResponse>(
        `/teacher/subject-links/${id}/approve`
      )
    ).data;
  },

  async declineLink(id: string) {
    return (
      await api.put<ClassSubjectLinkResponse>(
        `/teacher/subject-links/${id}/decline`
      )
    ).data;
  },

  async listStudentSubjects() {
    return (await api.get<SubjectResponse[]>("/students/me/subjects")).data;
  },
};
