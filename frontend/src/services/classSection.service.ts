import { api } from "./api";
import type {
  ClassEnrollmentRequestResponse,
  ClassSectionMemberResponse,
  ClassSectionRequest,
  ClassSectionResponse,
  StudentClassSectionResponse,
} from "@/types";

export const classSectionService = {
  async listTeacherClassSections() {
    return (await api.get<ClassSectionResponse[]>("/teacher/class-sections")).data;
  },

  async createClassSection(request: ClassSectionRequest) {
    return (await api.post<ClassSectionResponse>("/teacher/class-sections", request)).data;
  },

  async updateClassSection(id: string, request: ClassSectionRequest) {
    return (await api.put<ClassSectionResponse>(`/teacher/class-sections/${id}`, request)).data;
  },

  async deleteClassSection(id: string) {
    await api.delete(`/teacher/class-sections/${id}`);
  },

  async listEnrollmentRequests(classSectionId: string) {
    return (
      await api.get<ClassEnrollmentRequestResponse[]>(
        `/teacher/class-sections/${classSectionId}/join-requests`
      )
    ).data;
  },

  async approveEnrollmentRequest(id: string) {
    return (
      await api.put<ClassEnrollmentRequestResponse>(
        `/teacher/class-enrollment-requests/${id}/approve`
      )
    ).data;
  },

  async declineEnrollmentRequest(id: string) {
    return (
      await api.put<ClassEnrollmentRequestResponse>(
        `/teacher/class-enrollment-requests/${id}/decline`
      )
    ).data;
  },

  async listMembers(classSectionId: string) {
    return (
      await api.get<ClassSectionMemberResponse[]>(
        `/teacher/class-sections/${classSectionId}/students`
      )
    ).data;
  },

  async removeMember(classSectionId: string, studentId: string) {
    await api.delete(
      `/teacher/class-sections/${classSectionId}/students/${studentId}`
    );
  },

  async createEnrollmentRequest(classCode: string) {
    return (
      await api.post<ClassEnrollmentRequestResponse>(
        "/students/me/class-join-requests",
        { classCode }
      )
    ).data;
  },

  async listOwnEnrollmentRequests() {
    return (
      await api.get<ClassEnrollmentRequestResponse[]>(
        "/students/me/class-join-requests"
      )
    ).data;
  },

  async getOwnClassSection() {
    return (
      await api.get<StudentClassSectionResponse>("/students/me/class-section")
    ).data;
  },
};
