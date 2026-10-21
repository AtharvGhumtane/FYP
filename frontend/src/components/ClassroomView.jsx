import React, { useState, useEffect } from 'react';
import { useApp } from '../context/AppContext';
import { executeApiCall } from '../services/api';
import {
  BookOpen,
  Plus,
  Users,
  Copy,
  Check,
  CheckCircle,
  XCircle,
  Clock,
  ChevronRight,
  RefreshCw,
  FolderPlus,
  ArrowRight
} from 'lucide-react';

export default function ClassroomView({ onSelectClassForTests }) {
  const { baseUrl, currentUser, addToast } = useApp();

  const [classes, setClasses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedClass, setSelectedClass] = useState(null);
  const [roster, setRoster] = useState([]);
  const [requests, setRequests] = useState([]);
  const [rosterLoading, setRosterLoading] = useState(false);

  // Create class modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newClassName, setNewClassName] = useState('');
  const [newClassSubject, setNewClassSubject] = useState('');
  const [newClassDesc, setNewClassDesc] = useState('');
  const [creating, setCreating] = useState(false);

  // Join class modal for student
  const [showJoinModal, setShowJoinModal] = useState(false);
  const [joinClassId, setJoinClassId] = useState('');
  const [joining, setJoining] = useState(false);

  const [copiedCode, setCopiedCode] = useState(null);

  const isTeacher = currentUser?.role === 'ROLE_TEACHER';
  const isStudent = currentUser?.role === 'ROLE_STUDENT';

  useEffect(() => {
    if (currentUser) {
      fetchClasses();
    }
  }, [currentUser]);

  const fetchClasses = async () => {
    setLoading(true);
    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'GET',
        path: '/api/classes',
        token: currentUser?.token,
      });

      if (res.ok && Array.isArray(res.data)) {
        setClasses(res.data);
        if (res.data.length > 0 && !selectedClass) {
          selectClass(res.data[0]);
        }
      }
    } catch (err) {
      addToast('error', `Error loading classes: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const selectClass = async (cls) => {
    setSelectedClass(cls);
    setRosterLoading(true);

    try {
      // Fetch roster
      const rosterRes = await executeApiCall({
        baseUrl,
        method: 'GET',
        path: `/api/classes/${cls.id}/roster`,
        token: currentUser?.token,
      });

      if (rosterRes.ok && Array.isArray(rosterRes.data)) {
        setRoster(rosterRes.data);
      }

      // If teacher, also fetch pending enrollment requests
      if (isTeacher) {
        const reqRes = await executeApiCall({
          baseUrl,
          method: 'GET',
          path: `/api/classes/${cls.id}/requests`,
          token: currentUser?.token,
        });

        if (reqRes.ok && Array.isArray(reqRes.data)) {
          setRequests(reqRes.data);
        }
      }
    } catch (err) {
      console.error(err);
    } finally {
      setRosterLoading(false);
    }
  };

  const handleCreateClass = async (e) => {
    e.preventDefault();
    setCreating(true);

    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: '/api/classes',
        token: currentUser?.token,
        body: {
          name: newClassName,
          subject: newClassSubject,
          description: newClassDesc,
        },
      });

      if (res.ok && res.data) {
        addToast('success', `Classroom "${res.data.name}" created!`);
        setShowCreateModal(false);
        setNewClassName('');
        setNewClassSubject('');
        setNewClassDesc('');
        fetchClasses();
        selectClass(res.data);
      } else {
        addToast('error', `Failed to create class: ${res.data?.message || res.error}`);
      }
    } catch (err) {
      addToast('error', `Error: ${err.message}`);
    } finally {
      setCreating(false);
    }
  };

  const handleJoinClass = async (e) => {
    e.preventDefault();
    setJoining(true);

    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: `/api/classes/${joinClassId.trim()}/join`,
        token: currentUser?.token,
      });

      if (res.ok) {
        addToast('success', 'Join request submitted! Awaiting teacher approval.');
        setShowJoinModal(false);
        setJoinClassId('');
        fetchClasses();
      } else {
        addToast('error', `Failed to join: ${res.data?.message || res.error || 'Check Class ID'}`);
      }
    } catch (err) {
      addToast('error', `Error: ${err.message}`);
    } finally {
      setJoining(false);
    }
  };

  const handleDecideRequest = async (classId, studentId, decision) => {
    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: `/api/classes/${classId}/requests/${studentId}`,
        token: currentUser?.token,
        body: { status: decision },
      });

      if (res.ok) {
        addToast('success', `Student request ${decision.toLowerCase()}!`);
        if (selectedClass) {
          selectClass(selectedClass);
        }
      } else {
        addToast('error', `Decision failed: ${res.data?.message || res.error}`);
      }
    } catch (err) {
      addToast('error', `Error: ${err.message}`);
    }
  };

  const copyClassCode = (code) => {
    navigator.clipboard.writeText(code);
    setCopiedCode(code);
    setTimeout(() => setCopiedCode(null), 2000);
  };

  return (
    <div style={{ maxWidth: '1400px', margin: '0 auto', padding: '32px 24px' }}>
      {/* Top Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <BookOpen size={24} color="#6366f1" />
            <h2 style={{ fontSize: '1.5rem', margin: 0 }}>Classroom Workspace</h2>
          </div>
          <p style={{ color: '#94a3b8', fontSize: '0.88rem', marginTop: '4px' }}>
            {isTeacher ? 'Manage your classrooms, review enrollments, and coordinate assessments.' : 'View your enrolled courses and discover course materials.'}
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button onClick={fetchClasses} className="btn btn-secondary btn-sm" disabled={loading}>
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            Refresh
          </button>

          {isTeacher && (
            <button onClick={() => setShowCreateModal(true)} className="btn btn-primary btn-sm">
              <Plus size={16} />
              Create Classroom
            </button>
          )}

          {isStudent && (
            <button onClick={() => setShowJoinModal(true)} className="btn btn-primary btn-sm">
              <FolderPlus size={16} />
              Join Classroom
            </button>
          )}
        </div>
      </div>

      {/* Main Grid: Class list on left, Class detail on right */}
      <div style={{ display: 'grid', gridTemplateColumns: '360px 1fr', gap: '24px' }}>
        {/* Left column: Classes directory */}
        <div className="glass-panel" style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <div style={{ fontSize: '0.82rem', fontWeight: 700, color: '#94a3b8', padding: '0 4px 6px', borderBottom: '1px solid var(--border-subtle)' }}>
            {isTeacher ? 'Classes You Teach' : 'Enrolled Classes'} ({classes.length})
          </div>

          {classes.length === 0 ? (
            <div style={{ padding: '30px 16px', textAlign: 'center', color: '#64748b', fontSize: '0.85rem' }}>
              {isTeacher ? 'No classrooms yet. Click "Create Classroom" to start!' : 'You are not enrolled in any classrooms yet.'}
            </div>
          ) : (
            classes.map(cls => {
              const isSelected = selectedClass?.id === cls.id;
              return (
                <div
                  key={cls.id}
                  onClick={() => selectClass(cls)}
                  style={{
                    padding: '14px',
                    borderRadius: '10px',
                    cursor: 'pointer',
                    background: isSelected ? 'rgba(99, 102, 241, 0.15)' : 'rgba(255, 255, 255, 0.03)',
                    border: '1px solid',
                    borderColor: isSelected ? 'rgba(99, 102, 241, 0.4)' : 'rgba(255, 255, 255, 0.05)',
                    transition: 'all 0.2s',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '4px' }}>
                    <div style={{ fontWeight: 600, fontSize: '0.92rem', color: isSelected ? '#fff' : '#e2e8f0' }}>
                      {cls.name}
                    </div>
                    <span style={{ fontSize: '0.7rem', color: '#818cf8', fontWeight: 600 }}>
                      {cls.subject}
                    </span>
                  </div>

                  <p style={{ color: '#94a3b8', fontSize: '0.78rem', margin: '4px 0 8px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {cls.description || 'No description provided'}
                  </p>

                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.72rem', color: '#64748b' }}>
                    <span>Code: <code style={{ color: '#38bdf8' }}>{cls.classCode || cls.id.substring(0, 8)}</code></span>
                    <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <Users size={12} /> {cls.studentCount || 0} students
                    </span>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Right column: Selected Class Details */}
        {selectedClass ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {/* Banner */}
            <div className="glass-panel glow-card" style={{ padding: '24px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
                <div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#818cf8', textTransform: 'uppercase' }}>
                    {selectedClass.subject}
                  </span>
                  <h3 style={{ fontSize: '1.4rem', margin: '4px 0 8px' }}>{selectedClass.name}</h3>
                  <p style={{ color: '#94a3b8', fontSize: '0.9rem', maxWidth: '640px', margin: 0 }}>
                    {selectedClass.description}
                  </p>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'flex-end' }}>
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    background: 'rgba(0,0,0,0.3)',
                    padding: '6px 12px',
                    borderRadius: '8px',
                    border: '1px solid rgba(255,255,255,0.08)'
                  }}>
                    <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Class ID / Code:</span>
                    <code style={{ color: '#38bdf8', fontWeight: 700, fontSize: '0.85rem' }}>{selectedClass.id}</code>
                    <button
                      onClick={() => copyClassCode(selectedClass.id)}
                      style={{ background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer', padding: '2px' }}
                      title="Copy Class ID"
                    >
                      {copiedCode === selectedClass.id ? <Check size={14} color="#10b981" /> : <Copy size={14} />}
                    </button>
                  </div>

                  {onSelectClassForTests && (
                    <button
                      onClick={() => onSelectClassForTests(selectedClass)}
                      className="btn btn-accent btn-sm"
                      style={{ marginTop: '4px' }}
                    >
                      <span>View Class Assessments</span>
                      <ArrowRight size={14} />
                    </button>
                  )}
                </div>
              </div>
            </div>

            {/* Pending Requests (Teacher Only) */}
            {isTeacher && requests.length > 0 && (
              <div className="glass-panel" style={{ padding: '20px', borderLeft: '4px solid #f59e0b' }}>
                <h4 style={{ fontSize: '1rem', color: '#f59e0b', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Clock size={16} /> Pending Student Join Requests ({requests.length})
                </h4>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {requests.map(req => (
                    <div
                      key={req.id || req.studentId}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '12px 16px',
                        background: 'rgba(255, 255, 255, 0.03)',
                        borderRadius: '8px',
                      }}
                    >
                      <div>
                        <div style={{ fontWeight: 600, fontSize: '0.88rem', color: '#f8fafc' }}>
                          {req.studentName || 'Student Candidate'}
                        </div>
                        <div style={{ fontSize: '0.78rem', color: '#94a3b8' }}>
                          {req.studentEmail || req.studentId}
                        </div>
                      </div>

                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          onClick={() => handleDecideRequest(selectedClass.id, req.studentId, 'ACCEPTED')}
                          className="btn btn-sm"
                          style={{ background: 'rgba(16, 185, 129, 0.2)', color: '#34d399', border: '1px solid rgba(16, 185, 129, 0.4)' }}
                        >
                          <CheckCircle size={14} /> Accept
                        </button>
                        <button
                          onClick={() => handleDecideRequest(selectedClass.id, req.studentId, 'REJECTED')}
                          className="btn btn-sm"
                          style={{ background: 'rgba(239, 68, 68, 0.2)', color: '#f87171', border: '1px solid rgba(239, 68, 68, 0.4)' }}
                        >
                          <XCircle size={14} /> Reject
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Enrolled Roster */}
            <div className="glass-panel" style={{ overflow: 'hidden' }}>
              <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border-subtle)', fontWeight: 600, fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Users size={16} color="#818cf8" />
                Class Roster ({roster.length} Enrolled)
              </div>

              {rosterLoading ? (
                <div style={{ padding: '30px', textAlign: 'center', color: '#94a3b8' }}>Loading class roster...</div>
              ) : roster.length === 0 ? (
                <div style={{ padding: '40px', textAlign: 'center', color: '#64748b' }}>
                  No students accepted into this class yet.
                </div>
              ) : (
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
                  <thead>
                    <tr style={{ background: 'rgba(15, 23, 42, 0.7)', borderBottom: '1px solid var(--border-subtle)' }}>
                      <th style={{ padding: '12px 20px', color: '#94a3b8', fontWeight: 600 }}>Student Name</th>
                      <th style={{ padding: '12px 20px', color: '#94a3b8', fontWeight: 600 }}>Email</th>
                      <th style={{ padding: '12px 20px', color: '#94a3b8', fontWeight: 600 }}>Status</th>
                      <th style={{ padding: '12px 20px', color: '#94a3b8', fontWeight: 600 }}>Student ID</th>
                    </tr>
                  </thead>
                  <tbody>
                    {roster.map(st => (
                      <tr key={st.studentId} style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.04)' }}>
                        <td style={{ padding: '12px 20px', fontWeight: 600, color: '#f8fafc' }}>{st.studentName}</td>
                        <td style={{ padding: '12px 20px', color: '#94a3b8' }}>{st.studentEmail}</td>
                        <td style={{ padding: '12px 20px' }}>
                          <span className="badge badge-status-accepted">{st.status || 'ACCEPTED'}</span>
                        </td>
                        <td style={{ padding: '12px 20px', fontFamily: 'var(--font-mono)', fontSize: '0.75rem', color: '#64748b' }}>
                          {st.studentId}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        ) : (
          <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', color: '#64748b' }}>
            Select a classroom on the left to inspect its details and roster.
          </div>
        )}
      </div>

      {/* Create Class Modal */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ padding: '24px' }}>
            <h3 style={{ marginBottom: '16px' }}>Create New Classroom</h3>
            <form onSubmit={handleCreateClass}>
              <div className="input-group">
                <label className="input-label">Class Name</label>
                <input
                  type="text"
                  className="input-field"
                  placeholder="e.g. Advanced Operating Systems"
                  value={newClassName}
                  onChange={(e) => setNewClassName(e.target.value)}
                  required
                />
              </div>

              <div className="input-group">
                <label className="input-label">Subject</label>
                <input
                  type="text"
                  className="input-field"
                  placeholder="e.g. Computer Science"
                  value={newClassSubject}
                  onChange={(e) => setNewClassSubject(e.target.value)}
                  required
                />
              </div>

              <div className="input-group">
                <label className="input-label">Description</label>
                <textarea
                  className="textarea-field"
                  placeholder="Course overview, syllabus highlights..."
                  value={newClassDesc}
                  onChange={(e) => setNewClassDesc(e.target.value)}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                <button type="button" onClick={() => setShowCreateModal(false)} className="btn btn-secondary">
                  Cancel
                </button>
                <button type="submit" disabled={creating} className="btn btn-primary">
                  {creating ? 'Creating...' : 'Create Class'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Join Class Modal */}
      {showJoinModal && (
        <div className="modal-overlay" onClick={() => setShowJoinModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ padding: '24px', maxWidth: '440px' }}>
            <h3 style={{ marginBottom: '8px' }}>Join a Classroom</h3>
            <p style={{ color: '#94a3b8', fontSize: '0.85rem', marginBottom: '20px' }}>
              Enter the Class ID or Code provided by your instructor.
            </p>
            <form onSubmit={handleJoinClass}>
              <div className="input-group">
                <label className="input-label">Class ID</label>
                <input
                  type="text"
                  className="input-field"
                  placeholder="e.g. 550e8400-e29b-41d4-a716-446655440000"
                  value={joinClassId}
                  onChange={(e) => setJoinClassId(e.target.value)}
                  required
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                <button type="button" onClick={() => setShowJoinModal(false)} className="btn btn-secondary">
                  Cancel
                </button>
                <button type="submit" disabled={joining} className="btn btn-primary">
                  {joining ? 'Submitting...' : 'Send Join Request'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
