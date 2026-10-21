package fyp.example.assessment.service;

import fyp.example.assessment.client.ClassroomClient;
import fyp.example.assessment.entity.Question;
import fyp.example.assessment.entity.StudentAnswer;
import fyp.example.assessment.entity.Submission;
import fyp.example.assessment.entity.TestEntity;
import fyp.example.assessment.repository.QuestionRepository;
import fyp.example.assessment.repository.StudentAnswerRepository;
import fyp.example.assessment.repository.SubmissionRepository;
import fyp.example.assessment.repository.TestRepository;
import fyp.example.dto.assessment.AggregatedAnalyticsDto;
import fyp.example.dto.assessment.AnswerSubmissionDto;
import fyp.example.dto.assessment.AutosaveAnswersDto;
import fyp.example.dto.assessment.CreateQuestionDto;
import fyp.example.dto.assessment.CreateTestDto;
import fyp.example.dto.assessment.QuestionDto;
import fyp.example.dto.assessment.ScorecardDto;
import fyp.example.dto.assessment.StartTestResponseDto;
import fyp.example.dto.assessment.StudentAnswerScoreDto;
import fyp.example.dto.assessment.SubmitTestRequestDto;
import fyp.example.dto.assessment.TestDetailDto;
import fyp.example.dto.classroom.ClassVerificationResponseDto;
import fyp.example.dto.enums.TestStatus;
import fyp.example.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final SubmissionRepository submissionRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final ClassroomClient classroomClient;
    private final NotificationDispatcherService notificationDispatcherService;

    @Value("${app.security.internal-service-key:}")
    private String internalServiceKey;

    @Transactional
    public TestDetailDto createTest(CreateTestDto dto, UUID teacherId) {
        ClassVerificationResponseDto verification = classroomClient.verifyTeacher(dto.getClassId(), teacherId, internalServiceKey);
        if (verification == null || !verification.isAuthorized()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to create tests for this class");
        }

        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }

        int totalMarks = dto.getQuestions().stream().mapToInt(CreateQuestionDto::getMarks).sum();
        UUID testId = UUID.randomUUID();

        TestEntity test = TestEntity.builder()
                .id(testId)
                .classId(dto.getClassId())
                .teacherId(teacherId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .durationMinutes(dto.getDurationMinutes())
                .totalMarks(totalMarks)
                .isPublished(true)
                .createdAt(Instant.now())
                .questions(new ArrayList<>())
                .build();

        for (CreateQuestionDto qDto : dto.getQuestions()) {
            Question question = Question.builder()
                    .id(UUID.randomUUID())
                    .test(test)
                    .topic(qDto.getTopic())
                    .questionText(qDto.getQuestionText())
                    .optionA(qDto.getOptionA())
                    .optionB(qDto.getOptionB())
                    .optionC(qDto.getOptionC())
                    .optionD(qDto.getOptionD())
                    .correctOption(qDto.getCorrectOption().toUpperCase())
                    .marks(qDto.getMarks())
                    .explanation(qDto.getExplanation())
                    .build();
            test.getQuestions().add(question);
        }

        TestEntity saved = testRepository.save(test);

        // Notify enrolled students
        try {
            List<UUID> roster = classroomClient.getClassRoster(dto.getClassId(), internalServiceKey);
            if (roster != null && !roster.isEmpty()) {
                notificationDispatcherService.notifyStudentsTestScheduled(roster, saved.getTitle(), saved.getId());
            }
        } catch (Exception e) {
            log.warn("Non-fatal: Failed to dispatch test notification for test {}: {}", saved.getId(), e.getMessage());
        }

        return mapToTestDetailDto(saved, true);
    }

    @Transactional(readOnly = true)
    public List<TestDetailDto> getTestsForClass(UUID classId, UserPrincipal principal) {
        if (principal.getRole().name().equals("ROLE_TEACHER")) {
            ClassVerificationResponseDto verification = classroomClient.verifyTeacher(classId, principal.getId(), internalServiceKey);
            if (verification == null || !verification.isAuthorized()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view tests for this class");
            }
            List<TestEntity> tests = testRepository.findByClassId(classId);
            return tests.stream().map(t -> mapToTestDetailDto(t, true)).collect(Collectors.toList());
        } else {
            ClassVerificationResponseDto verification = classroomClient.verifyStudent(classId, principal.getId(), internalServiceKey);
            if (verification == null || !verification.isAuthorized()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not enrolled in this class");
            }
            List<TestEntity> tests = testRepository.findByClassIdAndIsPublishedTrue(classId);
            return tests.stream().map(t -> mapToTestDetailDto(t, false)).collect(Collectors.toList());
        }
    }

    @Transactional(readOnly = true)
    public TestDetailDto getTestById(UUID testId, UserPrincipal principal) {
        TestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));

        if (principal.getRole().name().equals("ROLE_TEACHER")) {
            ClassVerificationResponseDto verification = classroomClient.verifyTeacher(test.getClassId(), principal.getId(), internalServiceKey);
            if (verification == null || !verification.isAuthorized()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this test");
            }
            return mapToTestDetailDto(test, true);
        } else {
            ClassVerificationResponseDto verification = classroomClient.verifyStudent(test.getClassId(), principal.getId(), internalServiceKey);
            if (verification == null || !verification.isAuthorized()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not enrolled in this test's class");
            }
            return mapToTestDetailDto(test, false);
        }
    }

    @Transactional
    public StartTestResponseDto startTest(UUID testId, UUID studentId) {
        TestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));

        if (!test.isPublished()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test is not published");
        }

        Instant now = Instant.now();
        if (now.isBefore(test.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test has not started yet");
        }
        if (now.isAfter(test.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test has already ended");
        }

        ClassVerificationResponseDto verification = classroomClient.verifyStudent(test.getClassId(), studentId, internalServiceKey);
        if (verification == null || !verification.isAuthorized()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not enrolled in this class");
        }

        Optional<Submission> existingOpt = submissionRepository.findByTestIdAndStudentId(testId, studentId);
        Submission submission;

        if (existingOpt.isPresent()) {
            submission = existingOpt.get();
            if (submission.getStatus() == TestStatus.SUBMITTED || submission.getStatus() == TestStatus.AUTO_SUBMITTED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test has already been submitted");
            }
            Instant deadline = calculateDeadline(submission.getStartedAt(), test.getDurationMinutes(), test.getEndTime());
            if (now.isAfter(deadline)) {
                gradeSubmission(submission, test, null, TestStatus.AUTO_SUBMITTED, deadline);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test time has expired");
            }
        } else {
            submission = Submission.builder()
                    .id(UUID.randomUUID())
                    .testId(testId)
                    .studentId(studentId)
                    .startedAt(now)
                    .score(0)
                    .totalMarks(test.getTotalMarks())
                    .percentage(0.0)
                    .status(TestStatus.IN_PROGRESS)
                    .build();
            submission = submissionRepository.save(submission);
        }

        Instant deadline = calculateDeadline(submission.getStartedAt(), test.getDurationMinutes(), test.getEndTime());

        List<QuestionDto> questionDtos = test.getQuestions().stream()
                .map(q -> QuestionDto.builder()
                        .id(q.getId())
                        .topic(q.getTopic())
                        .questionText(q.getQuestionText())
                        .optionA(q.getOptionA())
                        .optionB(q.getOptionB())
                        .optionC(q.getOptionC())
                        .optionD(q.getOptionD())
                        .marks(q.getMarks())
                        .build())
                .collect(Collectors.toList());

        return StartTestResponseDto.builder()
                .submissionId(submission.getId())
                .testId(test.getId())
                .title(test.getTitle())
                .startedAt(submission.getStartedAt())
                .serverDeadline(deadline)
                .durationMinutes(test.getDurationMinutes())
                .questions(questionDtos)
                .build();
    }

    @Transactional
    public void autosaveAnswers(UUID testId, UUID studentId, AutosaveAnswersDto dto) {
        TestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));

        Submission submission = submissionRepository.findByTestIdAndStudentId(testId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active test session found"));

        if (submission.getStatus() != TestStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test is not in progress");
        }

        Instant deadline = calculateDeadline(submission.getStartedAt(), test.getDurationMinutes(), test.getEndTime());
        if (Instant.now().isAfter(deadline)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission window has expired");
        }

        if (dto.getAnswers() != null) {
            for (AnswerSubmissionDto item : dto.getAnswers()) {
                StudentAnswer answer = studentAnswerRepository
                        .findBySubmissionIdAndQuestionId(submission.getId(), item.getQuestionId())
                        .orElseGet(() -> StudentAnswer.builder()
                                .id(UUID.randomUUID())
                                .submission(submission)
                                .questionId(item.getQuestionId())
                                .build());

                answer.setSelectedOption(item.getSelectedOption());
                answer.setUpdatedAt(Instant.now());
                studentAnswerRepository.save(answer);
            }
        }
    }

    @Transactional
    public ScorecardDto submitTest(UUID testId, UUID studentId, SubmitTestRequestDto dto) {
        TestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));

        Submission submission = submissionRepository.findByTestIdAndStudentId(testId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active test session found"));

        if (submission.getStatus() == TestStatus.SUBMITTED || submission.getStatus() == TestStatus.AUTO_SUBMITTED) {
            return getMySubmission(testId, studentId);
        }

        List<AnswerSubmissionDto> answers = dto != null ? dto.getAnswers() : null;
        gradeSubmission(submission, test, answers, TestStatus.SUBMITTED, Instant.now());

        notificationDispatcherService.notifyStudentTestEvaluated(
                studentId,
                test.getTitle(),
                submission.getScore(),
                submission.getTotalMarks(),
                test.getId()
        );

        return buildScorecardDto(submission, test);
    }

    @Transactional(readOnly = true)
    public ScorecardDto getMySubmission(UUID testId, UUID studentId) {
        TestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));

        Submission submission = submissionRepository.findByTestIdAndStudentId(testId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        if (submission.getStatus() == TestStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test is still in progress");
        }

        return buildScorecardDto(submission, test);
    }

    @Transactional(readOnly = true)
    public List<ScorecardDto> getSubmissionsForTest(UUID testId, UUID teacherId) {
        TestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));

        ClassVerificationResponseDto verification = classroomClient.verifyTeacher(test.getClassId(), teacherId, internalServiceKey);
        if (verification == null || !verification.isAuthorized()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view submissions for this test");
        }

        List<Submission> submissions = submissionRepository.findByTestId(testId);
        return submissions.stream()
                .filter(s -> s.getStatus() != TestStatus.IN_PROGRESS)
                .map(s -> buildScorecardDto(s, test))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AggregatedAnalyticsDto getAggregatedAnalytics(UUID targetId, String type) {
        List<Submission> submissions;
        if ("class".equalsIgnoreCase(type)) {
            List<TestEntity> tests = testRepository.findByClassId(targetId);
            if (tests.isEmpty()) {
                return AggregatedAnalyticsDto.builder().targetId(targetId).totalSubmissions(0).build();
            }
            List<UUID> testIds = tests.stream().map(TestEntity::getId).collect(Collectors.toList());
            submissions = submissionRepository.findByTestIdIn(testIds);
        } else {
            submissions = submissionRepository.findByStudentId(targetId);
        }

        List<Submission> gradedSubmissions = submissions.stream()
                .filter(s -> s.getStatus() != TestStatus.IN_PROGRESS)
                .collect(Collectors.toList());

        if (gradedSubmissions.isEmpty()) {
            return AggregatedAnalyticsDto.builder().targetId(targetId).totalSubmissions(0).build();
        }

        int totalSubmissions = gradedSubmissions.size();
        double avgScore = gradedSubmissions.stream().mapToInt(Submission::getScore).average().orElse(0.0);
        double avgPercentage = gradedSubmissions.stream().mapToDouble(Submission::getPercentage).average().orElse(0.0);
        double highestScore = gradedSubmissions.stream().mapToInt(Submission::getScore).max().orElse(0);
        double lowestScore = gradedSubmissions.stream().mapToInt(Submission::getScore).min().orElse(0);

        // Score Distribution
        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("0-20%", 0);
        distribution.put("21-40%", 0);
        distribution.put("41-60%", 0);
        distribution.put("61-80%", 0);
        distribution.put("81-100%", 0);

        for (Submission s : gradedSubmissions) {
            double p = s.getPercentage();
            if (p <= 20.0) distribution.put("0-20%", distribution.get("0-20%") + 1);
            else if (p <= 40.0) distribution.put("21-40%", distribution.get("21-40%") + 1);
            else if (p <= 60.0) distribution.put("41-60%", distribution.get("41-60%") + 1);
            else if (p <= 80.0) distribution.put("61-80%", distribution.get("61-80%") + 1);
            else distribution.put("81-100%", distribution.get("81-100%") + 1);
        }

        // Score Trend
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));
        List<AggregatedAnalyticsDto.ScoreTrendPointDto> scoreTrend = new ArrayList<>();
        for (Submission s : gradedSubmissions) {
            TestEntity t = testRepository.findById(s.getTestId()).orElse(null);
            String title = t != null ? t.getTitle() : "Test";
            String date = s.getSubmittedAt() != null ? formatter.format(s.getSubmittedAt()) : "";
            scoreTrend.add(new AggregatedAnalyticsDto.ScoreTrendPointDto(title, s.getPercentage(), date));
        }

        // Topic accuracy and question miss rates
        List<UUID> submissionIds = gradedSubmissions.stream().map(Submission::getId).collect(Collectors.toList());
        List<StudentAnswer> allAnswers = studentAnswerRepository.findBySubmissionIdIn(submissionIds);

        Map<UUID, Question> questionMap = new HashMap<>();
        List<UUID> questionIds = allAnswers.stream().map(StudentAnswer::getQuestionId).distinct().collect(Collectors.toList());
        if (!questionIds.isEmpty()) {
            questionRepository.findAllById(questionIds).forEach(q -> questionMap.put(q.getId(), q));
        }

        Map<String, int[]> topicCounts = new HashMap<>(); // topic -> [correctCount, totalCount]
        Map<UUID, int[]> questionCounts = new HashMap<>(); // questionId -> [incorrectCount, totalCount]

        for (StudentAnswer ans : allAnswers) {
            Question q = questionMap.get(ans.getQuestionId());
            if (q != null) {
                topicCounts.putIfAbsent(q.getTopic(), new int[2]);
                topicCounts.get(q.getTopic())[1]++;
                if (ans.isCorrect()) {
                    topicCounts.get(q.getTopic())[0]++;
                }

                questionCounts.putIfAbsent(q.getId(), new int[2]);
                questionCounts.get(q.getId())[1]++;
                if (!ans.isCorrect()) {
                    questionCounts.get(q.getId())[0]++;
                }
            }
        }

        Map<String, Double> topicAccuracyMap = new HashMap<>();
        topicCounts.forEach((topic, counts) -> {
            double acc = counts[1] > 0 ? ((double) counts[0] / counts[1]) * 100.0 : 0.0;
            topicAccuracyMap.put(topic, Math.round(acc * 10.0) / 10.0);
        });

        Map<UUID, Double> questionMissRates = new HashMap<>();
        questionCounts.forEach((qid, counts) -> {
            double miss = counts[1] > 0 ? ((double) counts[0] / counts[1]) * 100.0 : 0.0;
            questionMissRates.put(qid, Math.round(miss * 10.0) / 10.0);
        });

        return AggregatedAnalyticsDto.builder()
                .targetId(targetId)
                .totalSubmissions(totalSubmissions)
                .averageScore(Math.round(avgScore * 10.0) / 10.0)
                .averagePercentage(Math.round(avgPercentage * 10.0) / 10.0)
                .highestScore(highestScore)
                .lowestScore(lowestScore)
                .topicAccuracyMap(topicAccuracyMap)
                .scoreTrend(scoreTrend)
                .scoreDistribution(distribution)
                .questionMissRates(questionMissRates)
                .build();
    }

    public void gradeSubmission(Submission submission, TestEntity test, List<AnswerSubmissionDto> finalAnswers, TestStatus targetStatus, Instant submittedAt) {
        if (finalAnswers != null) {
            for (AnswerSubmissionDto item : finalAnswers) {
                StudentAnswer answer = studentAnswerRepository
                        .findBySubmissionIdAndQuestionId(submission.getId(), item.getQuestionId())
                        .orElseGet(() -> StudentAnswer.builder()
                                .id(UUID.randomUUID())
                                .submission(submission)
                                .questionId(item.getQuestionId())
                                .build());

                answer.setSelectedOption(item.getSelectedOption());
                answer.setUpdatedAt(Instant.now());
                studentAnswerRepository.save(answer);
            }
        }

        List<StudentAnswer> existingAnswers = studentAnswerRepository.findBySubmissionId(submission.getId());
        Map<UUID, StudentAnswer> answerMap = existingAnswers.stream()
                .collect(Collectors.toMap(StudentAnswer::getQuestionId, a -> a, (a1, a2) -> a1));

        int totalScore = 0;
        List<Question> questions = test.getQuestions();

        for (Question q : questions) {
            StudentAnswer ans = answerMap.get(q.getId());
            if (ans == null) {
                ans = StudentAnswer.builder()
                        .id(UUID.randomUUID())
                        .submission(submission)
                        .questionId(q.getId())
                        .selectedOption(null)
                        .build();
            }

            boolean isCorrect = ans.getSelectedOption() != null &&
                    ans.getSelectedOption().trim().equalsIgnoreCase(q.getCorrectOption().trim());
            int marksObtained = isCorrect ? q.getMarks() : 0;

            ans.setCorrect(isCorrect);
            ans.setMarksObtained(marksObtained);
            ans.setUpdatedAt(Instant.now());
            studentAnswerRepository.save(ans);

            totalScore += marksObtained;
        }

        submission.setScore(totalScore);
        submission.setTotalMarks(test.getTotalMarks());
        submission.setPercentage(test.getTotalMarks() > 0 ? ((double) totalScore / test.getTotalMarks()) * 100.0 : 0.0);
        submission.setStatus(targetStatus);
        submission.setSubmittedAt(submittedAt != null ? submittedAt : Instant.now());
        submissionRepository.save(submission);
    }

    public Instant calculateDeadline(Instant startedAt, int durationMinutes, Instant testEndTime) {
        Instant durationEnd = startedAt.plus(durationMinutes, ChronoUnit.MINUTES);
        return durationEnd.isBefore(testEndTime) ? durationEnd : testEndTime;
    }

    private TestDetailDto mapToTestDetailDto(TestEntity test, boolean includeCorrectAnswers) {
        List<QuestionDto> questionDtos = test.getQuestions() != null ? test.getQuestions().stream()
                .map(q -> QuestionDto.builder()
                        .id(q.getId())
                        .topic(q.getTopic())
                        .questionText(q.getQuestionText())
                        .optionA(q.getOptionA())
                        .optionB(q.getOptionB())
                        .optionC(q.getOptionC())
                        .optionD(q.getOptionD())
                        .marks(q.getMarks())
                        .correctOption(includeCorrectAnswers ? q.getCorrectOption() : null)
                        .explanation(includeCorrectAnswers ? q.getExplanation() : null)
                        .build())
                .collect(Collectors.toList()) : List.of();

        return TestDetailDto.builder()
                .id(test.getId())
                .classId(test.getClassId())
                .teacherId(test.getTeacherId())
                .title(test.getTitle())
                .description(test.getDescription())
                .startTime(test.getStartTime())
                .endTime(test.getEndTime())
                .durationMinutes(test.getDurationMinutes())
                .totalMarks(test.getTotalMarks())
                .isPublished(test.isPublished())
                .createdAt(test.getCreatedAt())
                .questions(questionDtos)
                .build();
    }

    private ScorecardDto buildScorecardDto(Submission submission, TestEntity test) {
        List<StudentAnswer> answers = studentAnswerRepository.findBySubmissionId(submission.getId());
        Map<UUID, StudentAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(StudentAnswer::getQuestionId, a -> a, (a1, a2) -> a1));

        List<StudentAnswerScoreDto> questionScores = new ArrayList<>();
        if (test.getQuestions() != null) {
            for (Question q : test.getQuestions()) {
                StudentAnswer ans = answerMap.get(q.getId());
                questionScores.add(StudentAnswerScoreDto.builder()
                        .questionId(q.getId())
                        .topic(q.getTopic())
                        .questionText(q.getQuestionText())
                        .optionA(q.getOptionA())
                        .optionB(q.getOptionB())
                        .optionC(q.getOptionC())
                        .optionD(q.getOptionD())
                        .correctOption(q.getCorrectOption())
                        .selectedOption(ans != null ? ans.getSelectedOption() : null)
                        .isCorrect(ans != null && ans.isCorrect())
                        .marksObtained(ans != null ? ans.getMarksObtained() : 0)
                        .maxMarks(q.getMarks())
                        .explanation(q.getExplanation())
                        .build());
            }
        }

        return ScorecardDto.builder()
                .submissionId(submission.getId())
                .testId(test.getId())
                .testTitle(test.getTitle())
                .studentId(submission.getStudentId())
                .score(submission.getScore())
                .totalMarks(submission.getTotalMarks())
                .percentage(submission.getPercentage())
                .status(submission.getStatus())
                .startedAt(submission.getStartedAt())
                .submittedAt(submission.getSubmittedAt())
                .questions(questionScores)
                .build();
    }
}
