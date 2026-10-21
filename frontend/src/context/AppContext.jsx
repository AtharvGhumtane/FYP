import React, { createContext, useContext, useState, useEffect } from 'react';
import { executeApiCall, DEFAULT_BASE_URL } from '../services/api';

const AppContext = createContext();

export const DEMO_CREDENTIALS = {
  admin: {
    email: 'atharvghumtane02@gmail.com',
    password: 'AdminPass@2026',
    role: 'ROLE_ADMIN',
    name: 'System Admin'
  },
  teacher: {
    email: 'dr.smith@university.edu',
    password: 'Password123!',
    role: 'ROLE_TEACHER',
    name: 'Dr. John Smith',
    secretKey: 'TEACHER-SECRET-KEY-2026'
  },
  student: {
    email: 'grace@university.edu',
    password: 'Password123!',
    role: 'ROLE_STUDENT',
    name: 'Grace Hopper'
  }
};

export function AppProvider({ children }) {
  const [baseUrl, setBaseUrl] = useState(() => {
    return localStorage.getItem('fyp_api_base_url') || DEFAULT_BASE_URL;
  });

  const [currentUser, setCurrentUser] = useState(() => {
    try {
      const saved = localStorage.getItem('fyp_current_user');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  const [toasts, setToasts] = useState([]);
  const [callHistory, setCallHistory] = useState([]);
  const [gatewayStatus, setGatewayStatus] = useState('checking'); // 'healthy' | 'unreachable' | 'checking'
  const [activeTab, setActiveTab] = useState('platform'); // 'platform' | 'explorer' | 'runner'
  const [platformView, setPlatformView] = useState('dashboard'); // 'dashboard' | 'classes' | 'tests' | 'analytics' | 'notifications' | 'admin'
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  // Sync baseUrl to storage
  useEffect(() => {
    localStorage.setItem('fyp_api_base_url', baseUrl);
    checkGatewayHealth();
  }, [baseUrl]);

  // Sync currentUser to storage
  useEffect(() => {
    if (currentUser) {
      localStorage.setItem('fyp_current_user', JSON.stringify(currentUser));
      fetchNotifications();
    } else {
      localStorage.removeItem('fyp_current_user');
      setNotifications([]);
      setUnreadCount(0);
    }
  }, [currentUser]);

  const addToast = (type, message, duration = 4000) => {
    const id = Date.now() + Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, type, message }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, duration);
  };

  const removeToast = (id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  const checkGatewayHealth = async () => {
    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'GET',
        path: '/actuator/health'
      });
      if (res.ok) {
        setGatewayStatus('healthy');
      } else {
        setGatewayStatus('unreachable');
      }
    } catch {
      setGatewayStatus('unreachable');
    }
  };

  const loginUser = (authResponse) => {
    // Normalizes auth response from backend
    const user = {
      token: authResponse.token,
      userId: authResponse.userId,
      email: authResponse.email,
      role: authResponse.role,
      fullName: authResponse.fullName || authResponse.name || authResponse.email,
    };
    setCurrentUser(user);
    addToast('success', `Logged in as ${user.fullName} (${user.role.replace('ROLE_', '')})`);
  };

  const logout = () => {
    setCurrentUser(null);
    addToast('info', 'Logged out successfully');
  };

  const quickLoginAs = async (personaKey) => {
    const creds = DEMO_CREDENTIALS[personaKey];
    if (!creds) return;

    addToast('info', `Attempting login for ${creds.email}...`);

    const res = await executeApiCall({
      baseUrl,
      method: 'POST',
      path: '/api/auth/login',
      body: { email: creds.email, password: creds.password }
    });

    if (res.ok && res.data?.token) {
      loginUser(res.data);
    } else {
      addToast('error', `Login failed (${res.status}): ${res.data?.message || res.error || 'Check backend status'}`);
    }
  };

  const fetchNotifications = async () => {
    if (!currentUser?.token) return;
    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'GET',
        path: '/api/notifications',
        token: currentUser.token
      });
      if (res.ok && Array.isArray(res.data)) {
        setNotifications(res.data);
        setUnreadCount(res.data.filter(n => !n.isRead).length);
      }
    } catch {
      // silently handle background poll error
    }
  };

  const recordApiCall = (entry) => {
    setCallHistory((prev) => [entry, ...prev.slice(0, 49)]);
  };

  return (
    <AppContext.Provider
      value={{
        baseUrl,
        setBaseUrl,
        currentUser,
        setCurrentUser,
        loginUser,
        logout,
        quickLoginAs,
        toasts,
        addToast,
        removeToast,
        callHistory,
        recordApiCall,
        gatewayStatus,
        checkGatewayHealth,
        activeTab,
        setActiveTab,
        platformView,
        setPlatformView,
        notifications,
        unreadCount,
        fetchNotifications
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
}
