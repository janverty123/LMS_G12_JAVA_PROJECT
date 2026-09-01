import { useEffect, useState } from "react";
import { ClassSectionAnnouncementsTab } from "@/components/teacher/ClassSectionAnnouncementsTab";
import { EmptyState } from "@/components/shared/EmptyState";
import { classSectionService } from "@/services/classSection.service";
import type { ClassSectionResponse } from "@/types";

export function TeacherAnnouncementsPage() {
  const [classes, setClasses] = useState<ClassSectionResponse[]>([]);
  const [classId, setClassId] = useState("");
  useEffect(() => { void classSectionService.listTeacherClassSections().then((items) => { setClasses(items); setClassId(items[0]?.id ?? ""); }); }, []);
  return <div><div className="page-heading"><h1>Announcements</h1><p>Post updates to students in one of your advised classes.</p></div>{classes.length ? <><label className="mt-7 block max-w-md text-sm font-medium">Class Section<select className="mt-1 w-full rounded-lg border px-3 py-2" value={classId} onChange={(event) => setClassId(event.target.value)}>{classes.map((item) => <option key={item.id} value={item.id}>{item.gradeLevel} - {item.section}</option>)}</select></label><div className="mt-5"><ClassSectionAnnouncementsTab key={classId} classSectionId={classId} /></div></> : <div className="mt-7"><EmptyState title="No class sections" detail="Create a class before posting announcements." /></div>}</div>;
}
