import { api } from "./api";
import type { GradebookResponse, GradeConfiguration, StudentGrade } from "@/types";

export const gradeService = {
  async configure(classId: string, subjectId: string,
    writtenActivityWeight: number, performanceTaskWeight: number, testWeight: number) {
    return (await api.put<GradeConfiguration>(
      `/teacher/class-sections/${classId}/subjects/${subjectId}/grade-configuration`,
      { writtenActivityWeight, performanceTaskWeight, testWeight }
    )).data;
  },

  async gradebook(classId: string, subjectId: string) {
    return (await api.get<GradebookResponse>(
      `/teacher/class-sections/${classId}/subjects/${subjectId}/gradebook`
    )).data;
  },

  async exportGradebook(classId: string, subjectId: string) {
    const response = await api.get<Blob>(
      `/teacher/class-sections/${classId}/subjects/${subjectId}/gradebook/export`,
      { responseType: "blob" }
    );
    const url = URL.createObjectURL(response.data);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "gradebook.xlsx";
    anchor.click();
    URL.revokeObjectURL(url);
  },

  async studentGrades(subjectId: string) {
    return (await api.get<StudentGrade>(`/students/me/subjects/${subjectId}/grades`)).data;
  },
};
