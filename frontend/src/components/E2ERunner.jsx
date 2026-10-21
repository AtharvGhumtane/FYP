import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { executeApiCall } from '../services/api';
import {
  Play,
  RotateCcw,
  CheckCircle2,
  XCircle,
  Loader2,
  ChevronDown,
  ChevronUp,
  FileCheck,
  Zap
} from 'lucide-react';

const INITIAL_STEPS = [
  { id: 'step-1', name: '1. Admin Authentication', service: 'auth-service', endpoint: 'POST /api/auth/login', status: 'idle' },
  { id: 'step-2', name: '2. Admin Creates Teacher Secret Key', service: 'auth-service', endpoint: 'POST /api/admin/teacher-keys', status: 'idle' },
  { id: 'step-3', name: '3. Teacher Registration with Key', service: 'auth-service', endpoint: 'POST /api/auth/register/teacher', status: 'idle' },
  { id: 'step-4', name: '4. Teacher Login', service: 'auth-service', endpoint: 'POST /api/auth/login', status: 'idle' },
  { id: 'step-5', name: '5. Student Registration', service: 'auth-service', endpoint: 'POST /api/auth/register/student', status: 'idle' },
  { id: 'step-6', name: '6. Student Login', service: 'auth-service', endpoint: 'POST /api/auth/login', status: 'idle' },
  { id: 'step-7', name: '7. Teacher Creates Classroom', service: 'classroom-service', endpoint: 'POST /api/classes', status: 'idle' },
  { id: 'step-8', name: '8. Student Requests to Join Class', service: 'classroom-service', endpoint: 'POST /api/classes/{id}/join', status: 'idle' },
  { id: 'step-9', name: '9. Teacher Approves Join Request', service: 'classroom-service', endpoint: 'POST /api/classes/{id}/requests/{studentId}', status: 'idle' },
  { id: 'step-10', name: '10. Teacher Creates Exam with MCQs', service: 'assessment-service', endpoint: 'POST /api/tests', status: 'idle' },
  { id: 'step-11', name: '11. Student Starts Test Session', service: 'assessment-service', endpoint: 'POST /api/tests/{testId}/start', status: 'idle' },
  { id: 'step-12', name: '12. Student Autosaves Answers', service: 'assessment-service', endpoint: 'PUT /api/tests/{testId}/answers', status: 'idle' },
  { id: 'step-13', name: '13. Student Submits Test', service: 'assessment-service', endpoint: 'POST /api/tests/{testId}/submit', status: 'idle' },
  { id: 'step-14', name: '14. Student Views Graded Scorecard', service: 'assessment-service', endpoint: 'GET /api/tests/{testId}/my-submission', status: 'idle' },
  { id: 'step-15', name: '15. Teacher Views Test Submissions', service: 'assessment-service', endpoint: 'GET /api/tests/{testId}/submissions', status: 'idle' },
  { id: 'step-16', name: '16. Student Checks Notifications', service: 'notification-service', endpoint: 'GET /api/notifications', status: 'idle' },
  { id: 'step-17', name: '17. Student DB Visual Analytics', service: 'analytics-service', endpoint: 'GET /api/analytics/student/{id}/db-metrics', status: 'idle' },
  { id: 'step-18', name: '18. Class Gemini AI Diagnostic Report', service: 'analytics-service', endpoint: 'GET /api/analytics/class/{id}/ai-report', status: 'idle' },
];

export default function E2ERunner() {
  const { baseUrl, addToast } = useApp();
  const [steps, setSteps] = useState(INITIAL_STEPS);
  const [isRunning, setIsRunning] = useState(false);
  const [expandedStep, setExpandedStep] = useState(null);
  const [logs, setLogs] = useState({});

  const updateStepStatus = (id, status, error = null, responseData = null, durationMs = null) => {
    setSteps(prev => prev.map(s => s.id === id ? { ...s, status, error, durationMs } : s));
    if (responseData || error) {
      setLogs(prev => ({ ...prev, [id]: { responseData, error, durationMs } }));
    }
  };

  const runAllTests = async () => {
    setIsRunning(true);
    setLogs({});
    setSteps(INITIAL_STEPS.map(s => ({ ...s, status: 'idle', error: null, durationMs: null })));

    const randomSuffix = Math.floor(1000 + Math.random() * 9000);
    const teacherEmail = `prof.e2e.${randomSuffix}@university.edu`;
    const studentEmail = `student.e2e.${randomSuffix}@university.edu`;
    const testSecretKey = `TEACHER-KEY-${randomSuffix}`;

    let adminToken = '';
    let teacherToken = '';
    let studentToken = '';
    let teacherId = '';
    let studentId = '';
    let classId = '';
    let testId = '';
    let q1Id = '';
    let q2Id = '';

    try {
      // 1. Admin login
      updateStepStatus('step-1', 'running');
      const step1 = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: '/api/auth/login',
        body: { email: 'atharvghumtane02@gmail.com', password: 'AdminPass@2026' }
      });
      if (!step1.ok || !step1.data?.token) throw new Error(step1.data?.message || 'Admin login failed');
      adminToken = step1.data.token;
      updateStepStatus('step-1', 'success', null, step1.data, step1.durationMs);

      // 2. Admin creates key
      updateStepStatus('step-2', 'running');
      const step2 = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: '/api/admin/teacher-keys',
        token: adminToken,
        body: { teacherEmail, secretKey: testSecretKey }
      });
      if (!step2.ok) throw new Error(step2.data?.message || 'Create teacher key failed');
      const actualSecretKey = step2.data.rawSecretKey || testSecretKey;
      updateStepStatus('step-2', 'success', null, step2.data, step2.durationMs);

      // 3. Register teacher
      updateStepStatus('step-3', 'running');
      const step3 = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: '/api/auth/register/teacher',
        body: {
          fullName: `Dr. E2E Smith ${randomSuffix}`,
          email: teacherEmail,
          password: 'Password123!',
          secretKey: actualSecretKey
        }
      });
      if (!step3.ok) throw new Error(step3.data?.message || 'Register teacher failed');
      updateStepStatus('step-3', 'success', null, step3.data, step3.durationMs);

      // 4. Login teacher
      updateStepStatus('step-4', 'running');
      const step4 = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: '/api/auth/login',
        body: { email: teacherEmail, password: 'Password123!' }
      });
      if (!step4.ok || !step4.data?.token) throw new Error(step4.data?.message || 'Teacher login failed');
      teacherToken = step4.data.token;
      teacherId = step4.data.userId;
      updateStepStatus('step-4', 'success', null, step4.data, step4.durationMs);

      // 5. Register student
      updateStepStatus('step-5', 'running');
      const step5 = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: '/api/auth/register/student',
        body: {
          fullName: `Student E2E ${randomSuffix}`,
          email: studentEmail,
          password: 'Password123!'
        }
      });
      if (!step5.ok) throw new Error(step5.data?.message || 'Register student failed');
      studentId = step5.data.userId;
      updateStepStatus('step-5', 'success', null, step5.data, step5.durationMs);

      // 6. Login student
      updateStepStatus('step-6', 'running');
      const step6 = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: '/api/auth/login',
        body: { email: studentEmail, password: 'Password123!' }
      });
      if (!step6.ok || !step6.data?.token) throw new Error(step6.data?.message || 'Student login failed');
      studentToken = step6.data.token;
      updateStepStatus('step-6', 'success', null, step6.data, step6.durationMs);

      // 7. Teacher creates class
      updateStepStatus('step-7', 'running');
      const step7 = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: '/api/classes',
        token: teacherToken,
        body: {
          name: `Distributed Algorithms ${randomSuffix}`,
          subject: 'Computer Science',
          description: 'E2E automated test class for validation'
        }
      });
      if (!step7.ok || !step7.data?.id) throw new Error(step7.data?.message || 'Create class failed');
      classId = step7.data.id;
      updateStepStatus('step-7', 'success', null, step7.data, step7.durationMs);

      // 8. Student joins class
      updateStepStatus('step-8', 'running');
      const step8 = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: `/api/classes/${classId}/join`,
        token: studentToken,
      });
      if (!step8.ok) throw new Error(step8.data?.message || 'Join class failed');
      updateStepStatus('step-8', 'success', null, step8.data, step8.durationMs);

      // 9. Teacher approves request
      updateStepStatus('step-9', 'running');
      const step9 = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: `/api/classes/${classId}/requests/${studentId}`,
        token: teacherToken,
        body: { status: 'ACCEPTED' }
      });
      if (!step9.ok) throw new Error(step9.data?.message || 'Approve student failed');
      updateStepStatus('step-9', 'success', null, step9.data, step9.durationMs);

      // 10. Teacher creates test
      updateStepStatus('step-10', 'running');
      const step10 = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: '/api/tests',
        token: teacherToken,
        body: {
          classId,
          title: `Graph Algorithms Exam ${randomSuffix}`,
          description: 'E2E Assessment Test',
          startTime: new Date(Date.now() - 3600000).toISOString(),
          endTime: new Date(Date.now() + 86400000 * 7).toISOString(),
          durationMinutes: 45,
          questions: [
            {
              topic: 'Shortest Path',
              questionText: 'What is the time complexity of Dijkstra with min-heap?',
              optionA: 'O(V^2)',
              optionB: 'O((V + E) log V)',
              optionC: 'O(E log V)',
              optionD: 'O(V log E)',
              correctOption: 'B',
              marks: 5,
              explanation: 'Standard Dijkstra complexity using binary min-heap is O((V+E)logV).'
            },
            {
              topic: 'Cycle Detection',
              questionText: 'Which structure is used in Kruskal algorithm to detect cycles?',
              optionA: 'Disjoint Set Union (DSU)',
              optionB: 'Stack',
              optionC: 'Queue',
              optionD: 'Fenwick Tree',
              correctOption: 'A',
              marks: 5,
              explanation: 'DSU efficiently detects cycles in O(alpha(V)).'
            }
          ]
        }
      });
      if (!step10.ok || !step10.data?.id) throw new Error(step10.data?.message || 'Create test failed');
      testId = step10.data.id;
      q1Id = step10.data.questions?.[0]?.id || '';
      q2Id = step10.data.questions?.[1]?.id || '';
      updateStepStatus('step-10', 'success', null, step10.data, step10.durationMs);

      // 11. Student starts test
      updateStepStatus('step-11', 'running');
      const step11 = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: `/api/tests/${testId}/start`,
        token: studentToken,
      });
      if (!step11.ok) throw new Error(step11.data?.message || 'Start test failed');
      updateStepStatus('step-11', 'success', null, step11.data, step11.durationMs);

      // 12. Student autosaves answers
      updateStepStatus('step-12', 'running');
      const step12 = await executeApiCall({
        baseUrl,
        method: 'PUT',
        path: `/api/tests/${testId}/answers`,
        token: studentToken,
        body: {
          answers: [{ questionId: q1Id, selectedOption: 'B' }]
        }
      });
      if (!step12.ok) throw new Error(step12.data?.message || 'Autosave answers failed');
      updateStepStatus('step-12', 'success', null, step12.data, step12.durationMs);

      // 13. Student submits test
      updateStepStatus('step-13', 'running');
      const step13 = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: `/api/tests/${testId}/submit`,
        token: studentToken,
        body: {
          answers: [
            { questionId: q1Id, selectedOption: 'B' },
            { questionId: q2Id, selectedOption: 'A' }
          ],
          tabSwitchCount: 0
        }
      });
      if (!step13.ok) throw new Error(step13.data?.message || 'Submit test failed');
      updateStepStatus('step-13', 'success', null, step13.data, step13.durationMs);

      // 14. Student views scorecard
      updateStepStatus('step-14', 'running');
      const step14 = await executeApiCall({
        baseUrl,
        method: 'GET',
        path: `/api/tests/${testId}/my-submission`,
        token: studentToken,
      });
      if (!step14.ok) throw new Error(step14.data?.message || 'Get scorecard failed');
      updateStepStatus('step-14', 'success', null, step14.data, step14.durationMs);

      // 15. Teacher views submissions
      updateStepStatus('step-15', 'running');
      const step15 = await executeApiCall({
        baseUrl,
        method: 'GET',
        path: `/api/tests/${testId}/submissions`,
        token: teacherToken,
      });
      if (!step15.ok) throw new Error(step15.data?.message || 'Get test submissions failed');
      updateStepStatus('step-15', 'success', null, step15.data, step15.durationMs);

      // 16. Student notifications
      updateStepStatus('step-16', 'running');
      const step16 = await executeApiCall({
        baseUrl,
        method: 'GET',
        path: '/api/notifications',
        token: studentToken,
      });
      if (!step16.ok) throw new Error(step16.data?.message || 'Get notifications failed');
      updateStepStatus('step-16', 'success', null, step16.data, step16.durationMs);

      // 17. Student DB Visual Analytics
      updateStepStatus('step-17', 'running');
      const step17 = await executeApiCall({
        baseUrl,
        method: 'GET',
        path: `/api/analytics/student/${studentId}/db-metrics`,
        token: studentToken,
      });
      if (!step17.ok) throw new Error(step17.data?.message || 'Get student analytics failed');
      updateStepStatus('step-17', 'success', null, step17.data, step17.durationMs);

      // 18. Class Gemini AI Diagnostic Report
      updateStepStatus('step-18', 'running');
      const step18 = await executeApiCall({
        baseUrl,
        method: 'GET',
        path: `/api/analytics/class/${classId}/ai-report`,
        token: teacherToken,
      });
      if (!step18.ok) throw new Error(step18.data?.message || 'Get class AI report failed');
      updateStepStatus('step-18', 'success', null, step18.data, step18.durationMs);

      addToast('success', 'Full End-to-End Suite Completed Successfully! All services verified.');
    } catch (err) {
      addToast('error', `E2E suite encountered an issue: ${err.message}`);
    } finally {
      setIsRunning(false);
    }
  };

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '32px 24px' }}>
      {/* Header */}
      <div className="glass-panel glow-card" style={{ padding: '28px', marginBottom: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
              <Zap size={24} color="#ec4899" />
              <h2 style={{ fontSize: '1.5rem', margin: 0 }}>Automated End-to-End Multi-Service Test Suite</h2>
            </div>
            <p style={{ color: '#94a3b8', fontSize: '0.9rem', margin: 0, maxWidth: '720px' }}>
              Executes the complete cross-service workflow in sequence: Admin Key generation &rarr; Teacher registration &rarr; Student enrollment &rarr; Classroom management &rarr; Exam creation &rarr; Autosave & Submit &rarr; Graded Scorecard &rarr; Notifications &rarr; Gemini AI diagnostics.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              onClick={runAllTests}
              disabled={isRunning}
              className="btn btn-primary btn-lg"
              style={{ minWidth: '180px' }}
            >
              {isRunning ? (
                <>
                  <Loader2 size={18} className="animate-spin" />
                  <span>Executing Pipeline...</span>
                </>
              ) : (
                <>
                  <Play size={18} />
                  <span>Run Full E2E Flow</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Steps List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {steps.map((step, idx) => {
          const isExpanded = expandedStep === step.id;
          const logData = logs[step.id];

          return (
            <div
              key={step.id}
              className="glass-panel"
              style={{
                borderLeft: `4px solid ${
                  step.status === 'success' ? '#10b981' :
                  step.status === 'running' ? '#6366f1' :
                  step.status === 'error' ? '#ef4444' : 'rgba(255, 255, 255, 0.1)'
                }`,
                overflow: 'hidden',
              }}
            >
              <div
                onClick={() => setExpandedStep(isExpanded ? null : step.id)}
                style={{
                  padding: '16px 20px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  cursor: 'pointer',
                  userSelect: 'none',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                  {step.status === 'running' && <Loader2 size={20} color="#6366f1" className="animate-spin" />}
                  {step.status === 'success' && <CheckCircle2 size={20} color="#10b981" />}
                  {step.status === 'error' && <XCircle size={20} color="#ef4444" />}
                  {step.status === 'idle' && (
                    <span style={{
                      width: '20px',
                      height: '20px',
                      borderRadius: '50%',
                      background: 'rgba(255, 255, 255, 0.08)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '0.75rem',
                      color: '#94a3b8'
                    }}>
                      {idx + 1}
                    </span>
                  )}

                  <div>
                    <span style={{ fontSize: '0.95rem', fontWeight: 600, color: '#f8fafc' }}>
                      {step.name}
                    </span>
                    <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginTop: '2px' }}>
                      <span style={{ fontSize: '0.72rem', color: '#818cf8', fontFamily: 'var(--font-mono)' }}>
                        {step.service}
                      </span>
                      <span style={{ color: '#475569', fontSize: '0.7rem' }}>&bull;</span>
                      <span style={{ fontSize: '0.72rem', color: '#94a3b8', fontFamily: 'var(--font-mono)' }}>
                        {step.endpoint}
                      </span>
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  {step.durationMs !== null && (
                    <span style={{ fontSize: '0.75rem', color: '#94a3b8', fontFamily: 'var(--font-mono)' }}>
                      {step.durationMs}ms
                    </span>
                  )}
                  {isExpanded ? <ChevronUp size={18} color="#94a3b8" /> : <ChevronDown size={18} color="#94a3b8" />}
                </div>
              </div>

              {/* Collapsible log view */}
              {isExpanded && logData && (
                <div style={{
                  padding: '16px 20px',
                  background: 'rgba(15, 23, 42, 0.95)',
                  borderTop: '1px solid rgba(255, 255, 255, 0.06)',
                  fontSize: '0.82rem',
                }}>
                  {logData.error && (
                    <div style={{ color: '#f87171', marginBottom: '8px', fontWeight: 600 }}>
                      Error: {logData.error}
                    </div>
                  )}
                  <div style={{ color: '#94a3b8', marginBottom: '6px', fontWeight: 600 }}>Response Payload:</div>
                  <pre style={{
                    margin: 0,
                    padding: '10px',
                    borderRadius: '6px',
                    background: 'rgba(0,0,0,0.4)',
                    color: '#e2e8f0',
                    fontFamily: 'var(--font-mono)',
                    fontSize: '0.78rem',
                    overflowX: 'auto',
                    maxHeight: '220px',
                  }}>
                    {JSON.stringify(logData.responseData, null, 2)}
                  </pre>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
