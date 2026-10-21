package fyp.example.analytics.client;

import fyp.example.dto.classroom.ClassVerificationResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "classroom-service", url = "${classroom-service.url:http://localhost:8082}")
public interface ClassroomClient {

    @GetMapping("/internal/classes/{classId}/verify-teacher/{teacherId}")
    ClassVerificationResponseDto verifyTeacher(
            @PathVariable("classId") UUID classId,
            @PathVariable("teacherId") UUID teacherId,
            @RequestHeader("X-Internal-Token") String internalToken
    );

    @GetMapping("/internal/classes/{classId}/verify-student/{studentId}")
    ClassVerificationResponseDto verifyStudent(
            @PathVariable("classId") UUID classId,
            @PathVariable("studentId") UUID studentId,
            @RequestHeader("X-Internal-Token") String internalToken
    );

    @GetMapping("/internal/classes/{classId}/roster")
    List<UUID> getClassRoster(
            @PathVariable("classId") UUID classId,
            @RequestHeader("X-Internal-Token") String internalToken
    );
}
