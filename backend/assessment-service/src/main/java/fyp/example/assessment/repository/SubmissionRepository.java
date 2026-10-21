package fyp.example.assessment.repository;

import fyp.example.assessment.entity.Submission;
import fyp.example.dto.enums.TestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    Optional<Submission> findByTestIdAndStudentId(UUID testId, UUID studentId);
    List<Submission> findByTestId(UUID testId);
    List<Submission> findByStudentId(UUID studentId);
    List<Submission> findByStatus(TestStatus status);
    List<Submission> findByTestIdIn(List<UUID> testIds);
}
