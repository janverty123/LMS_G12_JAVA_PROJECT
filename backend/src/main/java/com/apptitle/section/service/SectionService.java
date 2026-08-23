package com.apptitle.section.service;

import com.apptitle.common.exception.ApiException;
import com.apptitle.joinrequest.repository.JoinRequestRepository;
import com.apptitle.section.dto.CreateSectionRequest;
import com.apptitle.section.dto.SectionResponse;
import com.apptitle.section.dto.UpdateSectionRequest;
import com.apptitle.section.entity.Section;
import com.apptitle.section.repository.SectionRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Full Phase 3 scope: create, list, edit, delete. Pending-join-request
 * review and member management live in JoinRequestService instead, since
 * they're about JoinRequest records, not Section fields.
 */
@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final ClassCodeGenerator classCodeGenerator;

    public SectionService(
            SectionRepository sectionRepository,
            TeacherRepository teacherRepository,
            UserRepository userRepository,
            JoinRequestRepository joinRequestRepository,
            ClassCodeGenerator classCodeGenerator
    ) {
        this.sectionRepository = sectionRepository;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.classCodeGenerator = classCodeGenerator;
    }

    @Transactional
    public SectionResponse createSection(String teacherEmail, CreateSectionRequest request) {
        Teacher teacher = resolveTeacher(teacherEmail);

        Section section = new Section();
        section.setTeacher(teacher);
        section.setName(request.name().trim());
        section.setSubjectName(request.subjectName().trim());
        section.setClassCode(classCodeGenerator.generateUnique());

        section = sectionRepository.save(section);
        return toResponse(section);
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> listOwnSections(String teacherEmail) {
        Teacher teacher = resolveTeacher(teacherEmail);
        return sectionRepository.findByTeacherId(teacher.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Name/subject only — classCode is intentionally NOT editable. Changing
     * it would silently invalidate the code students already have, with no
     * way for them to know it changed.
     */
    @Transactional
    public SectionResponse updateSection(String teacherEmail, UUID sectionId, UpdateSectionRequest request) {
        Section section = resolveOwnedSection(teacherEmail, sectionId);
        section.setName(request.name().trim());
        section.setSubjectName(request.subjectName().trim());
        section = sectionRepository.save(section);
        return toResponse(section);
    }

    /**
     * Cascades: deletes every JoinRequest tied to this section first (both
     * pending and approved), then the section itself, to avoid a foreign-key
     * violation. No confirmation/undo at this layer — the controller/
     * frontend is responsible for warning the teacher this is irreversible.
     */
    @Transactional
    public void deleteSection(String teacherEmail, UUID sectionId) {
        Section section = resolveOwnedSection(teacherEmail, sectionId);
        joinRequestRepository.deleteBySectionId(section.getId());
        sectionRepository.delete(section);
    }

    private Section resolveOwnedSection(String teacherEmail, UUID sectionId) {
        Teacher teacher = resolveTeacher(teacherEmail);
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> ApiException.notFound("Section not found."));

        if (!section.getTeacher().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not manage this section.");
        }
        return section;
    }

    private Teacher resolveTeacher(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only teachers can manage sections."));
    }

    private SectionResponse toResponse(Section section) {
        return new SectionResponse(
                section.getId(), section.getName(), section.getSubjectName(), section.getClassCode());
    }
}
