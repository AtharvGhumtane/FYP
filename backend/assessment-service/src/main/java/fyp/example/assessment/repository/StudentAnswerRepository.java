package fyp.example.assessment.repository;

import fyp.example.assessment.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, UUID> {
    List<StudentAnswer> findBySubmissionId(UUID submissionId);
    Optional<StudentAnswer> findBySubmissionIdAndQuestionId(UUID submissionId, UUID questionId);
    List<StudentAnswer> findBySubmissionIdIn(List<UUID> submissionIds);
}
