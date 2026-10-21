package fyp.example.classroom.repository;

import fyp.example.classroom.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, UUID> {
    List<ClassEntity> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId);
}
