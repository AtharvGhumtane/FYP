// Registry of all endpoints in the FYP Platform

export const ENDPOINT_GROUPS = [
  {
    group: 'Authentication & Identity',
    service: 'auth-service (:8081 via Gateway :8080)',
    description: 'User registration, login, profile inspection, and Google token exchange',
    endpoints: [
      {
        id: 'auth-login-admin',
        name: 'Login (Admin Default)',
        method: 'POST',
        path: '/api/auth/login',
        requiresAuth: false,
        description: 'Authenticates administrator with seeded credentials',
        sampleBody: {
          email: 'atharvghumtane02@gmail.com',
          password: 'AdminPass@2026'
        }
      },
      {
        id: 'auth-login-teacher',
        name: 'Login (Teacher)',
        method: 'POST',
        path: '/api/auth/login',
        requiresAuth: false,
        description: 'Authenticates registered teacher',
        sampleBody: {
          email: 'dr.smith@university.edu',
          password: 'Password123!'
        }
      },
      {
        id: 'auth-login-student',
        name: 'Login (Student)',
        method: 'POST',
        path: '/api/auth/login',
        requiresAuth: false,
        description: 'Authenticates registered student',
        sampleBody: {
          email: 'grace@university.edu',
          password: 'Password123!'
        }
      },
      {
        id: 'auth-register-student',
        name: 'Register Student',
        method: 'POST',
        path: '/api/auth/register/student',
        requiresAuth: false,
        description: 'Registers a new student account (server assigns ROLE_STUDENT)',
        sampleBody: {
          fullName: 'Grace Hopper',
          email: 'grace@university.edu',
          password: 'Password123!'
        }
      },
      {
        id: 'auth-register-teacher',
        name: 'Register Teacher',
        method: 'POST',
        path: '/api/auth/register/teacher',
        requiresAuth: false,
        description: 'Registers a teacher account using a pre-generated secret key',
        sampleBody: {
          fullName: 'Dr. John Smith',
          email: 'dr.smith@university.edu',
          password: 'Password123!',
          secretKey: 'TEACHER-SECRET-KEY-2026'
        }
      },
      {
        id: 'auth-google',
        name: 'Google OAuth Exchange',
        method: 'POST',
        path: '/api/auth/google',
        requiresAuth: false,
        description: 'Exchanges Google OAuth2 credential idToken for JWT',
        sampleBody: {
          idToken: 'mock-google-id-token-abc-123',
          role: 'ROLE_STUDENT',
          teacherSecretKey: ''
        }
      },
      {
        id: 'auth-me',
        name: 'Get Current Profile (Me)',
        method: 'GET',
        path: '/api/auth/me',
        requiresAuth: true,
        description: 'Returns profile details for the authenticated user from JWT',
        sampleBody: null
      }
    ]
  },
  {
    group: 'Admin Control Center',
    service: 'auth-service (:8081 via Gateway :8080)',
    description: 'Generate and manage authorized teacher secret keys',
    endpoints: [
      {
        id: 'admin-create-key',
        name: 'Generate Teacher Key',
        method: 'POST',
        path: '/api/admin/teacher-keys',
        requiresAuth: true,
        allowedRoles: ['ROLE_ADMIN'],
        description: 'Generates a new BCrypt-hashed secret key for teacher registration (plaintext returned once)',
        sampleBody: {
          teacherEmail: 'dr.smith@university.edu',
          secretKey: 'TEACHER-SECRET-KEY-2026'
        }
      },
      {
        id: 'admin-list-keys',
        name: 'List All Teacher Keys',
        method: 'GET',
        path: '/api/admin/teacher-keys',
        requiresAuth: true,
        allowedRoles: ['ROLE_ADMIN'],
        description: 'Lists all registered teacher keys and their active status',
        sampleBody: null
      },
      {
        id: 'admin-toggle-key',
        name: 'Toggle Key Status (Active / Revoked)',
        method: 'PATCH',
        path: '/api/admin/teacher-keys/{id}/status',
        params: ['id'],
        requiresAuth: true,
        allowedRoles: ['ROLE_ADMIN'],
        description: 'Enables or revokes a teacher registration key',
        sampleBody: {
          isActive: true
        }
      }
    ]
  },
  {
    group: 'Classroom Management',
    service: 'classroom-service (:8082 via Gateway :8080)',
    description: 'Class creation, student enrollment requests, approvals, and roster lookup',
    endpoints: [
      {
        id: 'class-create',
        name: 'Create Classroom',
        method: 'POST',
        path: '/api/classes',
        requiresAuth: true,
        allowedRoles: ['ROLE_TEACHER'],
        description: 'Creates a new class and generates a unique join code',
        sampleBody: {
          name: 'Advanced Data Structures & Algorithms',
          subject: 'Computer Science',
          description: 'Deep dive into trees, graphs, and dynamic programming'
        }
      },
      {
        id: 'class-list',
        name: 'List Classes',
        method: 'GET',
        path: '/api/classes',
        requiresAuth: true,
        description: 'Lists classes relevant to the authenticated user (taught for teachers, enrolled for students)',
        sampleBody: null
      },
      {
        id: 'class-get-by-id',
        name: 'Get Class by ID',
        method: 'GET',
        path: '/api/classes/{id}',
        params: ['id'],
        requiresAuth: true,
        description: 'Retrieves class details for teacher owner or enrolled student',
        sampleBody: null
      },
      {
        id: 'class-request-join',
        name: 'Request Join Class (by Class ID)',
        method: 'POST',
        path: '/api/classes/{id}/join',
        params: ['id'],
        requiresAuth: true,
        allowedRoles: ['ROLE_STUDENT'],
        description: 'Submits a student request to join a class (creates PENDING enrollment)',
        sampleBody: null
      },
      {
        id: 'class-get-requests',
        name: 'List Pending Join Requests',
        method: 'GET',
        path: '/api/classes/{id}/requests',
        params: ['id'],
        requiresAuth: true,
        allowedRoles: ['ROLE_TEACHER'],
        description: 'Teacher reviews pending student enrollment requests',
        sampleBody: null
      },
      {
        id: 'class-decide-request',
        name: 'Approve or Reject Student Request',
        method: 'POST',
        path: '/api/classes/{id}/requests/{studentId}',
        params: ['id', 'studentId'],
        requiresAuth: true,
        allowedRoles: ['ROLE_TEACHER'],
        description: 'Approves (ACCEPTED) or rejects (REJECTED) student enrollment',
        sampleBody: {
          status: 'ACCEPTED'
        }
      },
      {
        id: 'class-get-roster',
        name: 'Get Class Roster',
        method: 'GET',
        path: '/api/classes/{id}/roster',
        params: ['id'],
        requiresAuth: true,
        description: 'Fetches list of all enrolled students in the class',
        sampleBody: null
      }
    ]
  },
  {
    group: 'Assessment & Examination',
    service: 'assessment-service (:8083 via Gateway :8080)',
    description: 'Test creation, MCQ question builder, proctoring autosave, and grading scorecard',
    endpoints: [
      {
        id: 'test-create',
        name: 'Create Assessment Test with MCQs',
        method: 'POST',
        path: '/api/tests',
        requiresAuth: true,
        allowedRoles: ['ROLE_TEACHER'],
        description: 'Creates test with schedule, duration, and MCQ questions',
        sampleBody: {
          classId: '00000000-0000-0000-0000-000000000000',
          title: 'Exam 1: Graph Algorithms & Dynamic Programming',
          description: 'Shortest path, spanning trees, and memoization techniques',
          startTime: '2026-10-01T00:00:00Z',
          endTime: '2026-11-30T23:59:59Z',
          durationMinutes: 45,
          questions: [
            {
              topic: 'Shortest Path',
              questionText: 'What is the time complexity of Dijkstra with a binary min-heap?',
              optionA: 'O(V^2)',
              optionB: 'O((V + E) log V)',
              optionC: 'O(E log V)',
              optionD: 'O(V log E)',
              correctOption: 'B',
              marks: 5,
              explanation: 'Extracting min V times and updating E edge weights yields O((V + E) log V).'
            },
            {
              topic: 'Spanning Trees',
              questionText: 'Which data structure is primarily used by Kruskal\'s algorithm to detect cycles?',
              optionA: 'Disjoint Set Union (DSU)',
              optionB: 'Call Stack',
              optionC: 'Double-ended Queue',
              optionD: 'Segment Tree',
              correctOption: 'A',
              marks: 5,
              explanation: 'DSU / Union-Find maintains connected components in nearly O(1) amortized time.'
            }
          ]
        }
      },
      {
        id: 'test-list-class',
        name: 'List Tests for Class',
        method: 'GET',
        path: '/api/tests/class/{classId}',
        params: ['classId'],
        requiresAuth: true,
        description: 'Lists all scheduled/active assessments for a given class',
        sampleBody: null
      },
      {
        id: 'test-get-by-id',
        name: 'Get Test Details by ID',
        method: 'GET',
        path: '/api/tests/{testId}',
        params: ['testId'],
        requiresAuth: true,
        description: 'Fetches test details (answers hidden for students before submission)',
        sampleBody: null
      },
      {
        id: 'test-start',
        name: 'Start Test Session',
        method: 'POST',
        path: '/api/tests/{testId}/start',
        params: ['testId'],
        requiresAuth: true,
        allowedRoles: ['ROLE_STUDENT'],
        description: 'Initializes student attempt, returns deadline and questions without answer keys',
        sampleBody: null
      },
      {
        id: 'test-autosave-answers',
        name: 'Autosave Answers During Test',
        method: 'PUT',
        path: '/api/tests/{testId}/answers',
        params: ['testId'],
        requiresAuth: true,
        allowedRoles: ['ROLE_STUDENT'],
        description: 'Saves candidate answer selections in real-time as they click options',
        sampleBody: {
          answers: [
            {
              questionId: '00000000-0000-0000-0000-000000000000',
              selectedOption: 'B'
            }
          ]
        }
      },
      {
        id: 'test-submit',
        name: 'Submit Test for Instant Grading',
        method: 'POST',
        path: '/api/tests/{testId}/submit',
        params: ['testId'],
        requiresAuth: true,
        allowedRoles: ['ROLE_STUDENT'],
        description: 'Finalizes test submission, records tabSwitchCount proctoring events, and computes scorecard',
        sampleBody: {
          answers: [
            {
              questionId: '00000000-0000-0000-0000-000000000000',
              selectedOption: 'B'
            }
          ],
          tabSwitchCount: 0
        }
      },
      {
        id: 'test-my-submission',
        name: 'Get My Graded Scorecard',
        method: 'GET',
        path: '/api/tests/{testId}/my-submission',
        params: ['testId'],
        requiresAuth: true,
        allowedRoles: ['ROLE_STUDENT'],
        description: 'Returns student scorecard with score, percentage, question breakdown, and explanations',
        sampleBody: null
      },
      {
        id: 'test-list-submissions',
        name: 'List All Class Submissions',
        method: 'GET',
        path: '/api/tests/{testId}/submissions',
        params: ['testId'],
        requiresAuth: true,
        allowedRoles: ['ROLE_TEACHER'],
        description: 'Teacher reviews all graded student submissions and proctoring logs for a test',
        sampleBody: null
      }
    ]
  },
  {
    group: 'Notifications',
    service: 'notification-service (:8084 via Gateway :8080)',
    description: 'In-app alert polling, unread counts, and reading flags',
    endpoints: [
      {
        id: 'notification-list-all',
        name: 'Poll All Notifications',
        method: 'GET',
        path: '/api/notifications',
        requiresAuth: true,
        description: 'Polls notification feed for current authenticated user',
        sampleBody: null
      },
      {
        id: 'notification-list-unread',
        name: 'Poll Unread Notifications',
        method: 'GET',
        path: '/api/notifications?unreadOnly=true',
        requiresAuth: true,
        description: 'Fetches only unread alerts',
        sampleBody: null
      },
      {
        id: 'notification-mark-single-read',
        name: 'Mark Single Notification Read',
        method: 'PATCH',
        path: '/api/notifications/{id}/read',
        params: ['id'],
        requiresAuth: true,
        description: 'Marks specific notification as read by ID',
        sampleBody: null
      },
      {
        id: 'notification-mark-all-read',
        name: 'Mark All Notifications Read',
        method: 'POST',
        path: '/api/notifications/mark-all-read',
        requiresAuth: true,
        description: 'Marks all notifications for current user as read',
        sampleBody: null
      }
    ]
  },
  {
    group: 'Analytics & Google Gemini AI Diagnosis',
    service: 'analytics-service (:8085 via Gateway :8080)',
    description: 'DB-calculated performance metrics and Google Gemini AI diagnostic reports',
    endpoints: [
      {
        id: 'analytics-student-metrics',
        name: 'Get Student DB Performance Metrics',
        method: 'GET',
        path: '/api/analytics/student/{studentId}/db-metrics',
        params: ['studentId'],
        requiresAuth: true,
        description: 'Database metrics: tests taken, score progression, topic accuracy',
        sampleBody: null
      },
      {
        id: 'analytics-class-metrics',
        name: 'Get Class DB Visual Metrics',
        method: 'GET',
        path: '/api/analytics/class/{classId}/db-metrics',
        params: ['classId'],
        requiresAuth: true,
        allowedRoles: ['ROLE_TEACHER', 'ROLE_ADMIN'],
        description: 'Class-wide performance distribution, cohort average, and topic performance',
        sampleBody: null
      },
      {
        id: 'analytics-student-ai-report',
        name: 'Generate / Fetch Student Gemini AI Report',
        method: 'GET',
        path: '/api/analytics/student/{studentId}/ai-report',
        params: ['studentId'],
        requiresAuth: true,
        description: 'Personalized AI learning diagnosis evaluating strengths, pitfalls, and study advice',
        sampleBody: null
      },
      {
        id: 'analytics-class-ai-report',
        name: 'Generate / Fetch Class Gemini AI Report',
        method: 'GET',
        path: '/api/analytics/class/{classId}/ai-report',
        params: ['classId'],
        requiresAuth: true,
        allowedRoles: ['ROLE_TEACHER', 'ROLE_ADMIN'],
        description: 'Aggregate AI diagnosis delivering curriculum feedback and teaching strategies',
        sampleBody: null
      }
    ]
  },
  {
    group: 'Gateway & Health',
    service: 'api-gateway (:8080)',
    description: 'Spring Cloud Gateway status and actuator indicators',
    endpoints: [
      {
        id: 'actuator-health',
        name: 'Gateway Health Check',
        method: 'GET',
        path: '/actuator/health',
        requiresAuth: false,
        description: 'Reports overall API Gateway status and routing uptime',
        sampleBody: null
      }
    ]
  }
];
