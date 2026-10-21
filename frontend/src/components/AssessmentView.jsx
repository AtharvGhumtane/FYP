import React, { useState, useEffect } from 'react';
import { useApp } from '../context/AppContext';
import { executeApiCall } from '../services/api';
import {
  FileText,
  Plus,
  Clock,
  CheckCircle,
  AlertCircle,
  Play,
  RotateCcw,
  Send,
  Eye,
  Award,
  ShieldAlert,
  ChevronRight,
  HelpCircle,
  BookOpen
} from 'lucide-react';

export default function AssessmentView({ preselectedClass }) {
  const { baseUrl, currentUser, addToast } = useApp();

  const [classes, setClasses] = useState([]);
  const [selectedClassId, setSelectedClassId] = useState(preselectedClass?.id || '');
  const [tests, setTests] = useState([]);
  const [loading, setLoading] = useState(false);

  // Active Test Session (for student taking test)
  const [activeSession, setActiveSession] = useState(null); // { testId, testTitle, questions, durationMinutes, deadline }
  const [answers, setAnswers] = useState({}); // { [questionId]: 'A' | 'B' | 'C' | 'D' }
  const [currentQIndex, setCurrentQIndex] = useState(0);
  const [tabSwitchCount, setTabSwitchCount] = useState(0);
  const [autosaving, setAutosaving] = useState(false);
  const [timeLeftSec, setTimeLeftSec] = useState(0);

  // Scorecard inspection
  const [scorecard, setScorecard] = useState(null);
  const [submissionsList, setSubmissionsList] = useState(null);
  const [selectedTestForSubs, setSelectedTestForSubs] = useState(null);

  // Create test modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [testTitle, setTestTitle] = useState('');
  const [testDesc, setTestDesc] = useState('');
  const [durationMinutes, setDurationMinutes] = useState(30);
  const [questions, setQuestions] = useState([
    {
      topic: 'Algorithms',
      questionText: 'What is the average time complexity of QuickSort?',
      optionA: 'O(n)',
      optionB: 'O(n log n)',
      optionC: 'O(n^2)',
      optionD: 'O(log n)',
      correctOption: 'B',
      marks: 5,
      explanation: 'QuickSort divides problem size roughly by half on average, leading to O(n log n).'
    }
  ]);
  const [creating, setCreating] = useState(false);

  const isTeacher = currentUser?.role === 'ROLE_TEACHER';
  const isStudent = currentUser?.role === 'ROLE_STUDENT';

  // Load available classes
  useEffect(() => {
    fetchClasses();
  }, [currentUser]);

  // Load tests when class selected
  useEffect(() => {
    if (selectedClassId) {
      fetchTests(selectedClassId);
    }
  }, [selectedClassId]);

  // Proctoring tab-switch tracker during active test
  useEffect(() => {
    if (!activeSession) return;

    const handleVisibility = () => {
      if (document.hidden) {
        setTabSwitchCount(prev => {
          const next = prev + 1;
          addToast('error', `Proctoring Warning: Tab switch #${next} detected and logged!`);
          return next;
        });
      }
    };

    document.addEventListener('visibilitychange', handleVisibility);
    return () => document.removeEventListener('visibilitychange', handleVisibility);
  }, [activeSession]);

  // Countdown timer during test
  useEffect(() => {
    if (!activeSession || timeLeftSec <= 0) return;

    const interval = setInterval(() => {
      setTimeLeftSec(prev => {
        if (prev <= 1) {
          clearInterval(interval);
          handleAutoSubmit();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [activeSession, timeLeftSec]);

  const fetchClasses = async () => {
    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'GET',
        path: '/api/classes',
        token: currentUser?.token,
      });

      if (res.ok && Array.isArray(res.data)) {
        setClasses(res.data);
        if (!selectedClassId && res.data.length > 0) {
          setSelectedClassId(res.data[0].id);
        }
      }
    } catch (err) {
      console.error(err);
    }
  };

  const fetchTests = async (classId) => {
    setLoading(true);
    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'GET',
        path: `/api/tests/class/${classId}`,
        token: currentUser?.token,
      });

      if (res.ok && Array.isArray(res.data)) {
        setTests(res.data);
      } else {
        setTests([]);
      }
    } catch (err) {
      addToast('error', `Error loading tests: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleStartTest = async (test) => {
    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: `/api/tests/${test.id}/start`,
        token: currentUser?.token,
      });

      if (res.ok && res.data) {
        setActiveSession({
          testId: test.id,
          testTitle: test.title,
          questions: res.data.questions || test.questions || [],
          durationMinutes: test.durationMinutes,
        });
        setTimeLeftSec((test.durationMinutes || 30) * 60);
        setAnswers({});
        setCurrentQIndex(0);
        setTabSwitchCount(0);
        addToast('success', `Test "${test.title}" started! Autosave is active.`);
      } else {
        addToast('error', `Cannot start test: ${res.data?.message || res.error}`);
      }
    } catch (err) {
      addToast('error', `Error: ${err.message}`);
    }
  };

  const handleSelectOption = async (questionId, option) => {
    const updated = { ...answers, [questionId]: option };
    setAnswers(updated);

    // Autosave immediately
    setAutosaving(true);
    try {
      await executeApiCall({
        baseUrl,
        method: 'PUT',
        path: `/api/tests/${activeSession.testId}/answers`,
        token: currentUser?.token,
        body: {
          answers: [{ questionId, selectedOption: option }]
        }
      });
    } catch (err) {
      console.error('Autosave error', err);
    } finally {
      setTimeout(() => setAutosaving(false), 500);
    }
  };

  const handleSubmitTest = async () => {
    if (!activeSession) return;
    const answeredCount = Object.keys(answers).length;
    const totalQ = activeSession.questions.length;

    if (answeredCount < totalQ) {
      if (!confirm(`You answered ${answeredCount} of ${totalQ} questions. Are you sure you want to submit?`)) {
        return;
      }
    }

    try {
      const answersPayload = Object.entries(answers).map(([qId, opt]) => ({
        questionId: qId,
        selectedOption: opt
      }));

      const res = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: `/api/tests/${activeSession.testId}/submit`,
        token: currentUser?.token,
        body: {
          answers: answersPayload,
          tabSwitchCount
        }
      });

      if (res.ok && res.data) {
        addToast('success', `Test graded! You scored ${res.data.totalMarksAwarded} / ${res.data.maximumMarks}`);
        setActiveSession(null);
        setScorecard(res.data);
      } else {
        addToast('error', `Submission failed: ${res.data?.message || res.error}`);
      }
    } catch (err) {
      addToast('error', `Submission error: ${err.message}`);
    }
  };

  const handleAutoSubmit = async () => {
    addToast('warning', 'Time expired! Auto-submitting assessment...');
    handleSubmitTest();
  };

  const handleViewMyScorecard = async (testId) => {
    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'GET',
        path: `/api/tests/${testId}/my-submission`,
        token: currentUser?.token,
      });

      if (res.ok && res.data) {
        setScorecard(res.data);
      } else {
        addToast('error', `Could not fetch scorecard: ${res.data?.message || res.error}`);
      }
    } catch (err) {
      addToast('error', `Error: ${err.message}`);
    }
  };

  const handleViewSubmissions = async (test) => {
    setSelectedTestForSubs(test);
    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'GET',
        path: `/api/tests/${test.id}/submissions`,
        token: currentUser?.token,
      });

      if (res.ok && Array.isArray(res.data)) {
        setSubmissionsList(res.data);
      } else {
        setSubmissionsList([]);
      }
    } catch (err) {
      addToast('error', `Error fetching submissions: ${err.message}`);
    }
  };

  const handleCreateTest = async (e) => {
    e.preventDefault();
    setCreating(true);

    try {
      const body = {
        classId: selectedClassId,
        title: testTitle,
        description: testDesc,
        startTime: new Date().toISOString(),
        endTime: new Date(Date.now() + 86400000 * 30).toISOString(),
        durationMinutes: parseInt(durationMinutes, 10),
        questions
      };

      const res = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: '/api/tests',
        token: currentUser?.token,
        body
      });

      if (res.ok && res.data) {
        addToast('success', `Test "${res.data.title}" published with ${questions.length} questions!`);
        setShowCreateModal(false);
        setTestTitle('');
        setTestDesc('');
        fetchTests(selectedClassId);
      } else {
        addToast('error', `Failed to create test: ${res.data?.message || res.error}`);
      }
    } catch (err) {
      addToast('error', `Error: ${err.message}`);
    } finally {
      setCreating(false);
    }
  };

  const addQuestionField = () => {
    setQuestions([
      ...questions,
      {
        topic: 'General Topic',
        questionText: 'Enter question text here...',
        optionA: 'Option 1',
        optionB: 'Option 2',
        optionC: 'Option 3',
        optionD: 'Option 4',
        correctOption: 'A',
        marks: 5,
        explanation: 'Detailed explanation for why this answer is correct.'
      }
    ]);
  };

  const formatTimer = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // If student is actively taking a test, show full screen examination cockpit!
  if (activeSession) {
    const currentQ = activeSession.questions[currentQIndex];
    return (
      <div style={{ maxWidth: '1000px', margin: '0 auto', padding: '32px 24px' }}>
        {/* Exam Cockpit Header */}
        <div className="glass-panel glow-card" style={{ padding: '20px 24px', marginBottom: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
            <div>
              <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#818cf8', textTransform: 'uppercase' }}>
                Active Examination Session
              </span>
              <h2 style={{ fontSize: '1.4rem', margin: '4px 0 0' }}>{activeSession.testTitle}</h2>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
              {/* Autosave status indicator */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8rem', color: autosaving ? '#f59e0b' : '#10b981' }}>
                <span style={{
                  width: '8px',
                  height: '8px',
                  borderRadius: '50%',
                  background: autosaving ? '#f59e0b' : '#10b981',
                  boxShadow: `0 0 8px ${autosaving ? '#f59e0b' : '#10b981'}`,
                }} />
                <span>{autosaving ? 'Autosaving answers...' : 'Answers synced'}</span>
              </div>

              {/* Tab switch counter */}
              {tabSwitchCount > 0 && (
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  background: 'rgba(239, 68, 68, 0.15)',
                  color: '#f87171',
                  padding: '4px 10px',
                  borderRadius: '6px',
                  fontSize: '0.8rem',
                  fontWeight: 600,
                }}>
                  <ShieldAlert size={15} />
                  <span>{tabSwitchCount} tab switch flags</span>
                </div>
              )}

              {/* Timer */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                background: timeLeftSec < 300 ? 'rgba(239, 68, 68, 0.2)' : 'rgba(99, 102, 241, 0.2)',
                border: `1px solid ${timeLeftSec < 300 ? 'rgba(239, 68, 68, 0.4)' : 'rgba(99, 102, 241, 0.4)'}`,
                color: timeLeftSec < 300 ? '#f87171' : '#818cf8',
                padding: '6px 14px',
                borderRadius: '8px',
                fontWeight: 700,
                fontFamily: 'var(--font-mono)',
                fontSize: '1.1rem',
              }}>
                <Clock size={18} />
                <span>{formatTimer(timeLeftSec)}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Question Palette & Question Body */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 280px', gap: '24px' }}>
          {/* Main Question Box */}
          <div className="glass-panel" style={{ padding: '28px' }}>
            {currentQ ? (
              <div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                  <span style={{ fontSize: '0.85rem', fontWeight: 700, color: '#818cf8' }}>
                    Question {currentQIndex + 1} of {activeSession.questions.length} &bull; {currentQ.topic || 'General'}
                  </span>
                  <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
                    {currentQ.marks || 5} marks
                  </span>
                </div>

                <h3 style={{ fontSize: '1.15rem', fontWeight: 600, lineHeight: 1.5, marginBottom: '24px' }}>
                  {currentQ.questionText}
                </h3>

                {/* 4 Choices */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {['A', 'B', 'C', 'D'].map(optKey => {
                    const optText = currentQ[`option${optKey}`];
                    if (!optText) return null;
                    const isSelected = answers[currentQ.id] === optKey;

                    return (
                      <div
                        key={optKey}
                        onClick={() => handleSelectOption(currentQ.id, optKey)}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '14px',
                          padding: '14px 18px',
                          borderRadius: '10px',
                          cursor: 'pointer',
                          background: isSelected ? 'rgba(99, 102, 241, 0.2)' : 'rgba(255, 255, 255, 0.03)',
                          border: '1px solid',
                          borderColor: isSelected ? 'rgba(99, 102, 241, 0.6)' : 'rgba(255, 255, 255, 0.08)',
                          transition: 'all 0.15s ease',
                        }}
                      >
                        <span style={{
                          width: '28px',
                          height: '28px',
                          borderRadius: '6px',
                          background: isSelected ? 'var(--primary)' : 'rgba(255, 255, 255, 0.08)',
                          color: isSelected ? '#fff' : '#94a3b8',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontWeight: 700,
                          fontSize: '0.85rem',
                        }}>
                          {optKey}
                        </span>
                        <span style={{ fontSize: '0.92rem', color: isSelected ? '#fff' : '#cbd5e1' }}>
                          {optText}
                        </span>
                      </div>
                    );
                  })}
                </div>

                {/* Prev / Next buttons */}
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '32px' }}>
                  <button
                    disabled={currentQIndex === 0}
                    onClick={() => setCurrentQIndex(prev => prev - 1)}
                    className="btn btn-secondary"
                  >
                    Previous Question
                  </button>
                  <button
                    disabled={currentQIndex === activeSession.questions.length - 1}
                    onClick={() => setCurrentQIndex(prev => prev + 1)}
                    className="btn btn-secondary"
                  >
                    Next Question
                  </button>
                </div>
              </div>
            ) : (
              <div>No question found</div>
            )}
          </div>

          {/* Right Navigator & Submit Button */}
          <div className="glass-panel" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: '#94a3b8' }}>
              Question Palette
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '8px' }}>
              {activeSession.questions.map((q, idx) => {
                const isAnswered = !!answers[q.id];
                const isCurrent = currentQIndex === idx;

                return (
                  <button
                    key={q.id || idx}
                    onClick={() => setCurrentQIndex(idx)}
                    style={{
                      height: '38px',
                      borderRadius: '8px',
                      border: '1px solid',
                      borderColor: isCurrent ? '#6366f1' : 'transparent',
                      background: isAnswered ? 'rgba(16, 185, 129, 0.25)' : 'rgba(255, 255, 255, 0.05)',
                      color: isAnswered ? '#34d399' : '#94a3b8',
                      fontWeight: 700,
                      fontSize: '0.82rem',
                      cursor: 'pointer',
                    }}
                  >
                    {idx + 1}
                  </button>
                );
              })}
            </div>

            <div style={{ fontSize: '0.78rem', color: '#64748b', display: 'flex', flexDirection: 'column', gap: '4px', marginTop: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ width: '10px', height: '10px', borderRadius: '2px', background: 'rgba(16, 185, 129, 0.6)' }} />
                <span>Answered ({Object.keys(answers).length})</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ width: '10px', height: '10px', borderRadius: '2px', background: 'rgba(255, 255, 255, 0.1)' }} />
                <span>Unanswered ({activeSession.questions.length - Object.keys(answers).length})</span>
              </div>
            </div>

            <div style={{ marginTop: 'auto', paddingTop: '16px', borderTop: '1px solid var(--border-subtle)' }}>
              <button
                onClick={handleSubmitTest}
                className="btn btn-primary"
                style={{ width: '100%', padding: '12px' }}
              >
                <Send size={16} />
                <span>Submit Assessment</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '1400px', margin: '0 auto', padding: '32px 24px' }}>
      {/* Top Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <FileText size={24} color="#6366f1" />
            <h2 style={{ fontSize: '1.5rem', margin: 0 }}>Assessments & Examinations</h2>
          </div>
          <p style={{ color: '#94a3b8', fontSize: '0.88rem', marginTop: '4px' }}>
            {isTeacher ? 'Design tests with instant auto-grading, real-time autosave, and proctoring telemetry.' : 'Take scheduled tests, monitor your timer, and view graded scorecards.'}
          </p>
        </div>

        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          {/* Class selector */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Classroom:</span>
            <select
              className="select-field"
              value={selectedClassId}
              onChange={(e) => setSelectedClassId(e.target.value)}
              style={{ width: '240px', padding: '7px 12px' }}
            >
              {classes.map(c => (
                <option key={c.id} value={c.id}>{c.name} ({c.subject})</option>
              ))}
            </select>
          </div>

          {isTeacher && (
            <button
              onClick={() => setShowCreateModal(true)}
              disabled={!selectedClassId}
              className="btn btn-primary btn-sm"
            >
              <Plus size={16} />
              Create Assessment
            </button>
          )}
        </div>
      </div>

      {/* Tests Grid */}
      {loading ? (
        <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>Loading tests...</div>
      ) : tests.length === 0 ? (
        <div className="glass-panel" style={{ padding: '60px 20px', textAlign: 'center' }}>
          <FileText size={48} color="#64748b" style={{ margin: '0 auto 16px', opacity: 0.5 }} />
          <h3>No Assessments Available</h3>
          <p style={{ color: '#94a3b8', marginTop: '8px', maxWidth: '480px', margin: '8px auto 20px' }}>
            {isTeacher ? 'Create your first quiz or exam for this class to start assessing your students.' : 'Your instructor has not scheduled any assessments for this class yet.'}
          </p>
          {isTeacher && (
            <button onClick={() => setShowCreateModal(true)} className="btn btn-primary">
              <Plus size={16} />
              Create First Assessment
            </button>
          )}
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(380px, 1fr))', gap: '20px' }}>
          {tests.map(test => (
            <div key={test.id} className="glass-panel glow-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column' }}>
              <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '10px', marginBottom: '10px' }}>
                <h3 style={{ fontSize: '1.2rem', margin: 0 }}>{test.title}</h3>
                <span className="badge badge-method-get" style={{ fontSize: '0.72rem' }}>
                  {test.durationMinutes} Mins
                </span>
              </div>

              <p style={{ color: '#94a3b8', fontSize: '0.85rem', margin: '0 0 16px', flex: 1 }}>
                {test.description || 'Examination assessment'}
              </p>

              <div style={{
                background: 'rgba(255, 255, 255, 0.02)',
                borderRadius: '8px',
                padding: '12px',
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '8px',
                fontSize: '0.78rem',
                color: '#94a3b8',
                marginBottom: '16px',
              }}>
                <div>Questions: <strong style={{ color: '#f8fafc' }}>{test.questions?.length || 0}</strong></div>
                <div>Duration: <strong style={{ color: '#f8fafc' }}>{test.durationMinutes} min</strong></div>
                <div>Status: <strong style={{ color: '#10b981' }}>Active</strong></div>
                <div>ID: <strong style={{ color: '#818cf8', fontFamily: 'var(--font-mono)' }}>{test.id.substring(0, 8)}...</strong></div>
              </div>

              {/* Action buttons */}
              <div style={{ display: 'flex', gap: '10px', marginTop: 'auto' }}>
                {isStudent && (
                  <>
                    <button
                      onClick={() => handleStartTest(test)}
                      className="btn btn-primary btn-sm"
                      style={{ flex: 1 }}
                    >
                      <Play size={14} /> Start Test
                    </button>
                    <button
                      onClick={() => handleViewMyScorecard(test.id)}
                      className="btn btn-secondary btn-sm"
                      title="View existing submission"
                    >
                      <Award size={14} /> Scorecard
                    </button>
                  </>
                )}

                {isTeacher && (
                  <button
                    onClick={() => handleViewSubmissions(test)}
                    className="btn btn-secondary btn-sm"
                    style={{ width: '100%' }}
                  >
                    <Eye size={14} /> View Student Submissions
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Graded Scorecard Modal */}
      {scorecard && (
        <div className="modal-overlay" onClick={() => setScorecard(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '800px', padding: '28px' }}>
            <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '20px' }}>
              <div>
                <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#10b981', textTransform: 'uppercase' }}>
                  Auto-Graded Examination Scorecard
                </span>
                <h2 style={{ fontSize: '1.4rem', margin: '4px 0 0' }}>{scorecard.testTitle || 'Assessment Results'}</h2>
              </div>
              <button onClick={() => setScorecard(null)} className="btn btn-secondary btn-sm">Close</button>
            </div>

            {/* Overall Score Badge */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(4, 1fr)',
              gap: '12px',
              padding: '16px',
              borderRadius: '12px',
              background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.15) 0%, rgba(16, 185, 129, 0.15) 100%)',
              border: '1px solid rgba(99, 102, 241, 0.25)',
              marginBottom: '24px',
              textAlign: 'center',
            }}>
              <div>
                <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Score Awarded</div>
                <div style={{ fontSize: '1.4rem', fontWeight: 700, color: '#10b981' }}>
                  {scorecard.totalMarksAwarded} / {scorecard.maximumMarks}
                </div>
              </div>
              <div>
                <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Percentage</div>
                <div style={{ fontSize: '1.4rem', fontWeight: 700, color: '#38bdf8' }}>
                  {scorecard.percentage ? `${scorecard.percentage.toFixed(1)}%` : '100%'}
                </div>
              </div>
              <div>
                <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Status</div>
                <div style={{ fontSize: '1.4rem', fontWeight: 700, color: '#f8fafc' }}>
                  {scorecard.status || 'GRADED'}
                </div>
              </div>
              <div>
                <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Tab Switch Violations</div>
                <div style={{ fontSize: '1.4rem', fontWeight: 700, color: scorecard.tabSwitchCount > 0 ? '#ef4444' : '#10b981' }}>
                  {scorecard.tabSwitchCount || 0}
                </div>
              </div>
            </div>

            {/* Questions Detailed Breakdown */}
            <h4 style={{ fontSize: '1rem', marginBottom: '14px' }}>Question Explanations & Grading</h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', maxHeight: '420px', overflowY: 'auto' }}>
              {(scorecard.questionBreakdowns || scorecard.questions || []).map((q, idx) => {
                const isCorrect = q.selectedOption === q.correctOption;

                return (
                  <div
                    key={q.questionId || idx}
                    style={{
                      padding: '16px',
                      borderRadius: '10px',
                      background: 'rgba(255, 255, 255, 0.03)',
                      border: '1px solid',
                      borderColor: isCorrect ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <span style={{ fontSize: '0.8rem', fontWeight: 700, color: '#818cf8' }}>
                        Q{idx + 1}: {q.topic || 'General'}
                      </span>
                      <span style={{
                        fontSize: '0.75rem',
                        fontWeight: 700,
                        color: isCorrect ? '#10b981' : '#ef4444',
                      }}>
                        {isCorrect ? `+${q.marksAwarded || q.marks || 5} marks (Correct)` : `0 marks (Incorrect)`}
                      </span>
                    </div>

                    <p style={{ fontSize: '0.9rem', color: '#f8fafc', marginBottom: '12px' }}>
                      {q.questionText}
                    </p>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', fontSize: '0.82rem', marginBottom: '10px' }}>
                      <div style={{ color: isCorrect ? '#34d399' : '#f87171' }}>
                        Your Choice: <strong>{q.selectedOption || '(None)'}</strong>
                      </div>
                      <div style={{ color: '#34d399' }}>
                        Correct Answer: <strong>{q.correctOption}</strong>
                      </div>
                    </div>

                    {q.explanation && (
                      <div style={{
                        background: 'rgba(99, 102, 241, 0.1)',
                        padding: '10px',
                        borderRadius: '6px',
                        fontSize: '0.8rem',
                        color: '#cbd5e1',
                      }}>
                        <strong style={{ color: '#818cf8' }}>Explanation: </strong>
                        {q.explanation}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Teacher Submissions Viewer Modal */}
      {submissionsList && selectedTestForSubs && (
        <div className="modal-overlay" onClick={() => setSubmissionsList(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '850px', padding: '28px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
              <div>
                <h2 style={{ fontSize: '1.3rem', margin: 0 }}>Class Submissions & Proctoring</h2>
                <span style={{ color: '#94a3b8', fontSize: '0.85rem' }}>{selectedTestForSubs.title}</span>
              </div>
              <button onClick={() => setSubmissionsList(null)} className="btn btn-secondary btn-sm">Close</button>
            </div>

            {submissionsList.length === 0 ? (
              <div style={{ padding: '40px', textAlign: 'center', color: '#64748b' }}>
                No students have submitted this assessment yet.
              </div>
            ) : (
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
                <thead>
                  <tr style={{ background: 'rgba(15, 23, 42, 0.7)', borderBottom: '1px solid var(--border-subtle)' }}>
                    <th style={{ padding: '12px 16px', color: '#94a3b8', fontWeight: 600 }}>Student ID</th>
                    <th style={{ padding: '12px 16px', color: '#94a3b8', fontWeight: 600 }}>Score</th>
                    <th style={{ padding: '12px 16px', color: '#94a3b8', fontWeight: 600 }}>Percentage</th>
                    <th style={{ padding: '12px 16px', color: '#94a3b8', fontWeight: 600 }}>Tab Switch Violations</th>
                    <th style={{ padding: '12px 16px', color: '#94a3b8', fontWeight: 600 }}>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {submissionsList.map((sub, i) => (
                    <tr key={sub.id || i} style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.04)' }}>
                      <td style={{ padding: '12px 16px', fontFamily: 'var(--font-mono)', fontSize: '0.78rem', color: '#818cf8' }}>
                        {sub.studentId}
                      </td>
                      <td style={{ padding: '12px 16px', fontWeight: 700, color: '#10b981' }}>
                        {sub.totalMarksAwarded} / {sub.maximumMarks}
                      </td>
                      <td style={{ padding: '12px 16px', color: '#38bdf8' }}>
                        {sub.percentage ? `${sub.percentage.toFixed(1)}%` : '100%'}
                      </td>
                      <td style={{ padding: '12px 16px', color: sub.tabSwitchCount > 0 ? '#ef4444' : '#94a3b8' }}>
                        {sub.tabSwitchCount || 0}
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <span className="badge badge-status-accepted">{sub.status || 'SUBMITTED'}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {/* Create Test Modal */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '780px', padding: '28px' }}>
            <h3 style={{ marginBottom: '16px' }}>Create Assessment Test</h3>

            <form onSubmit={handleCreateTest}>
              <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '16px' }}>
                <div className="input-group">
                  <label className="input-label">Test Title</label>
                  <input
                    type="text"
                    className="input-field"
                    placeholder="e.g. Midterm: Graph Theory & Heaps"
                    value={testTitle}
                    onChange={(e) => setTestTitle(e.target.value)}
                    required
                  />
                </div>

                <div className="input-group">
                  <label className="input-label">Duration (Minutes)</label>
                  <input
                    type="number"
                    className="input-field"
                    value={durationMinutes}
                    onChange={(e) => setDurationMinutes(e.target.value)}
                    required
                    min={1}
                  />
                </div>
              </div>

              <div className="input-group">
                <label className="input-label">Description</label>
                <textarea
                  className="textarea-field"
                  placeholder="Exam instructions, coverage..."
                  value={testDesc}
                  onChange={(e) => setTestDesc(e.target.value)}
                  style={{ minHeight: '60px' }}
                />
              </div>

              {/* Questions List Builder */}
              <div style={{ margin: '20px 0' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
                  <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>MCQ Questions ({questions.length})</span>
                  <button type="button" onClick={addQuestionField} className="btn btn-secondary btn-sm">
                    <Plus size={14} /> Add Question
                  </button>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', maxHeight: '320px', overflowY: 'auto' }}>
                  {questions.map((q, idx) => (
                    <div key={idx} style={{ padding: '14px', borderRadius: '8px', background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr auto', gap: '10px', marginBottom: '8px' }}>
                        <input
                          type="text"
                          placeholder="Topic"
                          className="input-field"
                          value={q.topic}
                          onChange={(e) => {
                            const updated = [...questions];
                            updated[idx].topic = e.target.value;
                            setQuestions(updated);
                          }}
                        />
                        <select
                          className="select-field"
                          value={q.correctOption}
                          onChange={(e) => {
                            const updated = [...questions];
                            updated[idx].correctOption = e.target.value;
                            setQuestions(updated);
                          }}
                        >
                          <option value="A">Correct Option: A</option>
                          <option value="B">Correct Option: B</option>
                          <option value="C">Correct Option: C</option>
                          <option value="D">Correct Option: D</option>
                        </select>
                        <input
                          type="number"
                          placeholder="Marks"
                          className="input-field"
                          style={{ width: '80px' }}
                          value={q.marks}
                          onChange={(e) => {
                            const updated = [...questions];
                            updated[idx].marks = parseInt(e.target.value, 10);
                            setQuestions(updated);
                          }}
                        />
                      </div>

                      <input
                        type="text"
                        placeholder="Question text..."
                        className="input-field"
                        value={q.questionText}
                        onChange={(e) => {
                          const updated = [...questions];
                          updated[idx].questionText = e.target.value;
                          setQuestions(updated);
                        }}
                        style={{ marginBottom: '8px' }}
                        required
                      />

                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '8px' }}>
                        {['A', 'B', 'C', 'D'].map(key => (
                          <input
                            key={key}
                            type="text"
                            placeholder={`Option ${key}`}
                            className="input-field"
                            value={q[`option${key}`]}
                            onChange={(e) => {
                              const updated = [...questions];
                              updated[idx][`option${key}`] = e.target.value;
                              setQuestions(updated);
                            }}
                            required
                          />
                        ))}
                      </div>

                      <input
                        type="text"
                        placeholder="Teacher explanation for grading..."
                        className="input-field"
                        value={q.explanation}
                        onChange={(e) => {
                          const updated = [...questions];
                          updated[idx].explanation = e.target.value;
                          setQuestions(updated);
                        }}
                      />
                    </div>
                  ))}
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <button type="button" onClick={() => setShowCreateModal(false)} className="btn btn-secondary">
                  Cancel
                </button>
                <button type="submit" disabled={creating} className="btn btn-primary">
                  {creating ? 'Publishing...' : 'Publish Assessment'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
