package fyp.example.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_submission_question", columnNames = {"submission_id", "question_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAnswer {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "selected_option", length = 1)
    private String selectedOption;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    @Column(name = "marks_obtained", nullable = false)
    private int marksObtained;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
