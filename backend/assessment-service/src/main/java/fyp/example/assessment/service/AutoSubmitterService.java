package fyp.example.assessment.service;

import fyp.example.assessment.entity.Submission;
import fyp.example.assessment.entity.TestEntity;
import fyp.example.assessment.repository.SubmissionRepository;
import fyp.example.assessment.repository.TestRepository;
import fyp.example.dto.enums.TestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoSubmitterService {

    private final SubmissionRepository submissionRepository;
    private final TestRepository testRepository;
    private final AssessmentService assessmentService;
    private final NotificationDispatcherService notificationDispatcherService;

    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    @SchedulerLock(name = "autoSubmitterTask", lockAtLeastFor = "PT10S", lockAtMostFor = "PT2M")
    public void autoSubmitExpiredTests() {
        processExpiredSubmissions();
    }

    @Transactional
    public void processExpiredSubmissions() {
        List<Submission> inProgressList = submissionRepository.findByStatus(TestStatus.IN_PROGRESS);
        if (inProgressList.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        log.info("AutoSubmitter checking {} in-progress submission(s)", inProgressList.size());

        for (Submission sub : inProgressList) {
            try {
                TestEntity test = testRepository.findById(sub.getTestId()).orElse(null);
                if (test == null) {
                    continue;
                }

                Instant deadline = assessmentService.calculateDeadline(sub.getStartedAt(), test.getDurationMinutes(), test.getEndTime());
                if (now.isAfter(deadline)) {
                    log.info("Auto-submitting expired submission {} for student {} on test {}", sub.getId(), sub.getStudentId(), test.getId());
                    assessmentService.gradeSubmission(sub, test, null, TestStatus.AUTO_SUBMITTED, deadline);

                    notificationDispatcherService.notifyStudentTestEvaluated(
                            sub.getStudentId(),
                            test.getTitle(),
                            sub.getScore(),
                            sub.getTotalMarks(),
                            test.getId()
                    );
                }
            } catch (Exception e) {
                log.error("Failed to auto-submit submission {}: {}", sub.getId(), e.getMessage(), e);
            }
        }
    }
}
