import React, { useState, useEffect } from 'react';
import { useApp } from '../context/AppContext';
import { executeApiCall } from '../services/api';
import {
  Shield,
  Key,
  Copy,
  Check,
  Plus,
  RefreshCw,
  CheckCircle,
  XCircle,
  ToggleLeft,
  ToggleRight,
  AlertTriangle
} from 'lucide-react';

export default function AdminView() {
  const { baseUrl, currentUser, addToast } = useApp();

  const [keys, setKeys] = useState([]);
  const [loading, setLoading] = useState(false);
  const [copiedKey, setCopiedKey] = useState(null);

  // New key form
  const [teacherEmail, setTeacherEmail] = useState('');
  const [customSecret, setCustomSecret] = useState('');
  const [generatedKeyResult, setGeneratedKeyResult] = useState(null);
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    if (currentUser?.role === 'ROLE_ADMIN') {
      fetchKeys();
    }
  }, [currentUser]);

  const fetchKeys = async () => {
    setLoading(true);
    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'GET',
        path: '/api/admin/teacher-keys',
        token: currentUser?.token,
      });

      if (res.ok && Array.isArray(res.data)) {
        setKeys(res.data);
      } else {
        addToast('error', `Failed to load keys: ${res.data?.message || res.error}`);
      }
    } catch (err) {
      addToast('error', `Error: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateKey = async (e) => {
    e.preventDefault();
    setCreating(true);
    setGeneratedKeyResult(null);

    try {
      const body = {
        teacherEmail,
        ...(customSecret.trim() ? { secretKey: customSecret.trim() } : {})
      };

      const res = await executeApiCall({
        baseUrl,
        method: 'POST',
        path: '/api/admin/teacher-keys',
        token: currentUser?.token,
        body,
      });

      if (res.ok && res.data) {
        setGeneratedKeyResult(res.data);
        addToast('success', 'Teacher secret key generated successfully!');
        setTeacherEmail('');
        setCustomSecret('');
        fetchKeys();
      } else {
        addToast('error', `Generation failed: ${res.data?.message || res.error}`);
      }
    } catch (err) {
      addToast('error', `Error: ${err.message}`);
    } finally {
      setCreating(false);
    }
  };

  const handleToggleStatus = async (keyId, currentStatus) => {
    try {
      const res = await executeApiCall({
        baseUrl,
        method: 'PATCH',
        path: `/api/admin/teacher-keys/${keyId}/status`,
        token: currentUser?.token,
        body: { isActive: !currentStatus },
      });

      if (res.ok) {
        addToast('success', `Key status updated to ${!currentStatus ? 'Active' : 'Revoked'}`);
        fetchKeys();
      } else {
        addToast('error', `Toggle failed: ${res.data?.message || res.error}`);
      }
    } catch (err) {
      addToast('error', `Error: ${err.message}`);
    }
  };

  const copyKey = (text) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(text);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  if (currentUser?.role !== 'ROLE_ADMIN') {
    return (
      <div style={{ maxWidth: '800px', margin: '40px auto', textAlign: 'center' }} className="glass-panel">
        <div style={{ padding: '40px' }}>
          <AlertTriangle size={48} color="#f59e0b" style={{ margin: '0 auto 16px' }} />
          <h3>Admin Authorization Required</h3>
          <p style={{ color: '#94a3b8', margin: '12px 0 24px' }}>
            This section allows system administrators to issue and manage teacher registration keys.
          </p>
          <span style={{ fontSize: '0.85rem', color: '#64748b' }}>
            Use the top right <strong>👑 Admin</strong> button to sign in as Admin.
          </span>
        </div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '32px 24px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Shield size={24} color="#ec4899" />
            <h2 style={{ fontSize: '1.5rem', margin: 0 }}>Teacher Authorization Key Registry</h2>
          </div>
          <p style={{ color: '#94a3b8', fontSize: '0.88rem', marginTop: '4px' }}>
            Issue BCrypt-hashed security keys required for new educators to register.
          </p>
        </div>

        <button onClick={fetchKeys} className="btn btn-secondary btn-sm" disabled={loading}>
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          Refresh
        </button>
      </div>

      {/* Key Generation Form */}
      <div className="glass-panel glow-card" style={{ padding: '24px', marginBottom: '28px' }}>
        <h3 style={{ fontSize: '1.1rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Plus size={18} color="#6366f1" /> Generate New Teacher Key
        </h3>

        <form onSubmit={handleGenerateKey} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr auto', gap: '16px', alignItems: 'flex-end' }}>
          <div className="input-group" style={{ margin: 0 }}>
            <label className="input-label">Teacher Email Address</label>
            <input
              type="email"
              className="input-field"
              placeholder="e.g. dr.smith@university.edu"
              value={teacherEmail}
              onChange={(e) => setTeacherEmail(e.target.value)}
              required
            />
          </div>

          <div className="input-group" style={{ margin: 0 }}>
            <label className="input-label">Custom Secret (Optional, or auto-generate)</label>
            <input
              type="text"
              className="input-field"
              placeholder="Leave blank to auto-generate"
              value={customSecret}
              onChange={(e) => setCustomSecret(e.target.value)}
            />
          </div>

          <button type="submit" disabled={creating} className="btn btn-primary" style={{ height: '42px' }}>
            {creating ? 'Generating...' : (
              <>
                <Key size={16} />
                <span>Issue Key</span>
              </>
            )}
          </button>
        </form>

        {/* Once generated plaintext banner */}
        {generatedKeyResult && (
          <div style={{
            marginTop: '20px',
            padding: '16px',
            borderRadius: '10px',
            background: 'rgba(16, 185, 129, 0.12)',
            border: '1px solid rgba(16, 185, 129, 0.3)',
            animation: 'fadeIn 0.25s',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
              <span style={{ color: '#10b981', fontWeight: 700, fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <CheckCircle size={16} />
                Key Issued (Plaintext visible once):
              </span>
              <button
                onClick={() => copyKey(generatedKeyResult.rawSecretKey)}
                className="btn btn-secondary btn-sm"
                style={{ padding: '3px 8px', fontSize: '0.75rem' }}
              >
                {copiedKey === generatedKeyResult.rawSecretKey ? <Check size={12} color="#10b981" /> : <Copy size={12} />}
                {copiedKey === generatedKeyResult.rawSecretKey ? 'Copied' : 'Copy Key'}
              </button>
            </div>
            <div style={{
              fontFamily: 'var(--font-mono)',
              fontSize: '1rem',
              fontWeight: 700,
              color: '#f8fafc',
              background: 'rgba(0, 0, 0, 0.3)',
              padding: '8px 12px',
              borderRadius: '6px',
              wordBreak: 'break-all',
            }}>
              {generatedKeyResult.rawSecretKey}
            </div>
            <div style={{ color: '#94a3b8', fontSize: '0.78rem', marginTop: '6px' }}>
              Provide this key to <strong>{generatedKeyResult.teacherEmail}</strong> during their registration.
            </div>
          </div>
        )}
      </div>

      {/* Keys Table */}
      <div className="glass-panel" style={{ overflow: 'hidden' }}>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border-subtle)', fontWeight: 600, fontSize: '0.9rem' }}>
          Registered Authorization Keys ({keys.length})
        </div>

        {keys.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: '#64748b', fontSize: '0.88rem' }}>
            No teacher keys registered yet. Issue one above to allow teachers to register.
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
              <thead>
                <tr style={{ background: 'rgba(15, 23, 42, 0.7)', borderBottom: '1px solid var(--border-subtle)' }}>
                  <th style={{ padding: '12px 20px', color: '#94a3b8', fontWeight: 600 }}>Teacher Email</th>
                  <th style={{ padding: '12px 20px', color: '#94a3b8', fontWeight: 600 }}>Key ID</th>
                  <th style={{ padding: '12px 20px', color: '#94a3b8', fontWeight: 600 }}>Status</th>
                  <th style={{ padding: '12px 20px', color: '#94a3b8', fontWeight: 600 }}>Created By</th>
                  <th style={{ padding: '12px 20px', color: '#94a3b8', fontWeight: 600, textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {keys.map((k) => (
                  <tr key={k.id} style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.04)' }}>
                    <td style={{ padding: '14px 20px', fontWeight: 600, color: '#f8fafc' }}>
                      {k.teacherEmail}
                    </td>
                    <td style={{ padding: '14px 20px', fontFamily: 'var(--font-mono)', fontSize: '0.78rem', color: '#818cf8' }}>
                      {k.id}
                    </td>
                    <td style={{ padding: '14px 20px' }}>
                      <span className={`badge ${k.isActive ? 'badge-status-accepted' : 'badge-status-rejected'}`}>
                        {k.isActive ? 'Active' : 'Revoked'}
                      </span>
                    </td>
                    <td style={{ padding: '14px 20px', color: '#94a3b8' }}>
                      {k.createdBy || 'Admin'}
                    </td>
                    <td style={{ padding: '14px 20px', textAlign: 'right' }}>
                      <button
                        onClick={() => handleToggleStatus(k.id, k.isActive)}
                        className={`btn btn-sm ${k.isActive ? 'btn-danger' : 'btn-secondary'}`}
                        style={{ padding: '4px 10px', fontSize: '0.75rem' }}
                      >
                        {k.isActive ? 'Revoke Key' : 'Activate Key'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
