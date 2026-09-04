import { useEffect, useState } from "react";
import { EmptyState } from "@/components/shared/EmptyState";
import { notificationService } from "@/services/notification.service";
import type { Announcement } from "@/types";

export function StudentAnnouncementsPage() {
  const [items, setItems] = useState<Announcement[]>([]);
  useEffect(() => {
    notificationService.studentAnnouncements().then(setItems);
  }, []);
  return (
    <div>
      <div className="page-heading">
        <h1>Announcements</h1>
        <p>Updates from your class adviser.</p>
      </div>
      <div className="mt-6 space-y-3">
        {items.length ? (
          items.map((item) => (
            <article key={item.id} className="panel">
              <h2 className="font-semibold">{item.title}</h2>
              <p className="mt-2 whitespace-pre-wrap text-sm">{item.content}</p>
              <p className="mt-2 text-xs text-[var(--text-muted)]">
                {item.authorName} · {new Date(item.createdAt).toLocaleString()}
              </p>
            </article>
          ))
        ) : (
          <EmptyState
            title="No announcements"
            detail="Your adviser's updates will appear here."
          />
        )}
      </div>
    </div>
  );
}
