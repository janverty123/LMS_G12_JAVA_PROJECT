export function StatusBadge({ status }: { status: string }) {
  const colors =
    status === "APPROVED"
      ? "status-badge--approved"
      : status === "DECLINED"
        ? "status-badge--declined"
        : "bg-[var(--accent-soft)] text-[var(--accent-strong)]";

  return (
    <span
      className={`rounded-full px-2.5 py-1 text-xs font-semibold ${colors}`}
    >
      {status.toLowerCase()}
    </span>
  );
}
