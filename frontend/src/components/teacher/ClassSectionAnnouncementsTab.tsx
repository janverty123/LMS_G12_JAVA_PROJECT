import { useCallback, useEffect, useState, type FormEvent } from "react";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { notificationService } from "@/services/notification.service";
import { getErrorMessage } from "@/services/errors";
import type { Announcement } from "@/types";

export function ClassSectionAnnouncementsTab({ classSectionId }: { classSectionId: string }) {
  const [items, setItems] = useState<Announcement[]>([]); const [editing, setEditing] = useState<Announcement>(); const [title, setTitle] = useState(""); const [content, setContent] = useState(""); const [error, setError] = useState("");
  const load = useCallback(async () => { try { setItems(await notificationService.teacherAnnouncements(classSectionId)); } catch (value) { setError(getErrorMessage(value)); } }, [classSectionId]);
  useEffect(() => { void load(); }, [load]);
  const submit = async (event: FormEvent) => { event.preventDefault(); try { if (editing) await notificationService.updateAnnouncement(editing.id, title, content); else await notificationService.createAnnouncement(classSectionId, title, content); setEditing(undefined); setTitle(""); setContent(""); await load(); } catch (value) { setError(getErrorMessage(value)); } };
  return <div className="space-y-5">{error && <ApiAlert message={error} />}<form className="space-y-3 rounded-xl border bg-white p-5" onSubmit={submit}><input required maxLength={150} placeholder="Announcement title" className="w-full rounded border px-3 py-2" value={title} onChange={(event) => setTitle(event.target.value)} /><textarea required maxLength={5000} placeholder="Message" className="w-full rounded border px-3 py-2" rows={4} value={content} onChange={(event) => setContent(event.target.value)} /><button className="rounded bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white">{editing ? "Save" : "Post announcement"}</button></form>{items.map((item) => <article key={item.id} className="rounded-xl border bg-white p-5"><h3 className="font-semibold">{item.title}</h3><p className="mt-2 whitespace-pre-wrap text-sm">{item.content}</p><p className="mt-2 text-xs text-slate-500">{new Date(item.createdAt).toLocaleString()}</p><div className="responsive-actions mt-3"><button className="text-sm" onClick={() => { setEditing(item); setTitle(item.title); setContent(item.content); }}>Edit</button><button className="text-sm text-rose-600" onClick={() => void notificationService.deleteAnnouncement(item.id).then(load)}>Delete</button></div></article>)}</div>;
}
