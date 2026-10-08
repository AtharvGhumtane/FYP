package fyp.example.dto.classroom;

import fyp.example.dto.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDto {
    private UUID id;
    private UUID classId;
    private String className;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private EnrollmentStatus status;
    private Instant requestedAt;
    private Instant respondedAt;
}
