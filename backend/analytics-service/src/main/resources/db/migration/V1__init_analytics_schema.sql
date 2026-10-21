-- Analytics Service V1 Schema Migration

CREATE TABLE ai_reports (
    id UUID PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL,
    student_id UUID,
    class_id UUID,
    submission_count_snapshot INT NOT NULL DEFAULT 0,
    ai_summary TEXT NOT NULL,
    strengths TEXT NOT NULL,
    areas_for_improvement TEXT NOT NULL,
    recommendations TEXT NOT NULL,
    is_fallback BOOLEAN NOT NULL DEFAULT FALSE,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_report_target CHECK (
        (report_type = 'INDIVIDUAL_STUDENT' AND student_id IS NOT NULL AND class_id IS NULL) OR
        (report_type = 'CLASS_OVERALL' AND class_id IS NOT NULL AND student_id IS NULL)
    )
);

CREATE INDEX idx_ai_reports_student_id ON ai_reports(student_id);
CREATE INDEX idx_ai_reports_class_id ON ai_reports(class_id);
CREATE INDEX idx_ai_reports_generated_at ON ai_reports(generated_at);
