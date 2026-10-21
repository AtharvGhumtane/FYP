import React, { useState, useEffect } from 'react';
import { useApp } from '../context/AppContext';
import { ENDPOINT_GROUPS } from '../services/endpoints';
import { executeApiCall } from '../services/api';
import {
  Send,
  Copy,
  Check,
  RotateCcw,
  Clock,
  Code2,
  ListFilter,
  ShieldAlert,
  Search,
  ExternalLink,
  ChevronRight
} from 'lucide-react';

export default function ApiExplorer() {
  const { baseUrl, currentUser, addToast, recordApiCall } = useApp();

  const [selectedEndpoint, setSelectedEndpoint] = useState(ENDPOINT_GROUPS[0].endpoints[0]);
  const [methodFilter, setMethodFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Request state
  const [params, setParams] = useState({});
  const [reqBody, setReqBody] = useState('');
  const [useAuth, setUseAuth] = useState(true);
  const [customToken, setCustomToken] = useState('');
  const [isSending, setIsSending] = useState(false);

  // Response state
  const [response, setResponse] = useState(null);
  const [copied, setCopied] = useState(false);
  const [responseTab, setResponseTab] = useState('body'); // 'body' | 'headers'

  // When endpoint changes, reset params and body to default sample
  useEffect(() => {
    if (!selectedEndpoint) return;
    setParams({});
    setReqBody(selectedEndpoint.sampleBody ? JSON.stringify(selectedEndpoint.sampleBody, null, 2) : '');
    setUseAuth(selectedEndpoint.requiresAuth !== false);
    setResponse(null);
  }, [selectedEndpoint]);

  // Extract params needed by path
  const pathParams = (selectedEndpoint?.path.match(/\{([^}]+)\}/g) || []).map(p => p.slice(1, -1));

  // Compute resolved path
  let resolvedPath = selectedEndpoint ? selectedEndpoint.path : '';
  pathParams.forEach(param => {
    const val = params[param] || `{${param}}`;
    resolvedPath = resolvedPath.replace(`{${param}}`, val);
  });

  const handleSend = async () => {
    if (!selectedEndpoint) return;

    // Check if missing parameters
    for (const p of pathParams) {
      if (!params[p]) {
        addToast('error', `Please provide parameter: {${p}}`);
        return;
      }
    }

    let parsedBody = null;
    if (reqBody.trim() && ['POST', 'PUT', 'PATCH'].includes(selectedEndpoint.method)) {
      try {
        parsedBody = JSON.parse(reqBody);
      } catch (err) {
        addToast('error', `Invalid JSON body: ${err.message}`);
        return;
      }
    }

    setIsSending(true);
    const tokenToUse = useAuth ? (customToken.trim() || currentUser?.token || null) : null;

    try {
      const res = await executeApiCall({
        baseUrl,
        method: selectedEndpoint.method,
        path: resolvedPath,
        body: parsedBody,
        token: tokenToUse,
      });

      setResponse(res);
      recordApiCall({
        timestamp: new Date().toLocaleTimeString(),
        endpointName: selectedEndpoint.name,
        method: res.method,
        url: res.url,
        status: res.status,
        durationMs: res.durationMs,
      });

      if (res.ok) {
        addToast('success', `${selectedEndpoint.name} succeeded (${res.status}) in ${res.durationMs}ms`);
      } else {
        addToast('error', `HTTP ${res.status}: ${res.data?.message || res.error || 'Request returned an error'}`);
      }
    } catch (err) {
      addToast('error', `Execution failed: ${err.message}`);
    } finally {
      setIsSending(false);
    }
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(typeof text === 'object' ? JSON.stringify(text, null, 2) : text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const getStatusColor = (status) => {
    if (!status) return '#64748b';
    if (status >= 200 && status < 300) return '#10b981';
    if (status >= 300 && status < 400) return '#3b82f6';
    if (status >= 400 && status < 500) return '#f59e0b';
    return '#ef4444';
  };

  // Filtered list
  const filteredGroups = ENDPOINT_GROUPS.map(grp => ({
    ...grp,
    endpoints: grp.endpoints.filter(ep => {
      const matchesMethod = methodFilter === 'ALL' || ep.method === methodFilter;
      const matchesSearch = !searchQuery || 
        ep.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        ep.path.toLowerCase().includes(searchQuery.toLowerCase()) ||
        ep.description.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesMethod && matchesSearch;
    })
  })).filter(grp => grp.endpoints.length > 0);

  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: '360px 1fr',
      gap: '24px',
      maxWidth: '1600px',
      margin: '0 auto',
      padding: '24px',
      minHeight: 'calc(100vh - 70px)',
    }}>
      {/* Left Sidebar: Endpoints list */}
      <div className="glass-panel" style={{
        display: 'flex',
        flexDirection: 'column',
        height: 'calc(100vh - 118px)',
        overflow: 'hidden',
      }}>
        {/* Search & Filter Header */}
        <div style={{ padding: '16px', borderBottom: '1px solid var(--border-subtle)' }}>
          <div style={{ position: 'relative', marginBottom: '12px' }}>
            <Search size={16} color="#64748b" style={{ position: 'absolute', left: '12px', top: '12px' }} />
            <input
              type="text"
              placeholder="Search all endpoints..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="input-field"
              style={{ paddingLeft: '36px', height: '38px', fontSize: '0.85rem' }}
            />
          </div>

          <div style={{ display: 'flex', gap: '4px', overflowX: 'auto', paddingBottom: '4px' }}>
            {['ALL', 'GET', 'POST', 'PUT', 'PATCH', 'DELETE'].map(m => (
              <button
                key={m}
                onClick={() => setMethodFilter(m)}
                style={{
                  padding: '4px 10px',
                  borderRadius: '6px',
                  border: 'none',
                  fontSize: '0.72rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  background: methodFilter === m ? 'rgba(99, 102, 241, 0.3)' : 'rgba(255, 255, 255, 0.05)',
                  color: methodFilter === m ? '#818cf8' : '#94a3b8',
                }}
              >
                {m}
              </button>
            ))}
          </div>
        </div>

        {/* Scrollable Endpoints Directory */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '12px' }}>
          {filteredGroups.map(grp => (
            <div key={grp.group} style={{ marginBottom: '16px' }}>
              <div style={{
                fontSize: '0.75rem',
                fontWeight: 700,
                color: '#818cf8',
                textTransform: 'uppercase',
                letterSpacing: '0.05em',
                padding: '6px 8px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}>
                <span>{grp.group}</span>
                <span style={{ color: '#64748b', fontSize: '0.7rem' }}>{grp.endpoints.length}</span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                {grp.endpoints.map(ep => {
                  const isSelected = selectedEndpoint?.id === ep.id;
                  return (
                    <button
                      key={ep.id}
                      onClick={() => setSelectedEndpoint(ep)}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '10px',
                        padding: '8px 10px',
                        borderRadius: '8px',
                        border: '1px solid',
                        borderColor: isSelected ? 'rgba(99, 102, 241, 0.5)' : 'transparent',
                        background: isSelected ? 'rgba(99, 102, 241, 0.15)' : 'transparent',
                        color: isSelected ? '#f8fafc' : '#94a3b8',
                        cursor: 'pointer',
                        textAlign: 'left',
                        transition: 'all 0.15s ease',
                        width: '100%',
                      }}
                    >
                      <span className={`badge badge-method-${ep.method.toLowerCase()}`} style={{ padding: '2px 6px', fontSize: '0.65rem' }}>
                        {ep.method}
                      </span>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{
                          fontSize: '0.82rem',
                          fontWeight: 600,
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                          color: isSelected ? '#fff' : '#cbd5e1'
                        }}>
                          {ep.name}
                        </div>
                        <div style={{
                          fontSize: '0.7rem',
                          fontFamily: 'var(--font-mono)',
                          color: '#64748b',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap'
                        }}>
                          {ep.path}
                        </div>
                      </div>
                      {isSelected && <ChevronRight size={14} color="#818cf8" />}
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Right Main Panel: Request & Response Playground */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        {/* Endpoint Banner & URL Runner */}
        <div className="glass-panel glow-card" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '16px', marginBottom: '16px' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
                <span className={`badge badge-method-${selectedEndpoint.method.toLowerCase()}`} style={{ fontSize: '0.8rem', padding: '4px 10px' }}>
                  {selectedEndpoint.method}
                </span>
                <h2 style={{ fontSize: '1.25rem', margin: 0 }}>{selectedEndpoint.name}</h2>
              </div>
              <p style={{ color: '#94a3b8', fontSize: '0.88rem', margin: 0 }}>
                {selectedEndpoint.description}
              </p>
            </div>

            {selectedEndpoint.allowedRoles && (
              <div style={{ display: 'flex', gap: '6px' }}>
                {selectedEndpoint.allowedRoles.map(role => (
                  <span key={role} className="badge badge-role-admin" style={{ fontSize: '0.7rem' }}>
                    {role.replace('ROLE_', '')}
                  </span>
                ))}
              </div>
            )}
          </div>

          {/* Interactive URL Bar */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            background: 'rgba(15, 23, 42, 0.9)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '10px',
            padding: '6px 8px 6px 14px',
          }}>
            <span style={{
              fontFamily: 'var(--font-mono)',
              fontSize: '0.82rem',
              color: '#64748b',
              userSelect: 'none',
            }}>
              {baseUrl}
            </span>
            <input
              type="text"
              readOnly
              value={resolvedPath}
              style={{
                flex: 1,
                background: 'transparent',
                border: 'none',
                color: '#38bdf8',
                fontFamily: 'var(--font-mono)',
                fontSize: '0.9rem',
                fontWeight: 600,
                outline: 'none',
              }}
            />
            <button
              onClick={handleSend}
              disabled={isSending}
              className="btn btn-primary"
              style={{ minWidth: '120px' }}
            >
              {isSending ? (
                <span>Sending...</span>
              ) : (
                <>
                  <Send size={15} />
                  <span>Send</span>
                </>
              )}
            </button>
          </div>

          {/* Path Parameters Inputs (if any) */}
          {pathParams.length > 0 && (
            <div style={{
              marginTop: '16px',
              padding: '14px',
              borderRadius: '8px',
              background: 'rgba(255, 255, 255, 0.02)',
              border: '1px solid rgba(255, 255, 255, 0.06)'
            }}>
              <div style={{ fontSize: '0.8rem', fontWeight: 600, color: '#94a3b8', marginBottom: '8px' }}>
                Path Parameters
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '12px' }}>
                {pathParams.map(param => (
                  <div key={param}>
                    <label style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)', color: '#818cf8', display: 'block', marginBottom: '4px' }}>
                      :{param}
                    </label>
                    <input
                      type="text"
                      className="input-field"
                      placeholder={`Enter ${param}...`}
                      value={params[param] || ''}
                      onChange={(e) => setParams({ ...params, [param]: e.target.value })}
                      style={{ fontSize: '0.85rem', padding: '8px 10px' }}
                    />
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Authentication Options Bar */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginTop: '16px',
            fontSize: '0.82rem',
            color: '#94a3b8',
            flexWrap: 'wrap',
            gap: '12px'
          }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={useAuth}
                onChange={(e) => setUseAuth(e.target.checked)}
                style={{ cursor: 'pointer' }}
              />
              <span>Include Bearer Authorization Header</span>
            </label>

            {useAuth && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1, minWidth: '280px', justifyContent: 'flex-end' }}>
                <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Token:</span>
                <input
                  type="text"
                  className="input-field"
                  placeholder={currentUser?.token ? `Active: ${currentUser.fullName} (${currentUser.role})` : 'Paste custom Bearer JWT...'}
                  value={customToken}
                  onChange={(e) => setCustomToken(e.target.value)}
                  style={{ maxWidth: '320px', padding: '6px 10px', fontSize: '0.78rem' }}
                />
              </div>
            )}
          </div>
        </div>

        {/* Request Body & Response split */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
          {/* Request Body Editor */}
          <div className="glass-panel" style={{ padding: '20px', display: 'flex', flexDirection: 'column' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
              <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Code2 size={16} color="#818cf8" />
                Request Payload (JSON)
              </span>

              <div style={{ display: 'flex', gap: '6px' }}>
                {selectedEndpoint.sampleBody && (
                  <button
                    onClick={() => setReqBody(JSON.stringify(selectedEndpoint.sampleBody, null, 2))}
                    className="btn btn-secondary btn-sm"
                    style={{ fontSize: '0.75rem', padding: '4px 8px' }}
                  >
                    <RotateCcw size={12} /> Reset Sample
                  </button>
                )}
                {reqBody && (
                  <button
                    onClick={() => setReqBody('')}
                    className="btn btn-secondary btn-sm"
                    style={{ fontSize: '0.75rem', padding: '4px 8px' }}
                  >
                    Clear
                  </button>
                )}
              </div>
            </div>

            <textarea
              value={reqBody}
              onChange={(e) => setReqBody(e.target.value)}
              placeholder={['GET', 'DELETE'].includes(selectedEndpoint.method) ? '(No request body for GET/DELETE)' : 'Enter JSON payload...'}
              disabled={['GET', 'DELETE'].includes(selectedEndpoint.method)}
              style={{
                flex: 1,
                minHeight: '260px',
                background: 'rgba(15, 23, 42, 0.95)',
                border: '1px solid rgba(255, 255, 255, 0.08)',
                borderRadius: '8px',
                padding: '12px',
                color: '#f8fafc',
                fontFamily: 'var(--font-mono)',
                fontSize: '0.82rem',
                lineHeight: 1.5,
                resize: 'vertical',
                outline: 'none',
              }}
            />
          </div>

          {/* Response Viewer */}
          <div className="glass-panel" style={{ padding: '20px', display: 'flex', flexDirection: 'column' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#f8fafc' }}>
                  Response
                </span>
                {response && (
                  <span
                    style={{
                      background: `${getStatusColor(response.status)}22`,
                      color: getStatusColor(response.status),
                      border: `1px solid ${getStatusColor(response.status)}55`,
                      padding: '2px 8px',
                      borderRadius: '12px',
                      fontSize: '0.75rem',
                      fontWeight: 700,
                    }}
                  >
                    {response.status} {response.statusText}
                  </span>
                )}
              </div>

              {response && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <span style={{ fontSize: '0.75rem', color: '#94a3b8', display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <Clock size={12} /> {response.durationMs}ms
                  </span>
                  <button
                    onClick={() => copyToClipboard(responseTab === 'body' ? response.data : response.headers)}
                    className="btn btn-secondary btn-sm"
                    style={{ fontSize: '0.75rem', padding: '4px 8px' }}
                  >
                    {copied ? <Check size={12} color="#10b981" /> : <Copy size={12} />}
                    {copied ? 'Copied' : 'Copy'}
                  </button>
                </div>
              )}
            </div>

            {/* Response tabs */}
            <div style={{ display: 'flex', gap: '8px', marginBottom: '10px' }}>
              <button
                onClick={() => setResponseTab('body')}
                style={{
                  padding: '4px 10px',
                  borderRadius: '6px',
                  border: 'none',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                  background: responseTab === 'body' ? 'rgba(99, 102, 241, 0.3)' : 'transparent',
                  color: responseTab === 'body' ? '#818cf8' : '#94a3b8',
                }}
              >
                Body JSON
              </button>
              <button
                onClick={() => setResponseTab('headers')}
                style={{
                  padding: '4px 10px',
                  borderRadius: '6px',
                  border: 'none',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                  background: responseTab === 'headers' ? 'rgba(99, 102, 241, 0.3)' : 'transparent',
                  color: responseTab === 'headers' ? '#818cf8' : '#94a3b8',
                }}
              >
                Headers
              </button>
            </div>

            {/* Response Content Box */}
            <div style={{
              flex: 1,
              minHeight: '230px',
              background: 'rgba(15, 23, 42, 0.95)',
              border: '1px solid rgba(255, 255, 255, 0.08)',
              borderRadius: '8px',
              padding: '12px',
              overflow: 'auto',
              maxHeight: '400px',
            }}>
              {!response ? (
                <div style={{
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#64748b',
                  fontSize: '0.85rem',
                  gap: '8px',
                }}>
                  <Send size={24} style={{ opacity: 0.4 }} />
                  <span>Execute request above to inspect response</span>
                </div>
              ) : responseTab === 'body' ? (
                <pre style={{
                  margin: 0,
                  fontFamily: 'var(--font-mono)',
                  fontSize: '0.82rem',
                  color: response.ok ? '#e2e8f0' : '#fca5a5',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                }}>
                  {response.data !== null && response.data !== undefined
                    ? (typeof response.data === 'object' ? JSON.stringify(response.data, null, 2) : String(response.data))
                    : (response.error || '(Empty response body)')}
                </pre>
              ) : (
                <pre style={{
                  margin: 0,
                  fontFamily: 'var(--font-mono)',
                  fontSize: '0.78rem',
                  color: '#94a3b8',
                  whiteSpace: 'pre-wrap',
                }}>
                  {JSON.stringify(response.headers, null, 2)}
                </pre>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
