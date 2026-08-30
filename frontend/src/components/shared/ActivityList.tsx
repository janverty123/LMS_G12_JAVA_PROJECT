import { EmptyState } from "./EmptyState";
import type { ActivityResponse } from "@/types";

interface Props {
  activities: ActivityResponse[];
  onEdit?: (activity: ActivityResponse) => void;
  onDelete?: (activity: ActivityResponse) => void;
  onOpen?: (activity: ActivityResponse) => void;
}

export function ActivityList({ activities, onEdit, onDelete, onOpen }: Props) {
  if (!activities.length) {
    return <EmptyState title="No activities yet" detail="New activities will appear here." />;
  }

  return (
    <div className="grid gap-4 md:grid-cols-2">
      {activities.map((activity) => (
        <article key={activity.id} className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-amber-700">{activity.typeLabel}</p>
              <h3 className="mt-1 text-lg font-semibold">{activity.title}</h3>
            </div>
            <span className="rounded-full bg-slate-100 px-3 py-1 text-sm font-semibold">{activity.perfectScore} pts</span>
          </div>
          <p className="mt-3 whitespace-pre-wrap text-sm text-slate-600">{activity.instructions}</p>
          <p className="mt-4 text-sm font-medium text-slate-700">Due {new Date(activity.deadline).toLocaleString()}</p>
          {activity.allowStudentSelfSubmissionScore && (
            <p className="mt-1 text-xs text-emerald-700">Student score reporting enabled</p>
          )}
          {(onEdit || onDelete) && (
            <div className="mt-4 flex gap-3 border-t border-slate-100 pt-3">
              {onEdit && <button type="button" className="text-sm font-medium text-slate-600" onClick={() => onEdit(activity)}>Edit</button>}
              {onDelete && <button type="button" className="text-sm font-medium text-rose-600" onClick={() => onDelete(activity)}>Delete</button>}
            </div>
          )}
          {onOpen && <button type="button" className="mt-4 text-sm font-semibold text-amber-700" onClick={() => onOpen(activity)}>Open submissions and files →</button>}
        </article>
      ))}
    </div>
  );
}
