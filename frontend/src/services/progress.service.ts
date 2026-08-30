import { api } from "./api";
import type { StudentProgress } from "@/types";

export const progressService = {
  async configure(classId: string, subjectId: string,
    onTrackMinimum: number, needsAttentionMinimum: number) {
    await api.put(`/teacher/class-sections/${classId}/subjects/${subjectId}/progress/configuration`,
      { onTrackMinimum, needsAttentionMinimum });
  },
  async dashboard(classId: string, subjectId: string) {
    return (await api.get<StudentProgress[]>(
      `/teacher/class-sections/${classId}/subjects/${subjectId}/progress`)).data;
  },
  async own(subjectId: string) {
    return (await api.get<StudentProgress>(
      `/students/me/subjects/${subjectId}/progress`)).data;
  },
};
