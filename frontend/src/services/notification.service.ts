import { api } from "./api";
import type { Announcement, AppNotification } from "@/types";

export const notificationService = {
  async teacherAnnouncements(classId: string) { return (await api.get<Announcement[]>(`/teacher/class-sections/${classId}/announcements`)).data; },
  async studentAnnouncements() { return (await api.get<Announcement[]>("/students/me/announcements")).data; },
  async createAnnouncement(classId: string, title: string, content: string) { return (await api.post<Announcement>(`/teacher/class-sections/${classId}/announcements`, { title, content })).data; },
  async updateAnnouncement(id: string, title: string, content: string) { return (await api.put<Announcement>(`/teacher/announcements/${id}`, { title, content })).data; },
  async deleteAnnouncement(id: string) { await api.delete(`/teacher/announcements/${id}`); },
  async notifications() { return (await api.get<AppNotification[]>("/notifications")).data; },
  async read(id: string) { return (await api.put<AppNotification>(`/notifications/${id}/read`)).data; },
  async readAll() { await api.put("/notifications/read-all"); },
};
