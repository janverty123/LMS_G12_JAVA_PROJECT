import type { ActivityType } from "./activity";

export interface GradeConfiguration {
  classSectionId: string;
  subjectId: string;
  writtenActivityWeight: number;
  performanceTaskWeight: number;
  testWeight: number;
}

export interface GradebookActivity {
  id: string;
  title: string;
  type: ActivityType;
  perfectScore: number;
}

export interface StudentGrade {
  studentId: string;
  studentName: string;
  studentLrn: string;
  scores: { activityId: string; score: number | null; perfectScore: number }[];
  writtenActivityAverage: number;
  performanceTaskAverage: number;
  testAverage: number;
  finalGrade: number;
}

export interface GradebookResponse {
  configuration: GradeConfiguration;
  activities: GradebookActivity[];
  students: StudentGrade[];
}
