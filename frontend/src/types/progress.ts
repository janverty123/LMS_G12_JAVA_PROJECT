export type ProgressStatus = "ON_TRACK" | "NEEDS_ATTENTION" | "AT_RISK";

export interface StudentProgress {
  studentId: string;
  studentName: string;
  completionPercentage: number;
  missingActivityCount: number;
  missingActivities: string[];
  currentGrade: number;
  status: ProgressStatus;
  statusLabel: string;
}
