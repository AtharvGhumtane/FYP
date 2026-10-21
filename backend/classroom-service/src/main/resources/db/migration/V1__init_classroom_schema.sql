-- V1__init_classroom_schema.sql
-- Create Classes Table
CREATE TABLE IF NOT EXISTS classes (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    class_code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    teacher_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Create Enrollments Table
CREATE TABLE IF NOT EXISTS enrollments (
    id UUID PRIMARY KEY,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    student_name VARCHAR(255),
    student_email VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    responded_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_class_student UNIQUE (class_id, student_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_classes_teacher ON classes(teacher_id);
CREATE INDEX IF NOT EXISTS idx_classes_code ON classes(class_code);
CREATE INDEX IF NOT EXISTS idx_enrollments_student ON enrollments(student_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_class_status ON enrollments(class_id, status);
