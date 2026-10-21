import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { executeApiCall } from '../services/api';
import {
  X,
  Lock,
  Mail,
  User,
  Key,
  GraduationCap,
  BookOpen,
  ArrowRight,
  ShieldCheck
} from 'lucide-react';

export default function AuthModal({ isOpen, onClose }) {
  const { baseUrl, loginUser, addToast } = useApp();

  const [mode, setMode] = useState('login'); // 'login' | 'student-register' | 'teacher-register'
  const [loading, setLoading] = useState(false);

  // Form states
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [secretKey, setSecretKey] = useState('');

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (mode === 'login') {
        const res = await executeApiCall({
          baseUrl,
          method: 'POST',
          path: '/api/auth/login',
          body: { email, password },
        });

        if (res.ok && res.data?.token) {
          loginUser(res.data);
          onClose();
        } else {
          addToast('error', `Login failed (${res.status}): ${res.data?.message || res.error || 'Invalid credentials'}`);
        }
      } else if (mode === 'student-register') {
        const res = await executeApiCall({
          baseUrl,
          method: 'POST',
          path: '/api/auth/register/student',
          body: { fullName, email, password },
        });

        if (res.ok && res.data?.token) {
          loginUser(res.data);
          onClose();
        } else {
          addToast('error', `Student registration failed: ${res.data?.message || res.error}`);
        }
      } else if (mode === 'teacher-register') {
        const res = await executeApiCall({
          baseUrl,
          method: 'POST',
          path: '/api/auth/register/teacher',
          body: { fullName, email, password, secretKey },
        });

        if (res.ok && res.data?.token) {
          loginUser(res.data);
          onClose();
        } else {
          addToast('error', `Teacher registration failed: ${res.data?.message || res.error || 'Check teacher secret key'}`);
        }
      }
    } catch (err) {
      addToast('error', `Operation failed: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSimulate = async () => {
    setLoading(true);
    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: '/api/auth/google',
        body: {
          idToken: 'mock-google-id-token-' + Date.now(),
          role: 'ROLE_STUDENT',
          teacherSecretKey: ''
        }
      });

      if (res.ok && res.data?.token) {
        loginUser(res.data);
        onClose();
      } else {
        addToast('error', `Google auth error: ${res.data?.message || res.error}`);
      }
    } catch (err) {
      addToast('error', `Google simulation failed: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ padding: '28px' }}>
        {/* Modal Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
          <div>
            <h2 style={{ fontSize: '1.35rem', margin: 0 }}>
              {mode === 'login' && 'Sign in to SmartExam'}
              {mode === 'student-register' && 'Student Registration'}
              {mode === 'teacher-register' && 'Teacher Registration'}
            </h2>
            <p style={{ color: '#94a3b8', fontSize: '0.85rem', marginTop: '4px' }}>
              Microservices-backed authentication with JWT and Role-Based Access Control
            </p>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              color: '#94a3b8',
              cursor: 'pointer',
              padding: '6px',
            }}
          >
            <X size={20} />
          </button>
        </div>

        {/* Tab switch */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr 1fr',
          gap: '6px',
          background: 'rgba(15, 23, 42, 0.8)',
          padding: '4px',
          borderRadius: '10px',
          marginBottom: '20px',
        }}>
          <button
            type="button"
            onClick={() => setMode('login')}
            style={{
              padding: '8px',
              borderRadius: '7px',
              border: 'none',
              fontSize: '0.8rem',
              fontWeight: 600,
              cursor: 'pointer',
              background: mode === 'login' ? 'var(--primary)' : 'transparent',
              color: mode === 'login' ? '#fff' : '#94a3b8',
            }}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => setMode('student-register')}
            style={{
              padding: '8px',
              borderRadius: '7px',
              border: 'none',
              fontSize: '0.8rem',
              fontWeight: 600,
              cursor: 'pointer',
              background: mode === 'student-register' ? 'var(--primary)' : 'transparent',
              color: mode === 'student-register' ? '#fff' : '#94a3b8',
            }}
          >
            Student Sign Up
          </button>
          <button
            type="button"
            onClick={() => setMode('teacher-register')}
            style={{
              padding: '8px',
              borderRadius: '7px',
              border: 'none',
              fontSize: '0.8rem',
              fontWeight: 600,
              cursor: 'pointer',
              background: mode === 'teacher-register' ? 'var(--primary)' : 'transparent',
              color: mode === 'teacher-register' ? '#fff' : '#94a3b8',
            }}
          >
            Teacher Sign Up
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit}>
          {mode !== 'login' && (
            <div className="input-group">
              <label className="input-label">Full Name</label>
              <div style={{ position: 'relative' }}>
                <User size={16} color="#64748b" style={{ position: 'absolute', left: '12px', top: '12px' }} />
                <input
                  type="text"
                  className="input-field"
                  placeholder="e.g. Marie Curie"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  style={{ paddingLeft: '38px' }}
                  required
                />
              </div>
            </div>
          )}

          <div className="input-group">
            <label className="input-label">Email Address</label>
            <div style={{ position: 'relative' }}>
              <Mail size={16} color="#64748b" style={{ position: 'absolute', left: '12px', top: '12px' }} />
              <input
                type="email"
                className="input-field"
                placeholder="name@university.edu"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                style={{ paddingLeft: '38px' }}
                required
              />
            </div>
          </div>

          <div className="input-group">
            <label className="input-label">Password</label>
            <div style={{ position: 'relative' }}>
              <Lock size={16} color="#64748b" style={{ position: 'absolute', left: '12px', top: '12px' }} />
              <input
                type="password"
                className="input-field"
                placeholder="Password123!"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                style={{ paddingLeft: '38px' }}
                required
              />
            </div>
          </div>

          {mode === 'teacher-register' && (
            <div className="input-group">
              <label className="input-label">
                Teacher Authorization Key
                <span style={{ fontSize: '0.72rem', color: '#818cf8' }}>Requires Admin Generation</span>
              </label>
              <div style={{ position: 'relative' }}>
                <Key size={16} color="#64748b" style={{ position: 'absolute', left: '12px', top: '12px' }} />
                <input
                  type="text"
                  className="input-field"
                  placeholder="e.g. TEACHER-SECRET-KEY-2026"
                  value={secretKey}
                  onChange={(e) => setSecretKey(e.target.value)}
                  style={{ paddingLeft: '38px' }}
                  required
                />
              </div>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '8px', padding: '12px' }}
          >
            {loading ? 'Processing...' : (
              <>
                <span>Continue</span>
                <ArrowRight size={16} />
              </>
            )}
          </button>
        </form>

        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          margin: '20px 0',
          color: '#64748b',
          fontSize: '0.8rem',
        }}>
          <div style={{ flex: 1, height: '1px', background: 'rgba(255, 255, 255, 0.08)' }} />
          <span>OR</span>
          <div style={{ flex: 1, height: '1px', background: 'rgba(255, 255, 255, 0.08)' }} />
        </div>

        <button
          onClick={handleGoogleSimulate}
          type="button"
          disabled={loading}
          className="btn btn-secondary"
          style={{ width: '100%', gap: '10px' }}
        >
          <svg width="18" height="18" viewBox="0 0 24 24">
            <path fill="#EA4335" d="M12 5c1.6 0 3 .6 4.1 1.7l3.1-3.1C17.3 1.8 14.8 1 12 1 7.4 1 3.5 3.6 1.6 7.4l3.7 2.9C6.2 7.4 8.9 5 12 5z"/>
            <path fill="#4285F4" d="M23.5 12.3c0-.8-.1-1.7-.2-2.3H12v4.5h6.5c-.3 1.5-1.1 2.8-2.4 3.7l3.7 2.9c2.2-2 3.7-5.1 3.7-8.8z"/>
            <path fill="#FBBC05" d="M5.3 14.8c-.2-.7-.4-1.5-.4-2.3 0-.8.2-1.6.4-2.3L1.6 7.3C.6 9.3 0 11.6 0 14.8c0 3.2.6 5.5 1.6 7.5l3.7-2.9z"/>
            <path fill="#34A853" d="M12 23.5c3.2 0 6-1.1 8-3l-3.7-2.9c-1.1.7-2.5 1.2-4.3 1.2-3.1 0-5.8-2.4-6.7-5.4L1.6 16.3C3.5 20.1 7.4 23.5 12 23.5z"/>
          </svg>
          <span>Continue with Google OAuth (Simulation)</span>
        </button>
      </div>
    </div>
  );
}
