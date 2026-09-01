package com.apptitle.subject.service;

import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.classsection.repository.ClassSectionRepository;
import com.apptitle.common.entity.BaseEntity;
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
import com.apptitle.user.entity.Role;
import com.apptitle.user.entity.User;
import com.apptitle.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ClassSubjectLinkServiceTest {

    @Mock private ClassSubjectLinkRepository linkRepository;
    @Mock private ClassSectionRepository classSectionRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private UserRepository userRepository;

    private ClassSubjectLinkService service;
    private Teacher adviser;
    private Teacher subjectTeacher;
    private Teacher otherTeacher;
    private ClassSection classSection;
    private Subject subject;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ClassSubjectLinkService(
                linkRepository,
                classSectionRepository,
                subjectRepository,
                teacherRepository,
                userRepository);

        adviser = teacher("adviser@example.com", "Ms. Adviser");
        subjectTeacher = teacher("subject@example.com", "Mr. Subject");
        otherTeacher = teacher("other@example.com", "Ms. Other");

        classSection = new ClassSection();
        setId(classSection);
        classSection.setAdviser(adviser);
        classSection.setGradeLevel("Grade 12");
        classSection.setSection("Rossum");
        classSection.setSchoolYear("2026-2027");
        classSection.setClassCode("ABC123");

        subject = new Subject();
        setId(subject);
        subject.setSubjectTeacher(subjectTeacher);
        subject.setName("Programming");
        subject.setSubjectCode("PROG123");

        when(linkRepository.save(any(ClassSubjectLink.class))).thenAnswer(invocation -> {
            ClassSubjectLink link = invocation.getArgument(0);
            if (link.getId() == null) setId(link);
            return link;
        });
    }

    @Test
    void createLinkRequest_succeedsForOwningAdviser() {
        when(classSectionRepository.findById(classSection.getId()))
                .thenReturn(Optional.of(classSection));
        when(subjectRepository.findBySubjectCode("PROG123")).thenReturn(Optional.of(subject));

        ClassSubjectLinkResponse response = service.createLinkRequest(
                "adviser@example.com",
                classSection.getId(),
                new CreateClassSubjectLinkRequest("prog123"));

        assertEquals(ClassSubjectLinkStatus.PENDING, response.status());
        assertEquals("Ms. Adviser", response.requestingAdviserName());
    }

    @Test
    void createLinkRequest_rejectsDuplicateLink() {
        when(classSectionRepository.findById(classSection.getId()))
                .thenReturn(Optional.of(classSection));
        when(subjectRepository.findBySubjectCode("PROG123")).thenReturn(Optional.of(subject));
        when(linkRepository.existsByClassSectionIdAndSubjectId(
                classSection.getId(), subject.getId())).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () ->
                service.createLinkRequest(
                        "adviser@example.com",
                        classSection.getId(),
                        new CreateClassSubjectLinkRequest("PROG123")));

        assertEquals(409, exception.getStatus().value());
    }

    @Test
    void approveLink_succeedsForSubjectOwner() {
        ClassSubjectLink link = pendingLink();
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));

        ClassSubjectLinkResponse response = service.approveLink(
                "subject@example.com", link.getId());

        assertEquals(ClassSubjectLinkStatus.APPROVED, response.status());
    }

    @Test
    void approveLink_rejectsNonOwningTeacher() {
        ClassSubjectLink link = pendingLink();
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.approveLink("other@example.com", link.getId()));

        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void declineLink_rejectsNonOwningTeacher() {
        ClassSubjectLink link = pendingLink();
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.declineLink("other@example.com", link.getId()));

        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void listLinksForSubject_returnsAllStatusesForSubjectOwner() {
        ClassSubjectLink pending = pendingLink();
        ClassSubjectLink approved = pendingLink();
        approved.setStatus(ClassSubjectLinkStatus.APPROVED);
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(linkRepository.findBySubjectId(subject.getId()))
                .thenReturn(List.of(pending, approved));

        var responses = service.listLinksForSubject("subject@example.com", subject.getId());

        assertEquals(2, responses.size());
        assertEquals(ClassSubjectLinkStatus.PENDING, responses.get(0).status());
        assertEquals(ClassSubjectLinkStatus.APPROVED, responses.get(1).status());
    }

    private ClassSubjectLink pendingLink() {
        ClassSubjectLink link = new ClassSubjectLink();
        setId(link);
        link.setClassSection(classSection);
        link.setSubject(subject);
        link.setRequestedBy(adviser);
        link.setStatus(ClassSubjectLinkStatus.PENDING);
        return link;
    }

    private Teacher teacher(String email, String name) {
        User user = new User();
        setId(user);
        user.setEmail(email);
        user.setRole(Role.TEACHER);
        Teacher teacher = new Teacher();
        setId(teacher);
        teacher.setUser(user);
        teacher.setName(name);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(teacherRepository.findByUserId(user.getId())).thenReturn(Optional.of(teacher));
        return teacher;
    }

    private void setId(BaseEntity entity) {
        entity.setId(UUID.randomUUID());
    }
}
