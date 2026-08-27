package com.apptitle.classsection.service;

import com.apptitle.classsection.dto.ClassEnrollmentRequestResponse;
import com.apptitle.classsection.dto.SectionMemberResponse;
import com.apptitle.classsection.dto.StudentClassSectionResponse;
import com.apptitle.classsection.entity.ClassEnrollmentRequest;
import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.classsection.repository.ClassSectionRepository;
import com.apptitle.common.exception.ApiException;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.subject.dto.SubjectResponse;
import com.apptitle.subject.entity.ClassSubjectLinkStatus;
import com.apptitle.subject.repository.ClassSubjectLinkRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing ClassEnrollmentRequest entities.
 * Replaces the old JoinRequestService as part of the ClassSection/Subject migration.
 */
@Service
public class ClassEnrollmentRequestService {

    private final ClassEnrollmentRequestRepository classEnrollmentRequestRepository;
    private final ClassSectionRepository classSectionRepository;
    private final StudentRepository studentRepository;
    private final ClassSubjectLinkRepository classSubjectLinkRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    public ClassEnrollmentRequestService(
            ClassEnrollmentRequestRepository classEnrollmentRequestRepository,
            ClassSectionRepository classSectionRepository,
            StudentRepository studentRepository,
            ClassSubjectLinkRepository classSubjectLinkRepository,
            TeacherRepository teacherRepository,
            UserRepository userRepository
    ) {
        this.classEnrollmentRequestRepository = classEnrollmentRequestRepository;
        this.classSectionRepository = classSectionRepository;
        this.studentRepository = studentRepository;
        this.classSubjectLinkRepository = classSubjectLinkRepository;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
    }

    // ---------- Teacher side ----------

    @Transactional(readOnly = true)
    public List<ClassEnrollmentRequestResponse> listPendingForClassSection(String teacherEmail, UUID classSectionId) {
        ClassSection classSection = resolveOwnedClassSection(teacherEmail, classSectionId);
        return classEnrollmentRequestRepository.findByClassSectionIdAndStatus(
                        classSection.getId(), ClassEnrollmentRequestStatus.PENDING).stream()
                .map(this::toClassEnrollmentRequestResponse)
                .toList();
    }

    @Transactional
    public ClassEnrollmentRequestResponse approve(String teacherEmail, UUID requestId) {
        ClassEnrollmentRequest request = resolveOwnedRequest(teacherEmail, requestId);
        requirePending(request);
        if (classEnrollmentRequestRepository.existsByStudentIdAndStatus(
                request.getStudent().getId(), ClassEnrollmentRequestStatus.APPROVED)) {
            throw ApiException.conflict("This student already belongs to another class section.");
        }
        request.setStatus(ClassEnrollmentRequestStatus.APPROVED);
        request = classEnrollmentRequestRepository.save(request);
        return toClassEnrollmentRequestResponse(request);
    }

    @Transactional
    public ClassEnrollmentRequestResponse decline(String teacherEmail, UUID requestId) {
        ClassEnrollmentRequest request = resolveOwnedRequest(teacherEmail, requestId);
        requirePending(request);
        request.setStatus(ClassEnrollmentRequestStatus.DECLINED);
        request = classEnrollmentRequestRepository.save(request);
        return toClassEnrollmentRequestResponse(request);
    }

    @Transactional(readOnly = true)
    public List<SectionMemberResponse> listMembers(String teacherEmail, UUID classSectionId) {
        ClassSection classSection = resolveOwnedClassSection(teacherEmail, classSectionId);
        return classEnrollmentRequestRepository.findByClassSectionIdAndStatus(
                        classSection.getId(), ClassEnrollmentRequestStatus.APPROVED).stream()
                .map(jr -> new SectionMemberResponse(
                        jr.getStudent().getId(),
                        jr.getStudent().getName(),
                        jr.getStudent().getLrn(),
                        jr.getStudent().getUser().getEmail(),
                        jr.getStatus(),
                        jr.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public void removeMember(String teacherEmail, UUID classSectionId, UUID studentId) {
        ClassSection classSection = resolveOwnedClassSection(teacherEmail, classSectionId);
        ClassEnrollmentRequest request = classEnrollmentRequestRepository.findByStudentIdAndClassSectionId(studentId, classSection.getId())
                .orElseThrow(() -> ApiException.notFound("This student is not a member of this class section."));

        if (request.getStatus() != ClassEnrollmentRequestStatus.APPROVED) {
            throw ApiException.badRequest("This student is not currently an approved member of this class section.");
        }

        classEnrollmentRequestRepository.delete(request);
    }

    // ---------- Student side ----------

    @Transactional
    public ClassEnrollmentRequestResponse createJoinRequest(String studentEmail, String classCode) {
        Student student = resolveStudent(studentEmail);
        String code = classCode.trim().toUpperCase();

        ClassSection classSection = classSectionRepository.findByClassCode(code)
                .orElseThrow(() -> ApiException.badRequest(
                        "Invalid class code. Please check the code with your teacher."));

        if (classEnrollmentRequestRepository.existsByStudentIdAndStatus(
                student.getId(), ClassEnrollmentRequestStatus.APPROVED)) {
            throw ApiException.conflict("You already belong to a class section.");
        }

        if (classEnrollmentRequestRepository.existsByStudentIdAndClassSectionId(student.getId(), classSection.getId())) {
            throw ApiException.conflict(
                    "You've already requested to join this class (or are already a member).");
        }

        ClassEnrollmentRequest joinRequest = new ClassEnrollmentRequest();
        joinRequest.setStudent(student);
        joinRequest.setClassSection(classSection);
        joinRequest.setStatus(ClassEnrollmentRequestStatus.PENDING);
        joinRequest = classEnrollmentRequestRepository.save(joinRequest);

        return toClassEnrollmentRequestResponse(joinRequest);
    }

    @Transactional(readOnly = true)
    public List<ClassEnrollmentRequestResponse> listOwnJoinRequests(String studentEmail) {
        Student student = resolveStudent(studentEmail);
        return classEnrollmentRequestRepository.findByStudentId(student.getId()).stream()
                .map(this::toClassEnrollmentRequestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentClassSectionResponse getOwnApprovedClassSection(String studentEmail) {
        Student student = resolveStudent(studentEmail);
        return toStudentClassSectionResponse(resolveApprovedEnrollment(student));
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> listOwnApprovedSubjects(String studentEmail) {
        Student student = resolveStudent(studentEmail);
        ClassEnrollmentRequest enrollment = resolveApprovedEnrollment(student);
        return classSubjectLinkRepository.findByClassSectionIdAndStatus(
                        enrollment.getClassSection().getId(), ClassSubjectLinkStatus.APPROVED).stream()
                .map(link -> new SubjectResponse(
                        link.getSubject().getId(),
                        link.getSubject().getName(),
                        link.getSubject().getSubjectCode(),
                        link.getSubject().getSubjectTeacher().getName()))
                .toList();
    }

    // ---------- Shared helpers ----------

    private ClassSection resolveOwnedClassSection(String teacherEmail, UUID classSectionId) {
        Teacher teacher = resolveTeacher(teacherEmail);
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> ApiException.notFound("Class section not found."));

        if (!classSection.getAdviser().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not manage this class section.");
        }
        return classSection;
    }

    private ClassEnrollmentRequest resolveOwnedRequest(String teacherEmail, UUID requestId) {
        Teacher teacher = resolveTeacher(teacherEmail);
        ClassEnrollmentRequest request = classEnrollmentRequestRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Join request not found."));

        if (!request.getClassSection().getAdviser().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not manage this class section.");
        }
        return request;
    }

    private Teacher resolveTeacher(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only teachers can perform this action."));
    }

    private Student resolveStudent(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only students can perform this action."));
    }

    private ClassEnrollmentRequest resolveApprovedEnrollment(Student student) {
        return classEnrollmentRequestRepository.findByStudentIdAndStatus(
                        student.getId(), ClassEnrollmentRequestStatus.APPROVED).stream()
                .findFirst()
                .orElseThrow(() -> ApiException.notFound(
                        "You are not enrolled in any class section."));
    }

    private void requirePending(ClassEnrollmentRequest request) {
        if (request.getStatus() != ClassEnrollmentRequestStatus.PENDING) {
            throw ApiException.badRequest("Only pending enrollment requests can be reviewed.");
        }
    }

    private StudentClassSectionResponse toStudentClassSectionResponse(ClassEnrollmentRequest request) {
        ClassSection classSection = request.getClassSection();
        return new StudentClassSectionResponse(
                classSection.getId(),
                classSection.getGradeLevel() + " - " + classSection.getSection(),
                classSection.getSchoolYear(),
                classSection.getAdviser().getName());
    }

    private ClassEnrollmentRequestResponse toClassEnrollmentRequestResponse(ClassEnrollmentRequest request) {
        return new ClassEnrollmentRequestResponse(
                request.getId(),
                request.getStudent().getId(),
                request.getStudent().getName(),
                request.getStudent().getLrn(),
                request.getClassSection().getId(),
                request.getClassSection().getGradeLevel() + " - " + request.getClassSection().getSection(),
                request.getClassSection().getSchoolYear(),
                request.getStatus(),
                request.getUpdatedAt());
    }
}
