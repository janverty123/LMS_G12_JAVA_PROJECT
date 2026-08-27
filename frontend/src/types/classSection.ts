export type EnrollmentStatus = "PENDING" | "APPROVED" | "DECLINED";

export interface ClassSectionResponse {
  id: string;
  gradeLevel: string;
  section: string;
  schoolYear: string;
  classCode: string;
  adviserName: string;
}

export interface ClassSectionRequest {
  gradeLevel: string;
  section: string;
  schoolYear: string;
}

export interface ClassEnrollmentRequestResponse {
  id: string;
  studentId: string;
  studentName: string;
  studentLrn: string;
  classSectionId: string;
  classSectionName: string;
  schoolYear: string;
  status: EnrollmentStatus;
  updatedAt: string;
}

export interface ClassSectionMemberResponse {
  studentId: string;
  studentName: string;
  studentLrn: string;
  studentEmail: string;
  status: EnrollmentStatus;
  enrollmentDate: string;
}

export interface StudentClassSectionResponse {
  classSectionId: string;
  classSectionName: string;
  schoolYear: string;
  adviserName: string;
}
