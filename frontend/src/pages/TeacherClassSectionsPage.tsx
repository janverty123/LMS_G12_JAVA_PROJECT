import { useCallback, useEffect, useState } from "react";
import { ApiAlert } from "@/components/shared/ApiAlert";
import { ClassSectionDetails } from "@/components/teacher/ClassSectionDetails";
import { ClassSectionForm } from "@/components/teacher/ClassSectionForm";
import { ClassSectionList } from "@/components/teacher/ClassSectionList";
import { classSectionService } from "@/services/classSection.service";
import { getErrorMessage } from "@/services/errors";
import type { ClassSectionResponse } from "@/types";

export function TeacherClassSectionsPage() {
  const [sections, setSections] = useState<ClassSectionResponse[]>([]);
  const [selected, setSelected] = useState<ClassSectionResponse>();
  const [editing, setEditing] = useState<ClassSectionResponse>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const nextSections = await classSectionService.listTeacherClassSections();
      setSections(nextSections);
      setSelected((current) => current ? nextSections.find((item) => item.id === current.id) : undefined);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const saved = (nextSection: ClassSectionResponse) => {
    setSections((current) => {
      const exists = current.some((item) => item.id === nextSection.id);
      return exists
        ? current.map((item) => item.id === nextSection.id ? nextSection : item)
        : [nextSection, ...current];
    });
    setSelected(nextSection);
    setEditing(undefined);
  };

  const remove = async (section: ClassSectionResponse) => {
    setError("");
    try {
      await classSectionService.deleteClassSection(section.id);
      setSections((current) => current.filter((item) => item.id !== section.id));
      if (selected?.id === section.id) setSelected(undefined);
      if (editing?.id === section.id) setEditing(undefined);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
      throw requestError;
    }
  };

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-3xl font-semibold tracking-tight">Class sections</h1>
        <p className="mt-1 text-slate-500">Manage class codes, students, and subject links.</p>
      </div>
      {error && <div className="mb-4"><ApiAlert message={error} /></div>}
      <ClassSectionForm section={editing} onSaved={saved} onCancel={() => setEditing(undefined)} />
      <div className="mt-7">
        <ClassSectionList
          sections={sections}
          selectedId={selected?.id}
          loading={loading}
          onSelect={setSelected}
          onEdit={setEditing}
          onDelete={remove}
        />
      </div>
      {selected && <ClassSectionDetails key={selected.id} section={selected} />}
    </div>
  );
}
