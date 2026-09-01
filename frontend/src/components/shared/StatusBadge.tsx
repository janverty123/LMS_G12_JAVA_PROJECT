export function StatusBadge({ status }: { status: string }) {
  const colors =
    status === "APPROVED"
      ? "bg-emerald-100 text-emerald-800"
      : status === "DECLINED"
        ? "bg-rose-100 text-rose-800"
        : "bg-[var(--accent-soft)] text-[var(--accent-strong)]";

  return (
    <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${colors}`}>
      {status.toLowerCase()}
    </span>
  );
}
