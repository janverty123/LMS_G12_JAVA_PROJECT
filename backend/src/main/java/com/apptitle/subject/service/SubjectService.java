package com.apptitle.subject.service;

import com.apptitle.common.exception.ApiException;
import com.apptitle.subject.dto.CreateSubjectRequest;
import com.apptitle.subject.dto.SubjectResponse;
import com.apptitle.subject.dto.UpdateSubjectRequest;
import com.apptitle.subject.entity.Subject;
import com.apptitle.subject.repository.SubjectRepository;
import com.apptitle.subject.repository.ClassSubjectLinkRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing Subject entities.
 */
@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final ClassSubjectLinkRepository classSubjectLinkRepository;
    private final SubjectCodeGenerator subjectCodeGenerator;

    public SubjectService(
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository,
            UserRepository userRepository,
            ClassSubjectLinkRepository classSubjectLinkRepository,
            SubjectCodeGenerator subjectCodeGenerator
    ) {
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.classSubjectLinkRepository = classSubjectLinkRepository;
        this.subjectCodeGenerator = subjectCodeGenerator;
    }

    @Transactional
    public SubjectResponse createSubject(String teacherEmail, CreateSubjectRequest request) {
        Teacher teacher = resolveTeacher(teacherEmail);
        String name = request.name().trim();

        if (subjectRepository.existsBySubjectTeacherIdAndNameIgnoreCase(teacher.getId(), name)) {
            throw ApiException.conflict("You already have a subject with this name.");
        }

        Subject subject = new Subject();
        subject.setSubjectTeacher(teacher);
        subject.setName(name);
        subject.setSubjectCode(subjectCodeGenerator.generateUnique());

        subject = subjectRepository.save(subject);
        return toResponse(subject);
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> listOwnSubjects(String teacherEmail) {
        Teacher teacher = resolveTeacher(teacherEmail);
        return subjectRepository.findBySubjectTeacherId(teacher.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SubjectResponse updateSubject(String teacherEmail, UUID subjectId, UpdateSubjectRequest request) {
        Subject subject = resolveOwnedSubject(teacherEmail, subjectId);
        String name = request.name().trim();
        if (!subject.getName().equalsIgnoreCase(name)
                && subjectRepository.existsBySubjectTeacherIdAndNameIgnoreCase(
                        subject.getSubjectTeacher().getId(), name)) {
            throw ApiException.conflict("You already have a subject with this name.");
        }
        subject.setName(name);
        subject = subjectRepository.save(subject);
        return toResponse(subject);
    }

    @Transactional
    public void deleteSubject(String teacherEmail, UUID subjectId) {
        Subject subject = resolveOwnedSubject(teacherEmail, subjectId);
        classSubjectLinkRepository.deleteBySubjectId(subject.getId());
        subjectRepository.delete(subject);
    }

    private Subject resolveOwnedSubject(String teacherEmail, UUID subjectId) {
        Teacher teacher = resolveTeacher(teacherEmail);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> ApiException.notFound("Subject not found."));

        if (!subject.getSubjectTeacher().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not own this subject.");
        }
        return subject;
    }

    private Teacher resolveTeacher(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only teachers can manage subjects."));
    }

    private SubjectResponse toResponse(Subject subject) {
        return new SubjectResponse(
                subject.getId(),
                subject.getName(),
                subject.getSubjectCode(),
                subject.getSubjectTeacher().getName()
        );
    }
}
