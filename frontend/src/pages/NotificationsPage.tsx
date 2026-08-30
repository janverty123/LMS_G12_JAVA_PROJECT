import { useEffect, useState } from "react";
import { notificationService } from "@/services/notification.service";
import type { AppNotification } from "@/types";

export function NotificationsPage() {
  const [items, setItems] = useState<AppNotification[]>([]); const load = async () => setItems(await notificationService.notifications()); useEffect(() => { void load(); }, []);
  return <div><div className="flex items-center justify-between"><h1 className="text-3xl font-semibold">Notifications</h1><button className="text-sm font-semibold text-amber-700" onClick={() => void notificationService.readAll().then(load)}>Mark all read</button></div><div className="mt-6 space-y-3">{items.map((item) => <button key={item.id} className={`block w-full rounded-xl border p-4 text-left ${item.read ? "bg-white" : "border-amber-300 bg-amber-50"}`} onClick={() => void notificationService.read(item.id).then(load)}><p className="font-medium">{item.message}</p><p className="mt-1 text-xs text-slate-500">{new Date(item.createdAt).toLocaleString()}</p></button>)}</div></div>;
}
