import React, { useState } from 'react';
import { useApp, DEMO_CREDENTIALS } from '../context/AppContext';
import {
  Sparkles,
  Server,
  Layers,
  Terminal,
  PlayCircle,
  Bell,
  LogOut,
  User,
  Shield,
  GraduationCap,
  BookOpen,
  Settings,
  CheckCircle2,
  AlertTriangle
} from 'lucide-react';

export default function Navbar({ onOpenAuth }) {
  const {
    baseUrl,
    setBaseUrl,
    currentUser,
    logout,
    quickLoginAs,
    gatewayStatus,
    checkGatewayHealth,
    activeTab,
    setActiveTab,
    unreadCount,
    setPlatformView
  } = useApp();

  const [showConfigModal, setShowConfigModal] = useState(false);
  const [tempUrl, setTempUrl] = useState(baseUrl);

  const handleSaveBaseUrl = (e) => {
    e.preventDefault();
    setBaseUrl(tempUrl);
    setShowConfigModal(false);
  };

  return (
    <header style={{
      background: 'rgba(11, 15, 25, 0.85)',
      backdropFilter: 'blur(16px)',
      borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
      position: 'sticky',
      top: 0,
      zIndex: 100,
      padding: '0 24px',
    }}>
      <div style={{
        maxWidth: '1600px',
        margin: '0 auto',
        height: '70px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '20px',
      }}>
        {/* Logo and Brand */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '42px',
            height: '42px',
            borderRadius: '12px',
            background: 'linear-gradient(135deg, #6366f1 0%, #ec4899 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 0 20px rgba(99, 102, 241, 0.5)',
          }}>
            <Sparkles size={22} color="#ffffff" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{
                fontFamily: 'var(--font-display)',
                fontSize: '1.25rem',
                fontWeight: '800',
                letterSpacing: '-0.02em',
                background: 'linear-gradient(to right, #fff, #94a3b8)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
              }}>
                SmartExam
              </span>
              <span style={{
                fontSize: '0.7rem',
                fontWeight: '700',
                padding: '2px 7px',
                borderRadius: '6px',
                background: 'rgba(99, 102, 241, 0.2)',
                color: '#818cf8',
                border: '1px solid rgba(99, 102, 241, 0.3)',
              }}>
                AI HUB
              </span>
            </div>
            <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
              Microservices Platform & API Lab
            </div>
          </div>
        </div>

        {/* Central View Switcher */}
        <div style={{
          display: 'flex',
          background: 'rgba(15, 23, 42, 0.7)',
          padding: '4px',
          borderRadius: '12px',
          border: '1px solid rgba(255, 255, 255, 0.08)',
        }}>
          <button
            onClick={() => setActiveTab('platform')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '8px 16px',
              borderRadius: '9px',
              border: 'none',
              background: activeTab === 'platform' ? 'var(--primary-gradient)' : 'transparent',
              color: activeTab === 'platform' ? '#fff' : '#94a3b8',
              fontWeight: 600,
              fontSize: '0.85rem',
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
          >
            <Layers size={16} />
            Platform UI
          </button>

          <button
            onClick={() => setActiveTab('explorer')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '8px 16px',
              borderRadius: '9px',
              border: 'none',
              background: activeTab === 'explorer' ? 'var(--primary-gradient)' : 'transparent',
              color: activeTab === 'explorer' ? '#fff' : '#94a3b8',
              fontWeight: 600,
              fontSize: '0.85rem',
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
          >
            <Terminal size={16} />
            API Explorer
          </button>

          <button
            onClick={() => setActiveTab('runner')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '8px 16px',
              borderRadius: '9px',
              border: 'none',
              background: activeTab === 'runner' ? 'var(--primary-gradient)' : 'transparent',
              color: activeTab === 'runner' ? '#fff' : '#94a3b8',
              fontWeight: 600,
              fontSize: '0.85rem',
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
          >
            <PlayCircle size={16} />
            E2E Suite Runner
          </button>
        </div>

        {/* Right Section: Gateway Ping, Quick Logins, Profile */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          {/* Gateway Health Indicator */}
          <button
            onClick={() => setShowConfigModal(true)}
            title="Click to change Gateway URL or check status"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid rgba(255, 255, 255, 0.08)',
              padding: '6px 12px',
              borderRadius: '8px',
              cursor: 'pointer',
              color: '#94a3b8',
              fontSize: '0.78rem',
            }}
          >
            <span
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                background: gatewayStatus === 'healthy' ? '#10b981' : gatewayStatus === 'checking' ? '#f59e0b' : '#ef4444',
                boxShadow: gatewayStatus === 'healthy' ? '0 0 8px #10b981' : 'none',
              }}
            />
            <span>Gateway: {baseUrl.replace(/^https?:\/\//, '')}</span>
            <Settings size={13} style={{ marginLeft: '4px' }} />
          </button>

          {/* Quick Demo Personas */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '0.75rem', color: '#64748b', marginRight: '2px' }}>Quick:</span>
            <button
              onClick={() => quickLoginAs('admin')}
              title="Quick login as Seeded Admin"
              className="btn btn-secondary btn-sm"
              style={{ padding: '5px 9px', fontSize: '0.75rem' }}
            >
              👑 Admin
            </button>
            <button
              onClick={() => quickLoginAs('teacher')}
              title="Quick login as Teacher (Dr. Smith)"
              className="btn btn-secondary btn-sm"
              style={{ padding: '5px 9px', fontSize: '0.75rem' }}
            >
              👨‍🏫 Teacher
            </button>
            <button
              onClick={() => quickLoginAs('student')}
              title="Quick login as Student (Grace Hopper)"
              className="btn btn-secondary btn-sm"
              style={{ padding: '5px 9px', fontSize: '0.75rem' }}
            >
              🎓 Student
            </button>
          </div>

          {/* User Profile or Login Trigger */}
          {currentUser ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <button
                onClick={() => {
                  setActiveTab('platform');
                  setPlatformView('notifications');
                }}
                style={{
                  position: 'relative',
                  background: 'rgba(255, 255, 255, 0.06)',
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                  borderRadius: '8px',
                  padding: '7px',
                  color: '#94a3b8',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                }}
                title="Notifications"
              >
                <Bell size={18} />
                {unreadCount > 0 && (
                  <span style={{
                    position: 'absolute',
                    top: '-4px',
                    right: '-4px',
                    background: '#ef4444',
                    color: '#fff',
                    borderRadius: '10px',
                    fontSize: '0.65rem',
                    fontWeight: 700,
                    padding: '1px 5px',
                  }}>
                    {unreadCount}
                  </span>
                )}
              </button>

              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '6px 12px',
                background: 'rgba(255, 255, 255, 0.04)',
                border: '1px solid rgba(255, 255, 255, 0.08)',
                borderRadius: '10px',
              }}>
                <div style={{
                  width: '28px',
                  height: '28px',
                  borderRadius: '8px',
                  background: currentUser.role === 'ROLE_ADMIN' ? 'rgba(236, 72, 153, 0.2)' : currentUser.role === 'ROLE_TEACHER' ? 'rgba(99, 102, 241, 0.2)' : 'rgba(6, 182, 212, 0.2)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: currentUser.role === 'ROLE_ADMIN' ? '#f472b6' : currentUser.role === 'ROLE_TEACHER' ? '#818cf8' : '#22d3ee',
                }}>
                  {currentUser.role === 'ROLE_ADMIN' ? <Shield size={16} /> : currentUser.role === 'ROLE_TEACHER' ? <BookOpen size={16} /> : <GraduationCap size={16} />}
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#f8fafc', lineHeight: 1.1 }}>
                    {currentUser.fullName}
                  </span>
                  <span style={{ fontSize: '0.7rem', color: '#94a3b8' }}>
                    {currentUser.role.replace('ROLE_', '')}
                  </span>
                </div>
              </div>

              <button
                onClick={logout}
                title="Log out"
                style={{
                  background: 'rgba(239, 68, 68, 0.1)',
                  border: '1px solid rgba(239, 68, 68, 0.2)',
                  color: '#f87171',
                  borderRadius: '8px',
                  padding: '7px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                }}
              >
                <LogOut size={16} />
              </button>
            </div>
          ) : (
            <button
              onClick={onOpenAuth}
              className="btn btn-primary"
              style={{ padding: '8px 16px', fontSize: '0.85rem' }}
            >
              <User size={16} />
              Login / Register
            </button>
          )}
        </div>
      </div>

      {/* Gateway URL Configuration Modal */}
      {showConfigModal && (
        <div className="modal-overlay" onClick={() => setShowConfigModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '480px', padding: '24px' }}>
            <h3 style={{ marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Server size={20} color="#6366f1" /> API Gateway Configuration
            </h3>
            <p style={{ color: '#94a3b8', fontSize: '0.85rem', marginBottom: '20px' }}>
              All requests are routed through Spring Cloud Gateway on port 8080 to auth, classroom, assessment, notification, and analytics microservices.
            </p>

            <form onSubmit={handleSaveBaseUrl}>
              <div className="input-group">
                <label className="input-label">Base Gateway URL</label>
                <input
                  type="text"
                  className="input-field"
                  value={tempUrl}
                  onChange={(e) => setTempUrl(e.target.value)}
                  placeholder="http://localhost:8080"
                  required
                />
              </div>

              <div style={{
                background: 'rgba(15, 23, 42, 0.6)',
                padding: '12px',
                borderRadius: '8px',
                marginBottom: '20px',
                fontSize: '0.82rem',
                border: '1px solid rgba(255, 255, 255, 0.05)',
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                  {gatewayStatus === 'healthy' ? (
                    <><CheckCircle2 size={16} color="#10b981" /><span style={{ color: '#10b981', fontWeight: 600 }}>Connected to Gateway :8080</span></>
                  ) : (
                    <><AlertTriangle size={16} color="#f59e0b" /><span style={{ color: '#f59e0b', fontWeight: 600 }}>Gateway unreachable or checking</span></>
                  )}
                </div>
                <div style={{ color: '#64748b' }}>
                  Endpoint: {baseUrl}/actuator/health
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <button
                  type="button"
                  onClick={() => checkGatewayHealth()}
                  className="btn btn-secondary"
                  style={{ marginRight: 'auto' }}
                >
                  Test Connection
                </button>
                <button
                  type="button"
                  onClick={() => setShowConfigModal(false)}
                  className="btn btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                >
                  Save URL
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </header>
  );
}
