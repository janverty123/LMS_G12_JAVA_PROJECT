package com.apptitle.grade.service;

import com.apptitle.activity.entity.Activity;
import com.apptitle.activity.entity.ActivitySubmission;
import com.apptitle.activity.entity.ActivityType;
import com.apptitle.activity.entity.SubmissionStatus;
import com.apptitle.activity.repository.ActivityRepository;
import com.apptitle.activity.repository.ActivitySubmissionRepository;
import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.common.exception.ApiException;
import com.apptitle.grade.dto.GradeConfigurationRequest;
import com.apptitle.grade.dto.GradeConfigurationResponse;
import com.apptitle.grade.dto.GradebookResponse;
import com.apptitle.grade.entity.GradeConfiguration;
import com.apptitle.grade.repository.GradeConfigurationRepository;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.subject.entity.ClassSubjectLink;
import com.apptitle.subject.entity.ClassSubjectLinkStatus;
import com.apptitle.subject.repository.ClassSubjectLinkRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.repository.UserRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GradebookService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final GradeConfigurationRepository configurationRepository;
    private final ActivityRepository activityRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final ClassSubjectLinkRepository linkRepository;
    private final ClassEnrollmentRequestRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public GradebookService(GradeConfigurationRepository configurationRepository,
            ActivityRepository activityRepository,
            ActivitySubmissionRepository submissionRepository,
            ClassSubjectLinkRepository linkRepository,
            ClassEnrollmentRequestRepository enrollmentRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            UserRepository userRepository) {
        this.configurationRepository = configurationRepository;
        this.activityRepository = activityRepository;
        this.submissionRepository = submissionRepository;
        this.linkRepository = linkRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GradeConfigurationResponse configure(String email, UUID classId, UUID subjectId,
            GradeConfigurationRequest request) {
        ClassSubjectLink link = teacherLink(email, classId, subjectId);
        if (request.writtenActivityWeight().add(request.performanceTaskWeight())
                .add(request.testWeight()).compareTo(ONE_HUNDRED) != 0) {
            throw ApiException.badRequest("Grade weights must total exactly 100%.");
        }
        GradeConfiguration config = configurationRepository.findByClassSubjectLinkId(link.getId())
                .orElseGet(GradeConfiguration::new);
        config.setClassSubjectLink(link);
        config.setWrittenActivityWeight(request.writtenActivityWeight());
        config.setPerformanceTaskWeight(request.performanceTaskWeight());
        config.setTestWeight(request.testWeight());
        return configResponse(configurationRepository.save(config));
    }

    @Transactional(readOnly = true)
    public GradebookResponse teacherGradebook(String email, UUID classId, UUID subjectId) {
        return build(teacherLink(email, classId, subjectId), null);
    }

    @Transactional(readOnly = true)
    public GradebookResponse.StudentGrade studentGrades(String email, UUID subjectId) {
        Student student = resolveStudent(email);
        var enrollment = enrollmentRepository.findByStudentIdAndStatus(
                        student.getId(), ClassEnrollmentRequestStatus.APPROVED)
                .stream().findFirst().orElseThrow(() -> ApiException.forbidden(
                        "You need an approved enrollment to view grades."));
        ClassSubjectLink link = approvedLink(enrollment.getClassSection().getId(), subjectId);
        return build(link, student).students().getFirst();
    }

    @Transactional(readOnly = true)
    public byte[] export(String email, UUID classId, UUID subjectId) {
        GradebookResponse gradebook = teacherGradebook(email, classId, subjectId);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Gradebook");
            Row header = sheet.createRow(0);
            int column = 0;
            header.createCell(column++).setCellValue("Student Name");
            header.createCell(column++).setCellValue("LRN");
            for (var activity : gradebook.activities()) {
                header.createCell(column++).setCellValue(activity.title() + " / " + activity.perfectScore());
            }
            header.createCell(column++).setCellValue("Written Activity Average");
            header.createCell(column++).setCellValue("Performance Task Average");
            header.createCell(column++).setCellValue("Test Average");
            header.createCell(column).setCellValue("Final Grade");
            int rowIndex = 1;
            for (var student : gradebook.students()) {
                Row row = sheet.createRow(rowIndex++);
                column = 0;
                row.createCell(column++).setCellValue(student.studentName());
                row.createCell(column++).setCellValue(student.studentLrn());
                for (var score : student.scores()) {
                    if (score.score() != null) row.createCell(column).setCellValue(score.score().doubleValue());
                    column++;
                }
                row.createCell(column++).setCellValue(student.writtenActivityAverage().doubleValue());
                row.createCell(column++).setCellValue(student.performanceTaskAverage().doubleValue());
                row.createCell(column++).setCellValue(student.testAverage().doubleValue());
                row.createCell(column).setCellValue(student.finalGrade().doubleValue());
            }
            for (int index = 0; index <= column; index++) sheet.autoSizeColumn(index);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate gradebook export.", exception);
        }
    }

    private GradebookResponse build(ClassSubjectLink link, Student onlyStudent) {
        GradeConfiguration config = configurationRepository.findByClassSubjectLinkId(link.getId())
                .orElseThrow(() -> ApiException.conflict("Configure grade weights first."));
        List<Activity> activities = activityRepository
                .findByClassSubjectLinkIdOrderByDeadlineAsc(link.getId());
        List<Student> students = onlyStudent == null
                ? enrollmentRepository.findByClassSectionIdAndStatus(
                        link.getClassSection().getId(), ClassEnrollmentRequestStatus.APPROVED)
                        .stream().map(value -> value.getStudent()).toList()
                : List.of(onlyStudent);
        List<GradebookResponse.StudentGrade> rows = students.stream()
                .map(student -> studentGrade(student, activities, config)).toList();
        return new GradebookResponse(configResponse(config), activities.stream()
                .map(activity -> new GradebookResponse.ActivityColumn(activity.getId(),
                        activity.getTitle(), activity.getType(), activity.getPerfectScore()))
                .toList(), rows);
    }

    private GradebookResponse.StudentGrade studentGrade(Student student,
            List<Activity> activities, GradeConfiguration config) {
        Map<UUID, ActivitySubmission> submissions = new HashMap<>();
        for (Activity activity : activities) {
            submissionRepository.findByActivityIdAndStudentId(activity.getId(), student.getId())
                    .filter(value -> value.getStatus() == SubmissionStatus.GRADED)
                    .ifPresent(value -> submissions.put(activity.getId(), value));
        }
        Map<ActivityType, BigDecimal[]> totals = new EnumMap<>(ActivityType.class);
        for (ActivityType type : ActivityType.values()) {
            totals.put(type, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        List<GradebookResponse.ActivityScore> scores = new ArrayList<>();
        for (Activity activity : activities) {
            ActivitySubmission submission = submissions.get(activity.getId());
            BigDecimal score = submission == null ? null : submission.getScore();
            scores.add(new GradebookResponse.ActivityScore(activity.getId(), score,
                    activity.getPerfectScore()));
            if (score != null) {
                BigDecimal[] category = totals.get(activity.getType());
                category[0] = category[0].add(score);
                category[1] = category[1].add(activity.getPerfectScore());
            }
        }
        BigDecimal written = average(totals.get(ActivityType.WRITTEN_ACTIVITY));
        BigDecimal performance = average(totals.get(ActivityType.PERFORMANCE_TASK));
        BigDecimal test = average(totals.get(ActivityType.TEST));
        BigDecimal finalGrade = written.multiply(config.getWrittenActivityWeight())
                .add(performance.multiply(config.getPerformanceTaskWeight()))
                .add(test.multiply(config.getTestWeight()))
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        return new GradebookResponse.StudentGrade(student.getId(), student.getName(),
                student.getLrn(), scores, written, performance, test, finalGrade);
    }

    private BigDecimal average(BigDecimal[] totals) {
        return totals[1].signum() == 0 ? BigDecimal.ZERO
                : totals[0].multiply(ONE_HUNDRED).divide(totals[1], 2, RoundingMode.HALF_UP);
    }

    private ClassSubjectLink teacherLink(String email, UUID classId, UUID subjectId) {
        Teacher teacher = resolveTeacher(email);
        ClassSubjectLink link = approvedLink(classId, subjectId);
        if (!link.getSubject().getSubjectTeacher().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not own this subject.");
        }
        return link;
    }

    private ClassSubjectLink approvedLink(UUID classId, UUID subjectId) {
        ClassSubjectLink link = linkRepository.findByClassSectionIdAndSubjectId(classId, subjectId)
                .orElseThrow(() -> ApiException.notFound("Class-subject link not found."));
        if (link.getStatus() != ClassSubjectLinkStatus.APPROVED) {
            throw ApiException.forbidden("The subject link is not approved.");
        }
        return link;
    }

    private GradeConfigurationResponse configResponse(GradeConfiguration config) {
        var link = config.getClassSubjectLink();
        return new GradeConfigurationResponse(link.getClassSection().getId(),
                link.getSubject().getId(), config.getWrittenActivityWeight(),
                config.getPerformanceTaskWeight(), config.getTestWeight());
    }

    private Teacher resolveTeacher(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only teachers can access gradebooks."));
    }

    private Student resolveStudent(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only students can access student grades."));
    }
}
