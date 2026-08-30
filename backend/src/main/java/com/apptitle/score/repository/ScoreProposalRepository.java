package com.apptitle.score.repository;

import com.apptitle.score.entity.ScoreProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScoreProposalRepository extends JpaRepository<ScoreProposal, UUID> {
    Optional<ScoreProposal> findByActivityIdAndStudentId(UUID activityId, UUID studentId);
    List<ScoreProposal> findByActivityIdOrderBySubmittedAtAsc(UUID activityId);
    Optional<ScoreProposal> findByProofFileId(UUID proofFileId);
}
