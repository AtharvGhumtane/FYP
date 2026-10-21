import React from 'react';
import { useApp } from '../context/AppContext';
import {
  Sparkles,
  Server,
  Terminal,
  PlayCircle,
  BookOpen,
  FileText,
  Brain,
  Shield,
  GraduationCap,
  ArrowRight,
  CheckCircle2,
  Cpu,
  Database,
  Layers,
  Zap
} from 'lucide-react';

export default function DashboardView({ onNavigate, onOpenAuth }) {
  const { currentUser, gatewayStatus, quickLoginAs, setActiveTab } = useApp();

  const isTeacher = currentUser?.role === 'ROLE_TEACHER';
  const isStudent = currentUser?.role === 'ROLE_STUDENT';
  const isAdmin = currentUser?.role === 'ROLE_ADMIN';

  return (
    <div style={{ maxWidth: '1400px', margin: '0 auto', padding: '32px 24px' }}>
      {/* Hero Welcome Banner */}
      <div className="glass-panel glow-card" style={{ padding: '36px', marginBottom: '32px', position: 'relative' }}>
        <div style={{ maxWidth: '720px' }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '4px 12px',
            borderRadius: '20px',
            background: 'rgba(99, 102, 241, 0.15)',
            border: '1px solid rgba(99, 102, 241, 0.3)',
            color: '#818cf8',
            fontSize: '0.8rem',
            fontWeight: 700,
            marginBottom: '16px',
          }}>
            <Sparkles size={14} />
            <span>AI-Powered Smart Examination & Learning Analytics</span>
          </div>

          <h1 style={{ fontSize: '2.4rem', lineHeight: 1.2, marginBottom: '14px' }}>
            {currentUser ? `Welcome back, ${currentUser.fullName}` : 'Microservices Examination & AI Analytics Hub'}
          </h1>

          <p style={{ color: '#94a3b8', fontSize: '1rem', lineHeight: 1.6, marginBottom: '24px' }}>
            Comprehensive unified interface for all 5 Spring Boot microservices, featuring real-time proctored examinations, automated grading, autosave resilience, and Google Gemini neural diagnostics.
          </p>

          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
            {isTeacher && (
              <button onClick={() => onNavigate('tests')} className="btn btn-primary btn-lg">
                <FileText size={18} />
                <span>Create / Manage Assessments</span>
              </button>
            )}

            {isStudent && (
              <button onClick={() => onNavigate('tests')} className="btn btn-primary btn-lg">
                <PlayCircle size={18} />
                <span>Take Live Examination</span>
              </button>
            )}

            {isAdmin && (
              <button onClick={() => onNavigate('admin')} className="btn btn-primary btn-lg">
                <Shield size={18} />
                <span>Teacher Key Control Center</span>
              </button>
            )}

            {!currentUser && (
              <button onClick={onOpenAuth} className="btn btn-primary btn-lg">
                <span>Sign In / Register</span>
                <ArrowRight size={18} />
              </button>
            )}

            <button onClick={() => setActiveTab('explorer')} className="btn btn-secondary btn-lg">
              <Terminal size={18} />
              <span>Open API Explorer</span>
            </button>

            <button onClick={() => setActiveTab('runner')} className="btn btn-accent btn-lg">
              <Zap size={18} />
              <span>Run Automated E2E Suite</span>
            </button>
          </div>
        </div>
      </div>

      {/* Quick Role Switcher Bar */}
      <div className="glass-panel" style={{ padding: '20px 24px', marginBottom: '32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <div style={{ fontWeight: 700, fontSize: '0.95rem', color: '#f8fafc', marginBottom: '4px' }}>
              Instant Demo Persona Testing
            </div>
            <div style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
              Switch instantly between pre-configured personas without retyping credentials:
            </div>
          </div>

          <div style={{ display: 'flex', gap: '10px' }}>
            <button onClick={() => quickLoginAs('admin')} className="btn btn-secondary">
              <Shield size={16} color="#ec4899" />
              <span>👑 Seeded Admin</span>
            </button>
            <button onClick={() => quickLoginAs('teacher')} className="btn btn-secondary">
              <BookOpen size={16} color="#818cf8" />
              <span>👨‍🏫 Dr. Smith (Teacher)</span>
            </button>
            <button onClick={() => quickLoginAs('student')} className="btn btn-secondary">
              <GraduationCap size={16} color="#22d3ee" />
              <span>🎓 Grace Hopper (Student)</span>
            </button>
          </div>
        </div>
      </div>

      {/* Microservices Topology Architecture Grid */}
      <h3 style={{ fontSize: '1.25rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
        <Layers size={20} color="#6366f1" /> System Topology & Service Status
      </h3>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px', marginBottom: '36px' }}>
        {[
          {
            title: 'API Gateway',
            port: ':8080',
            desc: 'Spring Cloud Gateway, CORS, JWT routing filter & Actuator',
            icon: Server,
            color: '#6366f1',
            status: gatewayStatus === 'healthy' ? 'ONLINE' : 'CONNECTING',
          },
          {
            title: 'Auth Service',
            port: ':8081',
            desc: 'JWT security, BCrypt teacher secret registry, Google OAuth',
            icon: Shield,
            color: '#ec4899',
            status: 'ROUTED',
          },
          {
            title: 'Classroom Service',
            port: ':8082',
            desc: 'Course catalogs, join request approvals, roster management',
            icon: BookOpen,
            color: '#3b82f6',
            status: 'ROUTED',
          },
          {
            title: 'Assessment Service',
            port: ':8083',
            desc: 'MCQ test authoring, auto-grading, live autosave, proctoring',
            icon: FileText,
            color: '#10b981',
            status: 'ROUTED',
          },
          {
            title: 'Notification Service',
            port: ':8084',
            desc: 'In-app event notifications for tests and graded scorecards',
            icon: Zap,
            color: '#f59e0b',
            status: 'ROUTED',
          },
          {
            title: 'Analytics & Gemini AI',
            port: ':8085',
            desc: 'Database performance telemetry & Google Gemini learning reports',
            icon: Brain,
            color: '#a855f7',
            status: 'ROUTED',
          },
        ].map(srv => {
          const Icon = srv.icon;
          return (
            <div key={srv.title} className="glass-panel" style={{ padding: '22px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
                <div style={{
                  width: '36px',
                  height: '36px',
                  borderRadius: '10px',
                  background: `${srv.color}22`,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: srv.color,
                }}>
                  <Icon size={20} />
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.75rem', color: '#94a3b8' }}>
                    {srv.port}
                  </span>
                  <span className="badge badge-method-get" style={{ fontSize: '0.65rem', padding: '2px 6px' }}>
                    {srv.status}
                  </span>
                </div>
              </div>

              <div style={{ fontWeight: 700, fontSize: '1rem', color: '#f8fafc', marginBottom: '4px' }}>
                {srv.title}
              </div>

              <p style={{ color: '#94a3b8', fontSize: '0.82rem', margin: 0, lineHeight: 1.4 }}>
                {srv.desc}
              </p>
            </div>
          );
        })}
      </div>
    </div>
  );
}
