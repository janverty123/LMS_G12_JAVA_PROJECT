import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import { notificationService } from "@/services/notification.service";
import { subjectService } from "@/services/subject.service";
import type { Announcement, AppNotification, SubjectResponse } from "@/types";

export function StudentDashboardPage() {
  const { user } = useAuth();
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [announcements, setAnnouncements] = useState<Announcement[]>([]);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  useEffect(() => { void Promise.all([subjectService.listStudentSubjects(), notificationService.studentAnnouncements(), notificationService.notifications()]).then(([nextSubjects, nextAnnouncements, nextNotifications]) => { setSubjects(nextSubjects); setAnnouncements(nextAnnouncements.slice(0, 4)); setNotifications(nextNotifications.slice(0, 4)); }); }, []);
  return <div><div className="page-heading"><h1>Hello, {user?.name}!</h1><p>Your class activity at a glance.</p></div><div className="mt-8 grid gap-5 lg:grid-cols-2"><section className="panel min-h-56"><h2 className="font-semibold">Announcements</h2>{announcements.length ? <ul className="mt-5 space-y-3">{announcements.map((item) => <li key={item.id}><strong>{item.title}</strong><p className="text-sm text-[var(--text-muted)]">{item.content}</p></li>)}</ul> : <p className="mt-16 text-center text-[var(--text-muted)]">No Announcements.</p>}</section><section className="panel min-h-56"><h2 className="font-semibold">Recent Notification</h2>{notifications.length ? <ul className="mt-5 space-y-3 text-sm">{notifications.map((item) => <li key={item.id}>{item.message}</li>)}</ul> : <p className="mt-16 text-center text-[var(--text-muted)]">No Notifications</p>}</section></div><section className="mt-6"><h2 className="text-2xl">My Subjects</h2><div className="mt-4 flex flex-wrap gap-3">{subjects.map((subject) => <Link key={subject.id} className="button button--outline" to={`/student/subjects/${subject.id}/materials`}>{subject.name}</Link>)}</div></section></div>;
}
