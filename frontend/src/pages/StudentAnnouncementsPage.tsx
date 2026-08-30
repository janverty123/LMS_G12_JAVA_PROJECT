import { useEffect, useState } from "react";
import { notificationService } from "@/services/notification.service";
import type { Announcement } from "@/types";

export function StudentAnnouncementsPage() {
  const [items, setItems] = useState<Announcement[]>([]); useEffect(() => { notificationService.studentAnnouncements().then(setItems); }, []);
  return <div><h1 className="text-3xl font-semibold">Announcements</h1><div className="mt-6 space-y-3">{items.map((item) => <article key={item.id} className="rounded-xl border bg-white p-5"><h2 className="font-semibold">{item.title}</h2><p className="mt-2 whitespace-pre-wrap text-sm">{item.content}</p><p className="mt-2 text-xs text-slate-500">{item.authorName} · {new Date(item.createdAt).toLocaleString()}</p></article>)}</div></div>;
}
