import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "@/components/shared/AppShell";
import { RequireAuth } from "@/components/shared/RequireAuth";
import { AuthProvider } from "@/context/AuthProvider";
import { ThemeProvider } from "@/context/ThemeProvider";
import { useAuth } from "@/hooks/useAuth";
import { AuthPage } from "@/pages/AuthPage";
import { StudentClassPage } from "@/pages/StudentClassPage";
import { StudentActivitiesPage } from "@/pages/StudentActivitiesPage";
import { StudentGradesPage } from "@/pages/StudentGradesPage";
import { StudentProgressPage } from "@/pages/StudentProgressPage";
import { NotificationsPage } from "@/pages/NotificationsPage";
import { StudentAnnouncementsPage } from "@/pages/StudentAnnouncementsPage";
import { StudentMaterialsPage } from "@/pages/StudentMaterialsPage";
import { StudentSubjectsPage } from "@/pages/StudentSubjectsPage";
import { TeacherClassSectionsPage } from "@/pages/TeacherClassSectionsPage";
import { TeacherSubjectsPage } from "@/pages/TeacherSubjectsPage";
import { LandingPage } from "@/pages/LandingPage";
import { TeacherDashboardPage } from "@/pages/TeacherDashboardPage";
import { StudentDashboardPage } from "@/pages/StudentDashboardPage";
import { TeacherAnnouncementsPage } from "@/pages/TeacherAnnouncementsPage";
import { RegistrationRolePage } from "@/pages/RegistrationRolePage";

function HomeRedirect() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return (
    <Navigate
      to={user.role === "TEACHER" ? "/teacher/dashboard" : "/student/dashboard"}
      replace
    />
  );
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<AuthPage mode="login" />} />
            <Route path="/register" element={<RegistrationRolePage />} />
            <Route
              path="/register/teacher"
              element={<AuthPage mode="register" registrationRole="TEACHER" />}
            />
            <Route
              path="/register/student"
              element={<AuthPage mode="register" registrationRole="STUDENT" />}
            />

            <Route element={<RequireAuth role="TEACHER" />}>
              <Route element={<AppShell />}>
                <Route
                  path="/teacher/dashboard"
                  element={<TeacherDashboardPage />}
                />
                <Route
                  path="/teacher/class-sections"
                  element={<TeacherClassSectionsPage />}
                />
                <Route
                  path="/teacher/subjects"
                  element={<TeacherSubjectsPage />}
                />
                <Route
                  path="/teacher/announcements"
                  element={<TeacherAnnouncementsPage />}
                />
                <Route
                  path="/teacher/notifications"
                  element={<NotificationsPage />}
                />
              </Route>
            </Route>

            <Route element={<RequireAuth role="STUDENT" />}>
              <Route element={<AppShell />}>
                <Route
                  path="/student/dashboard"
                  element={<StudentDashboardPage />}
                />
                <Route
                  path="/student/class-section"
                  element={<StudentClassPage />}
                />
                <Route
                  path="/student/subjects"
                  element={<StudentSubjectsPage />}
                />
                <Route
                  path="/student/subjects/:subjectId/materials"
                  element={<StudentMaterialsPage />}
                />
                <Route
                  path="/student/subjects/:subjectId/activities"
                  element={<StudentActivitiesPage />}
                />
                <Route
                  path="/student/subjects/:subjectId/grades"
                  element={<StudentGradesPage />}
                />
                <Route
                  path="/student/subjects/:subjectId/progress"
                  element={<StudentProgressPage />}
                />
                <Route
                  path="/student/announcements"
                  element={<StudentAnnouncementsPage />}
                />
                <Route
                  path="/student/notifications"
                  element={<NotificationsPage />}
                />
              </Route>
            </Route>

            <Route path="*" element={<HomeRedirect />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}
