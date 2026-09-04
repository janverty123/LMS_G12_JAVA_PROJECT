export function ApiAlert({
  message,
  tone = "error",
}: {
  message: string;
  tone?: "error" | "success";
}) {
  const colors = tone === "success" ? "api-alert--success" : "api-alert--error";

  return (
    <div
      className={`rounded-lg border px-4 py-3 text-sm ${colors}`}
      role="alert"
    >
      {message}
    </div>
  );
}
