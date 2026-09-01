import { useEffect, useState } from "react";
import { NavLink, useParams } from "react-router-dom";
import { subjectService } from "@/services/subject.service";
import type { SubjectResponse } from "@/types";

export function StudentSubjectHeader() {
  const { subjectId = "" } = useParams();
  const [subject, setSubject] = useState<SubjectResponse>();

  useEffect(() => {
    subjectService.listStudentSubjects()
      .then((subjects) => setSubject(subjects.find((item) => item.id === subjectId)))
      .catch(() => setSubject(undefined));
  }, [subjectId]);

  const tabs = [
    ["materials", "Materials"],
    ["activities", "Activities"],
    ["grades", "Grades"],
    ["progress", "Progress"],
  ] as const;

  return (
    <header>
      <div className="page-heading"><h1>{subject?.name ?? "Subject"}</h1><p>{subject?.subjectTeacherName ?? "Subject Teacher"}</p></div>
      <div className="workspace-tabs" aria-label="Subject navigation">
        {tabs.map(([path, label]) => <NavLink key={path} className={({ isActive }) => `workspace-tab${isActive ? " workspace-tab--active" : ""}`} to={`/student/subjects/${subjectId}/${path}`}>{label}</NavLink>)}
      </div>
    </header>
  );
}
