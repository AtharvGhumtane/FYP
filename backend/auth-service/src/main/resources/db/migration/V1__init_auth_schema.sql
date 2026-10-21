-- V1__init_auth_schema.sql
-- Create Users Table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    auth_provider VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Create Teacher Key Registry Table
CREATE TABLE IF NOT EXISTS teacher_key_registry (
    id UUID PRIMARY KEY,
    teacher_email VARCHAR(255) UNIQUE NOT NULL,
    secret_key_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_admin VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Create Teacher Key Audit Log Table (for Rate Limiting & Lockout tracking)
CREATE TABLE IF NOT EXISTS teacher_key_audit_log (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(100) NOT NULL,
    is_success BOOLEAN NOT NULL,
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_teacher_key_email ON teacher_key_registry(teacher_email);
CREATE INDEX IF NOT EXISTS idx_audit_email_time ON teacher_key_audit_log(email, attempted_at);
CREATE INDEX IF NOT EXISTS idx_audit_ip_time ON teacher_key_audit_log(ip_address, attempted_at);
