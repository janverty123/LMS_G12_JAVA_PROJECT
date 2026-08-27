import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "@/components/shared/AppShell";
import { RequireAuth } from "@/components/shared/RequireAuth";
import { AuthProvider } from "@/context/AuthProvider";
import { useAuth } from "@/hooks/useAuth";
import { AuthPage } from "@/pages/AuthPage";
import { StudentClassPage } from "@/pages/StudentClassPage";
import { StudentMaterialsPage } from "@/pages/StudentMaterialsPage";
import { StudentSubjectsPage } from "@/pages/StudentSubjectsPage";
import { TeacherClassSectionsPage } from "@/pages/TeacherClassSectionsPage";
import { TeacherMaterialsPage } from "@/pages/TeacherMaterialsPage";
import { TeacherSubjectsPage } from "@/pages/TeacherSubjectsPage";

function HomeRedirect() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return (
    <Navigate
      to={user.role === "TEACHER" ? "/teacher/class-sections" : "/student/class-section"}
      replace
    />
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<AuthPage mode="login" />} />
          <Route path="/register" element={<AuthPage mode="register" />} />

          <Route element={<RequireAuth role="TEACHER" />}>
            <Route element={<AppShell />}>
              <Route path="/teacher/class-sections" element={<TeacherClassSectionsPage />} />
              <Route
                path="/teacher/class-sections/:classSectionId/subjects/:subjectId/materials"
                element={<TeacherMaterialsPage />}
              />
              <Route path="/teacher/subjects" element={<TeacherSubjectsPage />} />
            </Route>
          </Route>

          <Route element={<RequireAuth role="STUDENT" />}>
            <Route element={<AppShell />}>
              <Route path="/student/class-section" element={<StudentClassPage />} />
              <Route path="/student/subjects" element={<StudentSubjectsPage />} />
              <Route
                path="/student/subjects/:subjectId/materials"
                element={<StudentMaterialsPage />}
              />
            </Route>
          </Route>

          <Route path="*" element={<HomeRedirect />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
