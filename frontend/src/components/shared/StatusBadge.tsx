export function StatusBadge({ status }: { status: string }) {
  const colors =
    status === "APPROVED"
      ? "bg-emerald-100 text-emerald-800"
      : status === "DECLINED"
        ? "bg-rose-100 text-rose-800"
        : "bg-amber-100 text-amber-800";

  return (
    <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${colors}`}>
      {status.toLowerCase()}
    </span>
  );
}
