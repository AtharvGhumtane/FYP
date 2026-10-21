package fyp.example.dto.classroom;

import fyp.example.dto.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDecisionDto {
    @NotNull(message = "Decision status is required (ACCEPTED or REJECTED)")
    private EnrollmentStatus status;
}
