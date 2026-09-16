import { useEffect, useRef, useState, type FormEvent } from "react";
import { useAuth } from "@/hooks/useAuth";
import { api } from "@/services/api";
import type { AuthResponse } from "@/types";

async function preparePicture(file: File): Promise<string> {
  if (!["image/jpeg", "image/png", "image/webp"].includes(file.type) || file.size > 10 * 1024 * 1024) {
    throw new Error("Choose a JPG, PNG, or WebP image smaller than 10 MB.");
  }
  const bitmap = await createImageBitmap(file);
  try {
    const canvas = document.createElement("canvas");
    canvas.width = 192;
    canvas.height = 192;
    const context = canvas.getContext("2d");
    if (!context) throw new Error("Unable to process this picture.");
    const side = Math.min(bitmap.width, bitmap.height);
    context.drawImage(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side, 0, 0, 192, 192);
    return canvas.toDataURL("image/png");
  } finally {
    bitmap.close();
  }
}

export function EditProfileDialog({ onClose }: { onClose: () => void }) {
  const { user, saveSession } = useAuth();
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [name, setName] = useState(user?.name ?? "");
  const [email, setEmail] = useState(user?.email ?? "");
  const [lrn, setLrn] = useState(user?.lrn ?? "");
  const [picture, setPicture] = useState<string | null>(user?.profilePicture ?? null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    dialogRef.current?.showModal();
    let active = true;
    api.get<AuthResponse>("/profile").then(({ data }) => {
      if (!active) return;
      setName(data.name);
      setEmail(data.email);
      setLrn(data.lrn ?? "");
      setPicture(data.profilePicture ?? null);
      setLoading(false);
    }).catch((err: { message?: string }) => {
      if (active) setError(err.message ?? "Unable to load your profile. Close this window and try again.");
    });
    return () => { active = false; };
  }, []);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const { data } = await api.put<AuthResponse>("/profile", { name: name.trim(), email: email.trim(), lrn: user?.role === "STUDENT" ? lrn.trim() : null, profilePicture: picture });
      saveSession(data);
      onClose();
    } catch (err) {
      setError((err as { message?: string }).message ?? "Unable to save your profile.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <dialog ref={dialogRef} className="edit-profile-dialog" aria-labelledby="edit-profile-title" onCancel={(event) => { event.preventDefault(); if (!busy && !processing) onClose(); }}>
      <form onSubmit={submit}>
        <h2 id="edit-profile-title" className="text-xl font-semibold">Edit profile</h2>
        <p className="mt-1 text-sm text-[var(--text-muted)]">Update your picture and account details.</p>
        {error && <p role="alert" className="mt-3 text-sm text-red-600">{error}</p>}
        {loading && !error && <p role="status">Loading profile…</p>}
        <fieldset disabled={loading || busy || processing} className="mt-4 grid gap-4">
          <div className="flex items-center gap-4">
            {picture ? <img src={picture} alt="Profile preview" className="h-16 w-16 rounded-full object-cover" /> : <span className="profile-avatar">{name.charAt(0).toUpperCase() || "U"}</span>}
            <label className="grid gap-1 text-sm">Profile picture
              <input type="file" accept="image/png,image/jpeg,image/webp" onChange={async (event) => {
                const file = event.target.files?.[0];
                event.target.value = "";
                if (!file) return;
                setProcessing(true);
                setError("");
                try { setPicture(await preparePicture(file)); }
                catch (err) { setError((err as Error).message || "Unable to read this image."); }
                finally { setProcessing(false); }
              }} />
            </label>
          </div>
          {picture && <button type="button" className="text-left text-sm underline" onClick={() => setPicture(null)}>Remove picture</button>}
          <label className="grid gap-1 text-sm">Full name<input required maxLength={255} autoComplete="name" value={name} onChange={(event) => setName(event.target.value)} /></label>
          <label className="grid gap-1 text-sm">Email<input required type="email" maxLength={255} autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} /></label>
          {user?.role === "STUDENT" && <label className="grid gap-1 text-sm">LRN number<input required inputMode="numeric" pattern="[0-9]{12}" maxLength={12} title="Enter your 12-digit LRN" value={lrn} onChange={(event) => setLrn(event.target.value)} /></label>}
        </fieldset>
        <div className="mt-6 flex justify-end gap-3">
          <button type="button" className="rounded-lg border px-4 py-2" disabled={busy || processing} onClick={onClose}>Cancel</button>
          <button type="submit" className="rounded-lg bg-indigo-600 px-4 py-2 text-white disabled:opacity-50" disabled={loading || busy || processing}>{busy ? "Saving…" : processing ? "Processing picture…" : "Save changes"}</button>
        </div>
      </form>
    </dialog>
  );
}
