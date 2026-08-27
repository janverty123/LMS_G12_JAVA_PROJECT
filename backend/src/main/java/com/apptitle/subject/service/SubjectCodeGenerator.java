package com.apptitle.subject.service;

import com.apptitle.subject.repository.SubjectRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates unique 7-character subject codes for Subject entities.
 */
@Component
public class SubjectCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 10;

    private final SubjectRepository subjectRepository;
    private final SecureRandom random = new SecureRandom();

    public SubjectCodeGenerator(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public String generateUnique() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = generate();
            if (!subjectRepository.existsBySubjectCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not generate a unique subject code after " + MAX_ATTEMPTS + " attempts.");
    }

    private String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
