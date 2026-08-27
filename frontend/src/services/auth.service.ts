import { api } from "./api";
import type {
  AuthResponse,
  LoginRequest,
  RegisterStudentRequest,
  RegisterTeacherRequest,
} from "@/types";

export const authService = {
  async login(request: LoginRequest) {
    return (await api.post<AuthResponse>("/auth/login", request)).data;
  },

  async registerTeacher(request: RegisterTeacherRequest) {
    return (await api.post<AuthResponse>("/auth/register/teacher", request)).data;
  },

  async registerStudent(request: RegisterStudentRequest) {
    return (await api.post<AuthResponse>("/auth/register/student", request)).data;
  },
};
