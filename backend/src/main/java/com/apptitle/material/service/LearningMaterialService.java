package com.apptitle.material.service;

import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.common.exception.ApiException;
import com.apptitle.config.MinioProperties;
import com.apptitle.material.dto.CompleteUploadRequest;
import com.apptitle.material.dto.InitUploadRequest;
import com.apptitle.material.dto.InitUploadResponse;
import com.apptitle.material.dto.LearningMaterialResponse;
import com.apptitle.material.dto.MaterialDownloadResponse;
import com.apptitle.material.entity.LearningMaterial;
import com.apptitle.material.entity.LearningMaterialStatus;
import com.apptitle.material.entity.MaterialChunk;
import com.apptitle.material.repository.LearningMaterialRepository;
import com.apptitle.material.storage.MultipartStorage;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.subject.entity.ClassSubjectLink;
import com.apptitle.subject.entity.ClassSubjectLinkStatus;
import com.apptitle.subject.repository.ClassSubjectLinkRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class LearningMaterialService {

    public static final long CHUNK_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_PARTS = 10_000;
    private static final Map<String, Set<String>> ALLOWED_TYPES = allowedTypes();

    private final LearningMaterialRepository learningMaterialRepository;
    private final ClassSubjectLinkRepository classSubjectLinkRepository;
    private final ClassEnrollmentRequestRepository classEnrollmentRequestRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final MultipartStorage multipartStorage;
    private final MinioProperties minioProperties;

    public LearningMaterialService(
            LearningMaterialRepository learningMaterialRepository,
            ClassSubjectLinkRepository classSubjectLinkRepository,
            ClassEnrollmentRequestRepository classEnrollmentRequestRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            MultipartStorage multipartStorage,
            MinioProperties minioProperties
    ) {
        this.learningMaterialRepository = learningMaterialRepository;
        this.classSubjectLinkRepository = classSubjectLinkRepository;
        this.classEnrollmentRequestRepository = classEnrollmentRequestRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.multipartStorage = multipartStorage;
        this.minioProperties = minioProperties;
    }

    @Transactional
    public InitUploadResponse initUpload(
            String teacherEmail,
            UUID classSectionId,
            UUID subjectId,
            InitUploadRequest request
    ) {
        Teacher teacher = resolveTeacher(teacherEmail);
        ClassSubjectLink link = resolveApprovedLink(classSectionId, subjectId);
        requireSubjectOwnership(teacher, link);
        validateUpload(request);

        String fileName = leafFileName(request.fileName());
        String storageKey = buildStorageKey(classSectionId, subjectId, fileName);
        int totalParts = Math.toIntExact(
                (request.totalSizeBytes() + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES);

        String uploadId = multipartStorage.initiateUpload(storageKey);

        LearningMaterial material = new LearningMaterial();
        material.setClassSubjectLink(link);
        material.setUploadedBy(teacher);
        material.setTitle(request.title().trim());
        material.setDescription(normalizeDescription(request.description()));
        material.setFileName(fileName);
        material.setContentType(request.contentType().trim().toLowerCase(Locale.ROOT));
        material.setStorageKey(storageKey);
        material.setFileSizeBytes(request.totalSizeBytes());
        material.setUploadId(uploadId);
        material.setChunkSizeBytes(CHUNK_SIZE_BYTES);
        material.setTotalParts(totalParts);
        material.setStatus(LearningMaterialStatus.INITIALIZED);
        material = learningMaterialRepository.save(material);

        List<InitUploadResponse.PresignedPartUrl> urls = multipartStorage
                .presignUploadParts(storageKey, uploadId, totalParts)
                .stream()
                .map(part -> new InitUploadResponse.PresignedPartUrl(
                        part.partNumber(), part.url()))
                .toList();

        return new InitUploadResponse(
                material.getId(),
                uploadId,
                storageKey,
                CHUNK_SIZE_BYTES,
                totalParts,
                urls
        );
    }

    @Transactional
    public LearningMaterialResponse completeUpload(
            String teacherEmail,
            UUID materialId,
            CompleteUploadRequest request
    ) {
        Teacher teacher = resolveTeacher(teacherEmail);
        LearningMaterial material = resolveMaterial(materialId);
        requireMaterialOwnership(teacher, material);
        requireMatchingUpload(material, request);

        List<CompleteUploadRequest.CompletedPart> parts = validateAndSortParts(
                request.parts(), material.getTotalParts());

        material.setStatus(LearningMaterialStatus.COMPLETING);
        multipartStorage.completeUpload(
                material.getStorageKey(),
                material.getUploadId(),
                parts.stream()
                        .map(part -> new MultipartStorage.CompletedPart(
                                part.partNumber(), part.eTag().trim()))
                        .toList()
        );

        material.getChunks().clear();
        for (CompleteUploadRequest.CompletedPart part : parts) {
            MaterialChunk chunk = new MaterialChunk();
            chunk.setLearningMaterial(material);
            chunk.setPartNumber(part.partNumber());
            chunk.setETag(part.eTag().trim());
            chunk.setSizeBytes(partSize(material, part.partNumber()));
            material.getChunks().add(chunk);
        }
        material.setStatus(LearningMaterialStatus.COMPLETED);
        material.setCompletedAt(Instant.now());
        material = learningMaterialRepository.save(material);
        return toResponse(material);
    }

    @Transactional(readOnly = true)
    public List<LearningMaterialResponse> listForTeacher(
            String teacherEmail,
            UUID classSectionId,
            UUID subjectId
    ) {
        Teacher teacher = resolveTeacher(teacherEmail);
        ClassSubjectLink link = resolveApprovedLink(classSectionId, subjectId);
        requireSubjectOwnership(teacher, link);
        return listCompleted(link);
    }

    @Transactional(readOnly = true)
    public MaterialDownloadResponse getTeacherDownload(
            String teacherEmail,
            UUID materialId
    ) {
        Teacher teacher = resolveTeacher(teacherEmail);
        LearningMaterial material = resolveCompletedMaterial(materialId);
        requireMaterialOwnership(teacher, material);
        return createDownload(material);
    }

    @Transactional(readOnly = true)
    public List<LearningMaterialResponse> listForStudent(
            String studentEmail,
            UUID subjectId
    ) {
        Student student = resolveStudent(studentEmail);
        UUID classSectionId = resolveApprovedClassSectionId(student);
        ClassSubjectLink link = resolveApprovedLink(classSectionId, subjectId);
        return listCompleted(link);
    }

    @Transactional(readOnly = true)
    public MaterialDownloadResponse getStudentDownload(
            String studentEmail,
            UUID materialId
    ) {
        Student student = resolveStudent(studentEmail);
        LearningMaterial material = resolveCompletedMaterial(materialId);
        UUID approvedClassSectionId = resolveApprovedClassSectionId(student);
        ClassSubjectLink link = material.getClassSubjectLink();
        if (link.getStatus() != ClassSubjectLinkStatus.APPROVED
                || !link.getClassSection().getId().equals(approvedClassSectionId)) {
            throw ApiException.forbidden("You do not have access to this learning material.");
        }
        return createDownload(material);
    }

    private List<LearningMaterialResponse> listCompleted(ClassSubjectLink link) {
        return learningMaterialRepository
                .findByClassSubjectLinkIdAndStatusOrderByCreatedAtDesc(
                        link.getId(), LearningMaterialStatus.COMPLETED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ClassSubjectLink resolveApprovedLink(UUID classSectionId, UUID subjectId) {
        ClassSubjectLink link = classSubjectLinkRepository
                .findByClassSectionIdAndSubjectId(classSectionId, subjectId)
                .orElseThrow(() -> ApiException.notFound("Class-subject link not found."));
        if (link.getStatus() != ClassSubjectLinkStatus.APPROVED) {
            throw ApiException.forbidden("The subject link is not approved.");
        }
        return link;
    }

    private void requireSubjectOwnership(Teacher teacher, ClassSubjectLink link) {
        if (!link.getSubject().getSubjectTeacher().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not own this subject.");
        }
    }

    private void requireMaterialOwnership(Teacher teacher, LearningMaterial material) {
        if (!material.getUploadedBy().getId().equals(teacher.getId())
                || !material.getClassSubjectLink().getSubject()
                        .getSubjectTeacher().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not own this learning material.");
        }
    }

    private void requireMatchingUpload(
            LearningMaterial material,
            CompleteUploadRequest request
    ) {
        if (material.getStatus() != LearningMaterialStatus.INITIALIZED) {
            throw ApiException.conflict("This upload is not awaiting completion.");
        }
        if (!material.getUploadId().equals(request.uploadId())
                || !material.getStorageKey().equals(request.fileKey())) {
            throw ApiException.badRequest("Upload ID or file key does not match the initialized upload.");
        }
    }

    private List<CompleteUploadRequest.CompletedPart> validateAndSortParts(
            List<CompleteUploadRequest.CompletedPart> requestParts,
            int expectedCount
    ) {
        if (requestParts.size() != expectedCount) {
            throw ApiException.badRequest("Uploaded part count does not match the initialized upload.");
        }
        Set<Integer> seen = new HashSet<>();
        List<CompleteUploadRequest.CompletedPart> parts = new ArrayList<>(requestParts);
        parts.sort(Comparator.comparingInt(CompleteUploadRequest.CompletedPart::partNumber));
        for (int index = 0; index < parts.size(); index++) {
            CompleteUploadRequest.CompletedPart part = parts.get(index);
            int expectedPartNumber = index + 1;
            if (part.partNumber() != expectedPartNumber || !seen.add(part.partNumber())) {
                throw ApiException.badRequest("Parts must be unique and numbered consecutively from 1.");
            }
            if (part.eTag() == null || part.eTag().isBlank() || part.eTag().length() > 255) {
                throw ApiException.badRequest("Every uploaded part must include a valid ETag.");
            }
        }
        return List.copyOf(parts);
    }

    private void validateUpload(InitUploadRequest request) {
        if (request.totalSizeBytes() <= 0) {
            throw ApiException.badRequest("File size must be greater than zero.");
        }
        if (request.totalSizeBytes() > minioProperties.maxFileSizeBytes()) {
            throw ApiException.badRequest("File exceeds the maximum allowed size.");
        }
        long totalParts = (request.totalSizeBytes() + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES;
        if (totalParts > MAX_PARTS) {
            throw ApiException.badRequest("File requires too many upload parts.");
        }

        String fileName = leafFileName(request.fileName());
        int extensionSeparator = fileName.lastIndexOf('.');
        if (extensionSeparator < 1 || extensionSeparator == fileName.length() - 1) {
            throw ApiException.badRequest("File must have a supported extension.");
        }
        String extension = fileName.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        String contentType = request.contentType().trim().toLowerCase(Locale.ROOT);
        Set<String> expectedTypes = ALLOWED_TYPES.get(extension);
        if (expectedTypes == null || !expectedTypes.contains(contentType)) {
            throw ApiException.badRequest("File type is not supported or does not match its extension.");
        }
    }

    private Teacher resolveTeacher(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only teachers can manage materials."));
    }

    private Student resolveStudent(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only students can access student materials."));
    }

    private UUID resolveApprovedClassSectionId(Student student) {
        return classEnrollmentRequestRepository
                .findByStudentIdAndStatus(student.getId(), ClassEnrollmentRequestStatus.APPROVED)
                .stream()
                .findFirst()
                .orElseThrow(() -> ApiException.forbidden(
                        "You need an approved class enrollment to access materials."))
                .getClassSection()
                .getId();
    }

    private LearningMaterial resolveMaterial(UUID materialId) {
        return learningMaterialRepository.findById(materialId)
                .orElseThrow(() -> ApiException.notFound("Learning material not found."));
    }

    private LearningMaterial resolveCompletedMaterial(UUID materialId) {
        LearningMaterial material = resolveMaterial(materialId);
        if (material.getStatus() != LearningMaterialStatus.COMPLETED) {
            throw ApiException.notFound("Learning material is not available.");
        }
        return material;
    }

    private MaterialDownloadResponse createDownload(LearningMaterial material) {
        String url = multipartStorage.presignDownload(material.getStorageKey());
        return new MaterialDownloadResponse(
                material.getId(),
                material.getFileName(),
                material.getContentType(),
                material.getFileSizeBytes(),
                url,
                Instant.now().plusSeconds(minioProperties.presignedExpirySeconds())
        );
    }

    private LearningMaterialResponse toResponse(LearningMaterial material) {
        ClassSubjectLink link = material.getClassSubjectLink();
        return new LearningMaterialResponse(
                material.getId(),
                link.getClassSection().getId(),
                link.getSubject().getId(),
                link.getSubject().getName(),
                material.getTitle(),
                material.getDescription(),
                material.getFileName(),
                material.getContentType(),
                material.getFileSizeBytes(),
                material.getUploadedBy().getName(),
                material.getCreatedAt(),
                material.getCompletedAt()
        );
    }

    private long partSize(LearningMaterial material, int partNumber) {
        if (partNumber < material.getTotalParts()) {
            return material.getChunkSizeBytes();
        }
        return material.getFileSizeBytes()
                - material.getChunkSizeBytes() * (material.getTotalParts() - 1L);
    }

    private String buildStorageKey(UUID classSectionId, UUID subjectId, String fileName) {
        String safeName = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        return "materials/%s/%s/%s/%s".formatted(
                classSectionId, subjectId, UUID.randomUUID(), safeName);
    }

    private String leafFileName(String suppliedName) {
        String normalized = suppliedName.trim().replace('\\', '/');
        String leaf = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (leaf.isBlank() || leaf.equals(".") || leaf.equals("..")) {
            throw ApiException.badRequest("File name is invalid.");
        }
        return leaf;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private static Map<String, Set<String>> allowedTypes() {
        Map<String, Set<String>> types = new HashMap<>();
        types.put("pdf", Set.of("application/pdf"));
        types.put("ppt", Set.of("application/vnd.ms-powerpoint"));
        types.put("pptx", Set.of(
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        types.put("doc", Set.of("application/msword"));
        types.put("docx", Set.of(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        return Map.copyOf(types);
    }
}
