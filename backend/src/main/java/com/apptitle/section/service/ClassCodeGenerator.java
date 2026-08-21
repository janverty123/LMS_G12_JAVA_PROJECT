package com.apptitle.section.service;

import com.apptitle.section.repository.SectionRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates classroom join codes students type at registration.
 *
 * Alphabet deliberately excludes characters easily confused when
 * handwritten/read aloud by a teacher to a class: 0/O, 1/I/L.
 * Retries on collision (astronomically unlikely at this scale, but cheap
 * to guard against rather than assume).
 */
@Component
public class ClassCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final SectionRepository sectionRepository;

    public ClassCodeGenerator(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    public String generateUnique() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = generate();
            if (!sectionRepository.existsByClassCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique class code after " + MAX_ATTEMPTS + " attempts.");
    }

    private String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
