package com.apptitle.joinrequest.service;

import com.apptitle.common.exception.ApiException;
import com.apptitle.joinrequest.dto.CreateJoinRequestRequest;
import com.apptitle.joinrequest.dto.JoinRequestResponse;
import com.apptitle.joinrequest.dto.SectionMemberResponse;
import com.apptitle.joinrequest.dto.StudentSectionResponse;
import com.apptitle.joinrequest.entity.JoinRequest;
import com.apptitle.joinrequest.entity.JoinRequestStatus;
import com.apptitle.joinrequest.repository.JoinRequestRepository;
import com.apptitle.section.entity.Section;
import com.apptitle.section.repository.SectionRepository;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.entity.User;
import com.apptitle.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class JoinRequestService {

    private final JoinRequestRepository joinRequestRepository;
    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    public JoinRequestService(
            JoinRequestRepository joinRequestRepository,
            SectionRepository sectionRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            UserRepository userRepository
    ) {
        this.joinRequestRepository = joinRequestRepository;
        this.sectionRepository = sectionRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
    }

    // ---------- Teacher side ----------

    @Transactional(readOnly = true)
    public List<JoinRequestResponse> listPendingForSection(String teacherEmail, UUID sectionId) {
        Section section = resolveOwnedSection(teacherEmail, sectionId);
        return joinRequestRepository.findBySectionIdAndStatus(section.getId(), JoinRequestStatus.PENDING).stream()
                .map(this::toJoinRequestResponse)
                .toList();
    }

    @Transactional
    public JoinRequestResponse approve(String teacherEmail, UUID joinRequestId) {
        JoinRequest request = resolveOwnedJoinRequest(teacherEmail, joinRequestId);
        request.setStatus(JoinRequestStatus.APPROVED);
        request = joinRequestRepository.save(request);
        return toJoinRequestResponse(request);
    }

    @Transactional
    public JoinRequestResponse decline(String teacherEmail, UUID joinRequestId) {
        JoinRequest request = resolveOwnedJoinRequest(teacherEmail, joinRequestId);
        request.setStatus(JoinRequestStatus.DECLINED);
        request = joinRequestRepository.save(request);
        return toJoinRequestResponse(request);
    }

    @Transactional(readOnly = true)
    public List<SectionMemberResponse> listMembers(String teacherEmail, UUID sectionId) {
        Section section = resolveOwnedSection(teacherEmail, sectionId);
        return joinRequestRepository.findBySectionIdAndStatus(section.getId(), JoinRequestStatus.APPROVED).stream()
                .map(jr -> new SectionMemberResponse(
                        jr.getStudent().getId(),
                        jr.getStudent().getName(),
                        jr.getStudent().getLrn(),
                        jr.getUpdatedAt()))
                .toList();
    }

    /**
     * Removing a member deletes the JoinRequest row entirely (rather than
     * setting it to DECLINED) so the student is free to send a fresh
     * request later if re-added — DECLINED is reserved for "never was
     * admitted," which is a different situation semantically.
     */
    @Transactional
    public void removeMember(String teacherEmail, UUID sectionId, UUID studentId) {
        Section section = resolveOwnedSection(teacherEmail, sectionId);
        JoinRequest request = joinRequestRepository.findByStudentIdAndSectionId(studentId, section.getId())
                .orElseThrow(() -> ApiException.notFound("This student is not a member of this section."));

        if (request.getStatus() != JoinRequestStatus.APPROVED) {
            throw ApiException.badRequest("This student is not currently an approved member of this section.");
        }

        joinRequestRepository.delete(request);
    }

    // ---------- Student side ----------

    @Transactional
    public JoinRequestResponse createJoinRequest(String studentEmail, CreateJoinRequestRequest request) {
        Student student = resolveStudent(studentEmail);
        String code = request.classroomCode().trim().toUpperCase();

        Section section = sectionRepository.findByClassCode(code)
                .orElseThrow(() -> ApiException.badRequest(
                        "Invalid classroom code. Please check the code with your teacher."));

        if (joinRequestRepository.existsByStudentIdAndSectionId(student.getId(), section.getId())) {
            throw ApiException.conflict(
                    "You've already requested to join this classroom (or are already a member).");
        }

        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setStudent(student);
        joinRequest.setSection(section);
        joinRequest.setStatus(JoinRequestStatus.PENDING);
        joinRequest = joinRequestRepository.save(joinRequest);

        return toJoinRequestResponse(joinRequest);
    }

    @Transactional(readOnly = true)
    public List<JoinRequestResponse> listOwnJoinRequests(String studentEmail) {
        Student student = resolveStudent(studentEmail);
        return joinRequestRepository.findByStudentId(student.getId()).stream()
                .map(this::toJoinRequestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentSectionResponse> listOwnApprovedSections(String studentEmail) {
        Student student = resolveStudent(studentEmail);
        return joinRequestRepository.findByStudentId(student.getId()).stream()
                .filter(jr -> jr.getStatus() == JoinRequestStatus.APPROVED)
                .map(jr -> new StudentSectionResponse(
                        jr.getSection().getId(),
                        jr.getSection().getName(),
                        jr.getSection().getSubjectName(),
                        jr.getSection().getTeacher().getName()))
                .toList();
    }

    // ---------- Shared helpers ----------

    private Section resolveOwnedSection(String teacherEmail, UUID sectionId) {
        Teacher teacher = resolveTeacher(teacherEmail);
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> ApiException.notFound("Section not found."));

        if (!section.getTeacher().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not manage this section.");
        }
        return section;
    }

    private JoinRequest resolveOwnedJoinRequest(String teacherEmail, UUID joinRequestId) {
        Teacher teacher = resolveTeacher(teacherEmail);
        JoinRequest request = joinRequestRepository.findById(joinRequestId)
                .orElseThrow(() -> ApiException.notFound("Join request not found."));

        if (!request.getSection().getTeacher().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not manage this section.");
        }
        return request;
    }

    private Teacher resolveTeacher(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only teachers can perform this action."));
    }

    private Student resolveStudent(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only students can perform this action."));
    }

    private JoinRequestResponse toJoinRequestResponse(JoinRequest jr) {
        return new JoinRequestResponse(
                jr.getId(),
                jr.getStudent().getId(),
                jr.getStudent().getName(),
                jr.getStudent().getLrn(),
                jr.getSection().getId(),
                jr.getSection().getName(),
                jr.getSection().getSubjectName(),
                jr.getStatus(),
                jr.getUpdatedAt());
    }
}
