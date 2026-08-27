package com.apptitle.classsection.service;

import com.apptitle.classsection.dto.ClassSectionResponse;
import com.apptitle.classsection.dto.CreateClassSectionRequest;
import com.apptitle.classsection.dto.UpdateClassSectionRequest;
import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.classsection.repository.ClassSectionRepository;
import com.apptitle.common.exception.ApiException;
import com.apptitle.subject.repository.ClassSubjectLinkRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing ClassSection entities.
 */
@Service
public class ClassSectionService {

    private final ClassSectionRepository classSectionRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRequestRepository classEnrollmentRequestRepository;
    private final ClassSubjectLinkRepository classSubjectLinkRepository;
    private final ClassCodeGenerator classCodeGenerator;

    public ClassSectionService(
            ClassSectionRepository classSectionRepository,
            TeacherRepository teacherRepository,
            UserRepository userRepository,
            ClassEnrollmentRequestRepository classEnrollmentRequestRepository,
            ClassSubjectLinkRepository classSubjectLinkRepository,
            ClassCodeGenerator classCodeGenerator
    ) {
        this.classSectionRepository = classSectionRepository;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.classEnrollmentRequestRepository = classEnrollmentRequestRepository;
        this.classSubjectLinkRepository = classSubjectLinkRepository;
        this.classCodeGenerator = classCodeGenerator;
    }

    @Transactional
    public ClassSectionResponse createClassSection(String teacherEmail, CreateClassSectionRequest request) {
        Teacher teacher = resolveTeacher(teacherEmail);

        ClassSection classSection = new ClassSection();
        classSection.setAdviser(teacher);
        classSection.setGradeLevel(request.gradeLevel().trim());
        classSection.setSection(request.section().trim());
        classSection.setSchoolYear(request.schoolYear().trim());
        classSection.setClassCode(classCodeGenerator.generateUnique());

        classSection = classSectionRepository.save(classSection);
        return toResponse(classSection);
    }

    @Transactional(readOnly = true)
    public List<ClassSectionResponse> listOwnClassSections(String teacherEmail) {
        Teacher teacher = resolveTeacher(teacherEmail);
        return classSectionRepository.findByAdviserId(teacher.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ClassSectionResponse updateClassSection(String teacherEmail, UUID classSectionId, UpdateClassSectionRequest request) {
        ClassSection classSection = resolveOwnedClassSection(teacherEmail, classSectionId);
        classSection.setGradeLevel(request.gradeLevel().trim());
        classSection.setSection(request.section().trim());
        classSection.setSchoolYear(request.schoolYear().trim());
        classSection = classSectionRepository.save(classSection);
        return toResponse(classSection);
    }

    @Transactional
    public void deleteClassSection(String teacherEmail, UUID classSectionId) {
        ClassSection classSection = resolveOwnedClassSection(teacherEmail, classSectionId);
        classSubjectLinkRepository.deleteByClassSectionId(classSection.getId());
        classEnrollmentRequestRepository.deleteByClassSectionId(classSection.getId());
        classSectionRepository.delete(classSection);
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

    private Teacher resolveTeacher(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only teachers can manage class sections."));
    }

    private ClassSectionResponse toResponse(ClassSection classSection) {
        return new ClassSectionResponse(
                classSection.getId(),
                classSection.getGradeLevel(),
                classSection.getSection(),
                classSection.getSchoolYear(),
                classSection.getClassCode(),
                classSection.getAdviser().getName()
        );
    }
}
