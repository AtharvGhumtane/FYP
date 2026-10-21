package fyp.example.analytics.repository;

import fyp.example.analytics.entity.AiReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiReportRepository extends JpaRepository<AiReport, UUID> {

    Optional<AiReport> findTopByStudentIdOrderByGeneratedAtDesc(UUID studentId);

    Optional<AiReport> findTopByClassIdOrderByGeneratedAtDesc(UUID classId);
}
