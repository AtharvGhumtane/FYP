package fyp.example.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassDto {
    @NotBlank(message = "Class name is required")
    private String name;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String description;
}
