package fyp.example.assessment.repository;

import fyp.example.assessment.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findByTestId(UUID testId);
    List<Question> findByTestIdIn(List<UUID> testIds);
}
