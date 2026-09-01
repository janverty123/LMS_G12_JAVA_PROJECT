import { useEffect, useState } from "react";
import { notificationService } from "@/services/notification.service";
import type { AppNotification } from "@/types";

export function NotificationsPage() {
  const [items, setItems] = useState<AppNotification[]>([]);
  const load = async () => setItems(await notificationService.notifications());

  useEffect(() => {
    void load();
  }, []);

  return (
    <div>
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-3xl font-semibold">Notifications</h1>
        <button
          className="notification-action text-sm font-semibold"
          onClick={() => void notificationService.readAll().then(load)}
        >
          Mark all read
        </button>
      </div>
      <div className="mt-6 space-y-3">
        {items.map((item) => (
          <button
            key={item.id}
            className={`notification-card block w-full rounded-xl border p-4 text-left ${item.read ? "" : "notification-card--unread"}`}
            onClick={() => void notificationService.read(item.id).then(load)}
          >
            <p className="font-medium">{item.message}</p>
            <p className="notification-card__time mt-1 text-xs">
              {new Date(item.createdAt).toLocaleString()}
            </p>
          </button>
        ))}
      </div>
    </div>
  );
}
