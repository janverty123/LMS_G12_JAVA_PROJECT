package com.apptitle.subject.service;

import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.classsection.repository.ClassSectionRepository;
import com.apptitle.common.exception.ApiException;
import com.apptitle.subject.dto.ClassSubjectLinkResponse;
import com.apptitle.subject.dto.CreateClassSubjectLinkRequest;
import com.apptitle.subject.entity.ClassSubjectLink;
import com.apptitle.subject.entity.ClassSubjectLinkStatus;
import com.apptitle.subject.entity.Subject;
import com.apptitle.subject.repository.ClassSubjectLinkRepository;
import com.apptitle.subject.repository.SubjectRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing ClassSection-Subject link requests.
 */
@Service
public class ClassSubjectLinkService {

    private final ClassSubjectLinkRepository classSubjectLinkRepository;
    private final ClassSectionRepository classSectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    public ClassSubjectLinkService(
            ClassSubjectLinkRepository classSubjectLinkRepository,
            ClassSectionRepository classSectionRepository,
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository,
            UserRepository userRepository
    ) {
        this.classSubjectLinkRepository = classSubjectLinkRepository;
        this.classSectionRepository = classSectionRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ClassSubjectLinkResponse createLinkRequest(String teacherEmail, UUID classSectionId, CreateClassSubjectLinkRequest request) {
        Teacher teacher = resolveTeacher(teacherEmail);
        ClassSection classSection = resolveOwnedClassSection(teacherEmail, classSectionId);
        String subjectCode = request.subjectCode().trim().toUpperCase();
        Subject subject = subjectRepository.findBySubjectCode(subjectCode)
                .orElseThrow(() -> ApiException.notFound("Subject not found."));

        // Check for existing link
        if (classSubjectLinkRepository.existsByClassSectionIdAndSubjectId(classSection.getId(), subject.getId())) {
            throw ApiException.conflict("This subject is already linked to this class section.");
        }

        ClassSubjectLink link = new ClassSubjectLink();
        link.setClassSection(classSection);
        link.setSubject(subject);
        link.setStatus(ClassSubjectLinkStatus.PENDING);
        link.setRequestedBy(teacher);

        link = classSubjectLinkRepository.save(link);
        return toResponse(link);
    }

    @Transactional
    public ClassSubjectLinkResponse approveLink(String teacherEmail, UUID linkId) {
        ClassSubjectLink link = resolveOwnedLinkForSubjectTeacher(teacherEmail, linkId);
        requirePending(link);
        link.setStatus(ClassSubjectLinkStatus.APPROVED);
        link = classSubjectLinkRepository.save(link);
        return toResponse(link);
    }

    @Transactional
    public ClassSubjectLinkResponse declineLink(String teacherEmail, UUID linkId) {
        ClassSubjectLink link = resolveOwnedLinkForSubjectTeacher(teacherEmail, linkId);
        requirePending(link);
        link.setStatus(ClassSubjectLinkStatus.DECLINED);
        link = classSubjectLinkRepository.save(link);
        return toResponse(link);
    }

    @Transactional(readOnly = true)
    public List<ClassSubjectLinkResponse> listPendingLinksForSubject(String teacherEmail, UUID subjectId) {
        Subject subject = resolveOwnedSubject(teacherEmail, subjectId);
        return classSubjectLinkRepository.findBySubjectIdAndStatus(subject.getId(), ClassSubjectLinkStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClassSubjectLinkResponse> listLinksForClassSection(String teacherEmail, UUID classSectionId) {
        ClassSection classSection = resolveOwnedClassSection(teacherEmail, classSectionId);
        return classSubjectLinkRepository.findByClassSectionId(classSection.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ClassSubjectLink resolveOwnedLinkForSubjectTeacher(String teacherEmail, UUID linkId) {
        Teacher teacher = resolveTeacher(teacherEmail);
        ClassSubjectLink link = classSubjectLinkRepository.findById(linkId)
                .orElseThrow(() -> ApiException.notFound("Link request not found."));

        if (!link.getSubject().getSubjectTeacher().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not own this subject.");
        }
        return link;
    }

    private ClassSection resolveOwnedClassSection(String teacherEmail, UUID classSectionId) {
        Teacher teacher = resolveTeacher(teacherEmail);
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> ApiException.notFound("Class section not found."));

        if (!classSection.getAdviser().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not manage this class section.");
        }
        return classSection;
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
                .orElseThrow(() -> ApiException.forbidden("Only teachers can perform this action."));
    }

    private void requirePending(ClassSubjectLink link) {
        if (link.getStatus() != ClassSubjectLinkStatus.PENDING) {
            throw ApiException.badRequest("Only pending subject-link requests can be reviewed.");
        }
    }

    private ClassSubjectLinkResponse toResponse(ClassSubjectLink link) {
        return new ClassSubjectLinkResponse(
                link.getId(),
                link.getClassSection().getId(),
                link.getClassSection().getGradeLevel() + " - " + link.getClassSection().getSection(),
                link.getClassSection().getSchoolYear(),
                link.getRequestedBy().getName(),
                link.getSubject().getId(),
                link.getSubject().getName(),
                link.getStatus(),
                link.getCreatedAt(),
                link.getUpdatedAt()
        );
    }
}
