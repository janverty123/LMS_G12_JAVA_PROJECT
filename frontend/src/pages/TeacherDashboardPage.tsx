import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import { classSectionService } from "@/services/classSection.service";
import { notificationService } from "@/services/notification.service";
import { subjectService } from "@/services/subject.service";
import type { AppNotification } from "@/types";

export function TeacherDashboardPage() {
  const { user } = useAuth();
  const [counts, setCounts] = useState({
    classes: 0,
    students: 0,
    requests: 0,
  });
  const [notifications, setNotifications] = useState<AppNotification[]>([]);

  useEffect(() => {
    const load = async () => {
      const [classes, subjects, notices] = await Promise.all([
        classSectionService.listTeacherClassSections(),
        subjectService.listTeacherSubjects(),
        notificationService.notifications(),
      ]);
      const [members, classRequests, subjectRequests] = await Promise.all([
        Promise.all(
          classes.map((item) => classSectionService.listMembers(item.id)),
        ),
        Promise.all(
          classes.map((item) =>
            classSectionService.listEnrollmentRequests(item.id),
          ),
        ),
        Promise.all(
          subjects.map((item) => subjectService.listPendingLinks(item.id)),
        ),
      ]);
      setCounts({
        classes: classes.length,
        students: new Set(members.flat().map((item) => item.studentId)).size,
        requests:
          classRequests.flat().filter((item) => item.status === "PENDING")
            .length + subjectRequests.flat().length,
      });
      setNotifications(notices.slice(0, 5));
    };
    void load();
  }, []);

  return (
    <div>
      <div className="page-heading">
        <h1>Hello, {user?.name}!</h1>
        <p>Here is what is happening across your classes and subjects.</p>
      </div>
      <div className="mt-8 grid gap-5 sm:grid-cols-3">
        {[
          ["Total Classes", counts.classes],
          ["Total Students", counts.students],
          ["Pending Requests", counts.requests],
        ].map(([label, value]) => (
          <div key={label} className="panel dashboard-stat-card">
            <p className="text-sm text-[var(--text-muted)]">{label}</p>
            <p className="dashboard-stat-card__value mt-7 text-3xl">{value}</p>
          </div>
        ))}
      </div>
      <div className="mt-5 grid gap-5 lg:grid-cols-2">
        <section className="panel min-h-56">
          <h2 className="font-semibold">Recent Notifications</h2>
          {notifications.length ? (
            <ul className="mt-5 space-y-3 text-sm">
              {notifications.map((item) => (
                <li className="dashboard-feed-item" key={item.id}>
                  {item.message}
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-16 text-center text-[var(--text-muted)]">
              No Notifications
            </p>
          )}
        </section>
        <section className="panel min-h-56">
          <h2 className="font-semibold">Upcoming Deadlines</h2>
          <p className="mt-16 text-center text-[var(--text-muted)]">
            Open a Subject to review its requirements.
          </p>
        </section>
      </div>
      <div className="mt-6 flex flex-wrap gap-3">
        <Link className="button button--primary" to="/teacher/class-sections">
          OPEN CLASSES
        </Link>
        <Link className="button button--outline" to="/teacher/subjects">
          OPEN SUBJECTS
        </Link>
      </div>
    </div>
  );
}
