import { useState } from "react";
import { EmptyState } from "@/components/shared/EmptyState";
import type { ClassSubjectLinkResponse } from "@/types";

export function SubjectLinkRequestList({
  requests,
  onApprove,
  onDecline,
}: {
  requests: ClassSubjectLinkResponse[];
  onApprove: (id: string) => Promise<void>;
  onDecline: (id: string) => Promise<void>;
}) {
  const [activeId, setActiveId] = useState("");

  const act = async (id: string, action: (id: string) => Promise<void>) => {
    setActiveId(id);
    try {
      await action(id);
    } finally {
      setActiveId("");
    }
  };

  if (!requests.length) {
    return <EmptyState title="No pending link requests" detail="Requests from class advisers will appear here." />;
  }

  return (
    <div className="divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white">
      {requests.map((request) => (
        <div key={request.id} className="flex flex-wrap items-center justify-between gap-3 p-4">
          <div>
            <p className="font-medium">{request.classSectionName}</p>
            <p className="text-sm text-slate-500">{request.schoolYear} · requested by {request.requestingAdviserName}</p>
          </div>
          <div className="flex gap-2">
            <button disabled={Boolean(activeId)} onClick={() => act(request.id, onApprove)} className="rounded-lg bg-emerald-600 px-3 py-2 text-sm font-medium text-white disabled:opacity-60">Approve</button>
            <button disabled={Boolean(activeId)} onClick={() => act(request.id, onDecline)} className="rounded-lg border border-rose-300 px-3 py-2 text-sm font-medium text-rose-700 disabled:opacity-60">Decline</button>
          </div>
        </div>
      ))}
    </div>
  );
}
