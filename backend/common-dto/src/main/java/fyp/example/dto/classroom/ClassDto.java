package fyp.example.dto.classroom;

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
public class ClassDto {
    private UUID id;
    private String name;
    private String subject;
    private String classCode;
    private UUID teacherId;
    private String description;
    private Instant createdAt;
}
