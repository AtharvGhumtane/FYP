import React, { useState } from 'react';
import { useApp } from './context/AppContext';
import Navbar from './components/Navbar';
import Toast from './components/Toast';
import AuthModal from './components/AuthModal';
import DashboardView from './components/DashboardView';
import ClassroomView from './components/ClassroomView';
import AssessmentView from './components/AssessmentView';
import AnalyticsView from './components/AnalyticsView';
import NotificationView from './components/NotificationView';
import AdminView from './components/AdminView';
import ApiExplorer from './components/ApiExplorer';
import E2ERunner from './components/E2ERunner';

import {
  LayoutDashboard,
  BookOpen,
  FileText,
  BarChart3,
  Bell,
  Shield
} from 'lucide-react';

export default function App() {
  const {
    activeTab,
    setActiveTab,
    platformView,
    setPlatformView,
    currentUser,
    unreadCount
  } = useApp();

  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [preselectedClassForTests, setPreselectedClassForTests] = useState(null);

  const handleSelectClassForTests = (cls) => {
    setPreselectedClassForTests(cls);
    setPlatformView('tests');
  };

  const navItems = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'classes', label: 'Classrooms', icon: BookOpen },
    { id: 'tests', label: 'Assessments', icon: FileText },
    { id: 'analytics', label: 'AI Analytics', icon: BarChart3 },
    { id: 'notifications', label: 'Notifications', icon: Bell, badge: unreadCount },
    ...(currentUser?.role === 'ROLE_ADMIN'
      ? [{ id: 'admin', label: 'Admin Keys', icon: Shield }]
      : [])
  ];

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Top Navbar */}
      <Navbar onOpenAuth={() => setAuthModalOpen(true)} />

      {/* Secondary Subnav for Platform UI Mode */}
      {activeTab === 'platform' && (
        <div style={{
          background: 'rgba(15, 23, 42, 0.6)',
          borderBottom: '1px solid rgba(255, 255, 255, 0.06)',
          padding: '0 24px',
        }}>
          <div style={{
            maxWidth: '1400px',
            margin: '0 auto',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            overflowX: 'auto',
          }}>
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = platformView === item.id;

              return (
                <button
                  key={item.id}
                  onClick={() => setPlatformView(item.id)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    padding: '12px 16px',
                    background: 'transparent',
                    border: 'none',
                    borderBottom: '2px solid',
                    borderColor: isActive ? '#6366f1' : 'transparent',
                    color: isActive ? '#f8fafc' : '#94a3b8',
                    fontWeight: isActive ? 600 : 500,
                    fontSize: '0.88rem',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                    whiteSpace: 'nowrap',
                  }}
                >
                  <Icon size={16} color={isActive ? '#818cf8' : '#64748b'} />
                  <span>{item.label}</span>
                  {item.badge > 0 && (
                    <span style={{
                      background: '#ef4444',
                      color: '#fff',
                      fontSize: '0.65rem',
                      fontWeight: 700,
                      padding: '1px 6px',
                      borderRadius: '10px',
                    }}>
                      {item.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Main View Area */}
      <main style={{ flex: 1 }}>
        {activeTab === 'platform' && (
          <>
            {platformView === 'dashboard' && (
              <DashboardView
                onNavigate={(view) => setPlatformView(view)}
                onOpenAuth={() => setAuthModalOpen(true)}
              />
            )}
            {platformView === 'classes' && (
              <ClassroomView onSelectClassForTests={handleSelectClassForTests} />
            )}
            {platformView === 'tests' && (
              <AssessmentView preselectedClass={preselectedClassForTests} />
            )}
            {platformView === 'analytics' && <AnalyticsView />}
            {platformView === 'notifications' && <NotificationView />}
            {platformView === 'admin' && <AdminView />}
          </>
        )}

        {activeTab === 'explorer' && <ApiExplorer />}
        {activeTab === 'runner' && <E2ERunner />}
      </main>

      {/* Auth Modal */}
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
      />

      {/* Toast Notification Container */}
      <Toast />
    </div>
  );
}
