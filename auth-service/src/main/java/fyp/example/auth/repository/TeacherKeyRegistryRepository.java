package fyp.example.auth.repository;

import fyp.example.auth.entity.TeacherKeyRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherKeyRegistryRepository extends JpaRepository<TeacherKeyRegistry, UUID> {
    Optional<TeacherKeyRegistry> findByTeacherEmailIgnoreCase(String teacherEmail);
    boolean existsByTeacherEmailIgnoreCase(String teacherEmail);
}
