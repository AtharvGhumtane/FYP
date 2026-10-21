package fyp.example.assessment.repository;

import fyp.example.assessment.entity.TestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestRepository extends JpaRepository<TestEntity, UUID> {
    List<TestEntity> findByClassId(UUID classId);
    List<TestEntity> findByClassIdAndIsPublishedTrue(UUID classId);
    List<TestEntity> findByTeacherId(UUID teacherId);
}
