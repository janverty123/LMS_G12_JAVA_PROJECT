export function ApiAlert({
  message,
  tone = "error",
}: {
  message: string;
  tone?: "error" | "success";
}) {
  const colors =
    tone === "success"
      ? "border-emerald-200 bg-emerald-50 text-emerald-800"
      : "border-rose-200 bg-rose-50 text-rose-800";

  return (
    <div className={`rounded-lg border px-4 py-3 text-sm ${colors}`} role="alert">
      {message}
    </div>
  );
}
