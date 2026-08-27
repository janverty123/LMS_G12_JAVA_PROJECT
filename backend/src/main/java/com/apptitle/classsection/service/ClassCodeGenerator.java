package com.apptitle.classsection.service;

import com.apptitle.classsection.repository.ClassSectionRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ClassCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final ClassSectionRepository classSectionRepository;

    public ClassCodeGenerator(ClassSectionRepository classSectionRepository) {
        this.classSectionRepository = classSectionRepository;
    }

    public String generateUnique() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = generate();
            if (!classSectionRepository.existsByClassCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not generate a unique class code after " + MAX_ATTEMPTS + " attempts.");
    }

    private String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
