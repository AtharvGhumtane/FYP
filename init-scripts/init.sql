-- Initialize PostgreSQL databases for all microservices
CREATE DATABASE auth_db;
CREATE DATABASE classroom_db;
CREATE DATABASE assessment_db;
CREATE DATABASE notification_db;
CREATE DATABASE analytics_db;

-- Grant all privileges to the default user
GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE classroom_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE assessment_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE analytics_db TO postgres;
