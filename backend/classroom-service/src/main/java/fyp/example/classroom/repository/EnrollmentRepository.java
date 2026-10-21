package fyp.example.classroom.repository;

import fyp.example.classroom.entity.Enrollment;
import fyp.example.dto.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    Optional<Enrollment> findByClassEntityIdAndStudentId(UUID classId, UUID studentId);

    List<Enrollment> findByClassEntityIdAndStatus(UUID classId, EnrollmentStatus status);

    List<Enrollment> findByStudentIdAndStatus(UUID studentId, EnrollmentStatus status);

    List<Enrollment> findByStudentId(UUID studentId);

    @Query("SELECT e.studentId FROM Enrollment e WHERE e.classEntity.id = :classId AND e.status = :status")
    List<UUID> findStudentIdsByClassIdAndStatus(@Param("classId") UUID classId, @Param("status") EnrollmentStatus status);

    boolean existsByClassEntityIdAndStudentIdAndStatus(UUID classId, UUID studentId, EnrollmentStatus status);
}
