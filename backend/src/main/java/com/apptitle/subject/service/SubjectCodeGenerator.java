package com.apptitle.subject.service;

import com.apptitle.subject.repository.SubjectRepository;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Generates unique 7-character subject codes for Subject entities.
 */
@Component
public class SubjectCodeGenerator {

    private final SubjectRepository subjectRepository;
    private final Random random = new Random();

    public SubjectCodeGenerator(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public String generateUnique() {
        String code;
        do {
            code = generateRandomCode();
        } while (subjectRepository.existsBySubjectCode(code));
        return code;
    }

    private String generateRandomCode() {
        // Generate 7-character alphanumeric code with 4 letters and 3 digits
        StringBuilder code = new StringBuilder();
        
        // First 4 characters: uppercase letters
        for (int i = 0; i < 4; i++) {
            code.append((char) (random.nextInt(26) + 'A'));
        }
        
        // Last 3 characters: digits
        for (int i = 0; i < 3; i++) {
            code.append((char) (random.nextInt(10) + '0'));
        }
        
        return code.toString();
    }
}