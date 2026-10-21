import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { executeApiCall } from '../services/api';
import {
  Bell,
  CheckCircle,
  CheckCheck,
  RefreshCw,
  Mail,
  Clock,
  Sparkles,
  Inbox
} from 'lucide-react';

export default function NotificationView() {
  const { baseUrl, currentUser, notifications, unreadCount, fetchNotifications, addToast } = useApp();
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleMarkAllRead = async () => {
    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: '/api/notifications/mark-all-read',
        token: currentUser?.token,
      });

      if (res.ok) {
        addToast('success', 'All notifications marked as read');
        fetchNotifications();
      } else {
        addToast('error', `Failed to mark read: ${res.data?.message || res.error}`);
      }
    } catch (err) {
      addToast('error', `Error: ${err.message}`);
    }
  };

  const handleMarkSingleRead = async (id) => {
    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'PATCH',
        path: `/api/notifications/${id}/read`,
        token: currentUser?.token,
      });

      if (res.ok) {
        fetchNotifications();
      }
    } catch (err) {
      console.error(err);
    }
  };

  const displayedNotifications = unreadOnly
    ? notifications.filter(n => !n.isRead)
    : notifications;

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto', padding: '32px 24px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Bell size={24} color="#6366f1" />
            <h2 style={{ fontSize: '1.5rem', margin: 0 }}>Notification Feed</h2>
          </div>
          <p style={{ color: '#94a3b8', fontSize: '0.88rem', marginTop: '4px' }}>
            Alerts for scheduled examinations, graded evaluations, and classroom enrollments.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={() => setUnreadOnly(!unreadOnly)}
            className={`btn btn-sm ${unreadOnly ? 'btn-primary' : 'btn-secondary'}`}
          >
            {unreadOnly ? 'Showing Unread Only' : 'Show All'}
          </button>

          <button
            onClick={fetchNotifications}
            className="btn btn-secondary btn-sm"
            title="Poll for new alerts"
          >
            <RefreshCw size={14} />
            Poll
          </button>

          {unreadCount > 0 && (
            <button
              onClick={handleMarkAllRead}
              className="btn btn-secondary btn-sm"
              style={{ color: '#10b981', borderColor: 'rgba(16, 185, 129, 0.3)' }}
            >
              <CheckCheck size={14} />
              Mark All Read
            </button>
          )}
        </div>
      </div>

      {/* Notifications list */}
      <div className="glass-panel" style={{ overflow: 'hidden' }}>
        {displayedNotifications.length === 0 ? (
          <div style={{ padding: '60px 20px', textAlign: 'center', color: '#64748b' }}>
            <Inbox size={42} style={{ margin: '0 auto 12px', opacity: 0.4 }} />
            <h4>No notifications found</h4>
            <p style={{ fontSize: '0.85rem', marginTop: '4px' }}>
              You are all caught up! Scheduled tests and evaluations will appear here.
            </p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            {displayedNotifications.map((notif) => (
              <div
                key={notif.id}
                style={{
                  padding: '18px 24px',
                  display: 'flex',
                  alignItems: 'flex-start',
                  justifyContent: 'space-between',
                  gap: '16px',
                  borderBottom: '1px solid rgba(255, 255, 255, 0.04)',
                  background: notif.isRead ? 'transparent' : 'rgba(99, 102, 241, 0.07)',
                }}
              >
                <div style={{ display: 'flex', gap: '14px', alignItems: 'flex-start' }}>
                  <div style={{
                    width: '36px',
                    height: '36px',
                    borderRadius: '8px',
                    background: notif.isRead ? 'rgba(255, 255, 255, 0.05)' : 'rgba(99, 102, 241, 0.2)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: notif.isRead ? '#94a3b8' : '#818cf8',
                    marginTop: '2px',
                  }}>
                    <Bell size={18} />
                  </div>

                  <div>
                    <div style={{
                      fontWeight: notif.isRead ? 500 : 700,
                      color: notif.isRead ? '#cbd5e1' : '#f8fafc',
                      fontSize: '0.92rem',
                      lineHeight: 1.4,
                      marginBottom: '4px',
                    }}>
                      {notif.message || notif.title || 'Platform Notification'}
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.75rem', color: '#64748b' }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Clock size={12} />
                        {notif.createdAt ? new Date(notif.createdAt).toLocaleString() : 'Just now'}
                      </span>
                      {notif.type && (
                        <span className="badge badge-method-get" style={{ padding: '1px 6px', fontSize: '0.65rem' }}>
                          {notif.type}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                {!notif.isRead && (
                  <button
                    onClick={() => handleMarkSingleRead(notif.id)}
                    className="btn btn-secondary btn-sm"
                    style={{ padding: '4px 8px', fontSize: '0.72rem' }}
                    title="Mark as read"
                  >
                    <CheckCircle size={13} />
                    Mark Read
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
