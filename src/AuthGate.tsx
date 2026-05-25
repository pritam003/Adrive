import { useState, useEffect, useCallback, ReactNode } from 'react';
import { HardDrive, LogIn, Copy, ExternalLink, Loader } from 'lucide-react';
import type { MeResponse } from './types';
import { getMe, startDeviceLogin, pollDeviceLogin, logout, type DeviceCodeInfo } from './api';

interface Props {
  children: (me: MeResponse) => ReactNode;
}

export function AuthGate({ children }: Props) {
  const [me, setMe] = useState<MeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [flow, setFlow] = useState<DeviceCodeInfo | null>(null);
  const [polling, setPolling] = useState(false);
  const [copied, setCopied] = useState(false);

  const refreshMe = useCallback(async () => {
    try {
      const m = await getMe();
      setMe(m);
      return m;
    } catch (e) {
      setError(String(e));
      return null;
    }
  }, []);

  useEffect(() => {
    refreshMe();
  }, [refreshMe]);

  useEffect(() => {
    if (!flow) return;
    let cancelled = false;
    setPolling(true);
    const intervalMs = Math.max(2, flow.interval) * 1000;
    const expiresAt = Date.now() + flow.expires_in * 1000;

    const tick = async () => {
      if (cancelled) return;
      if (Date.now() > expiresAt) {
        setError('Login code expired. Please try again.');
        setFlow(null);
        setPolling(false);
        return;
      }
      try {
        const r = await pollDeviceLogin(flow.device_code);
        if (cancelled) return;
        if (r.status === 'success') {
          setFlow(null);
          setPolling(false);
          await refreshMe();
          return;
        }
        if (r.status === 'expired') {
          setError('Login code expired. Please try again.');
          setFlow(null);
          setPolling(false);
          return;
        }
        if (r.status === 'error') {
          setError(r.error || 'Login failed.');
          setFlow(null);
          setPolling(false);
          return;
        }
        setTimeout(tick, intervalMs);
      } catch (e) {
        if (cancelled) return;
        setError(String(e));
        setPolling(false);
      }
    };

    const t = setTimeout(tick, intervalMs);
    return () => {
      cancelled = true;
      clearTimeout(t);
    };
  }, [flow, refreshMe]);

  const startLogin = async () => {
    setError(null);
    try {
      const f = await startDeviceLogin();
      setFlow(f);
      window.open(f.verification_uri, '_blank', 'noopener,noreferrer');
    } catch (e) {
      setError(String(e));
    }
  };

  const copyCode = async () => {
    if (!flow) return;
    try {
      await navigator.clipboard.writeText(flow.user_code);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      /* ignore */
    }
  };

  if (!me) {
    return (
      <div className="auth-screen">
        <p>Loading…</p>
      </div>
    );
  }

  if (!me.authenticated) {
    return (
      <div className="auth-screen">
        <div className="auth-card">
          <HardDrive size={48} className="logo-icon" />
          <h1>Adrive</h1>
          {flow ? (
            <>
              <p>Enter this code at the Microsoft sign-in page:</p>
              <div className="device-code-box">
                <span className="device-code">{flow.user_code}</span>
                <button onClick={copyCode} className="device-code-copy" title="Copy">
                  <Copy size={16} />
                </button>
              </div>
              {copied && <p className="device-code-copied">Copied!</p>}
              <a
                href={flow.verification_uri}
                target="_blank"
                rel="noopener noreferrer"
                className="auth-btn"
              >
                <ExternalLink size={18} /> Open Microsoft sign-in
              </a>
              <p className="device-status">
                {polling ? (
                  <>
                    <Loader size={14} className="spin" /> Waiting for you to sign in…
                  </>
                ) : (
                  'Waiting…'
                )}
              </p>
              <button
                onClick={() => {
                  setFlow(null);
                  setError(null);
                }}
                className="link-btn"
              >
                Cancel
              </button>
            </>
          ) : (
            <>
              <p>Sign in with your Microsoft account to access your drive.</p>
              <button onClick={startLogin} className="auth-btn">
                <LogIn size={18} /> Sign in with Microsoft
              </button>
            </>
          )}
          {error && <p className="auth-error">{error}</p>}
        </div>
      </div>
    );
  }

  if (me.ownerConfigured && me.isOwner === false) {
    return (
      <div className="auth-screen">
        <div className="auth-card">
          <h1>Access denied</h1>
          <p>
            This Adrive instance is private. Signed in as{' '}
            <strong>{me.userDetails}</strong>.
          </p>
          <button
            onClick={async () => {
              await logout();
              window.location.reload();
            }}
            className="auth-btn"
          >
            Sign out
          </button>
        </div>
      </div>
    );
  }

  return <>{children(me)}</>;
}
