export type SubjectLinkStatus = "PENDING" | "APPROVED" | "DECLINED";

export interface SubjectResponse {
  id: string;
  name: string;
  subjectCode: string;
  subjectTeacherName: string;
}

export interface ClassSubjectLinkResponse {
  id: string;
  classSectionId: string;
  classSectionName: string;
  schoolYear: string;
  requestingAdviserName: string;
  subjectId: string;
  subjectName: string;
  status: SubjectLinkStatus;
  createdAt: string;
  updatedAt: string;
}
