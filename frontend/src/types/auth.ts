export type Role = "TEACHER" | "STUDENT";

export interface AuthResponse {
  token: string;
  userId: string;
  name: string;
  email: string;
  role: Role;
  lrn?: string | null;
  profilePicture?: string | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterTeacherRequest extends LoginRequest {
  name: string;
}

export interface RegisterStudentRequest extends RegisterTeacherRequest {
  lrn: string;
}
