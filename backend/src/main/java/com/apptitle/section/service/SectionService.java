package com.apptitle.section.service;

import com.apptitle.common.exception.ApiException;
import com.apptitle.section.dto.CreateSectionRequest;
import com.apptitle.section.dto.SectionResponse;
import com.apptitle.section.entity.Section;
import com.apptitle.section.repository.SectionRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Deliberately minimal — create + list only. Full Phase 3 scope (edit,
 * delete, pending-join-request review, member management) is not here yet;
 * this exists so a teacher can create a classroom and get a real code to
 * test student registration against.
 */
@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final ClassCodeGenerator classCodeGenerator;

    public SectionService(
            SectionRepository sectionRepository,
            TeacherRepository teacherRepository,
            UserRepository userRepository,
            ClassCodeGenerator classCodeGenerator
    ) {
        this.sectionRepository = sectionRepository;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
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

    public List<SectionResponse> listOwnSections(String teacherEmail) {
        Teacher teacher = resolveTeacher(teacherEmail);
        return sectionRepository.findByTeacherId(teacher.getId()).stream()
                .map(this::toResponse)
                .toList();
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
