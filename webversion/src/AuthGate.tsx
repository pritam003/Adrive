import { useState, useEffect, useCallback, useMemo, ReactNode } from 'react';
import { HardDrive, LogIn, Copy, ExternalLink, Loader, Sparkles } from 'lucide-react';
import type { MeResponse } from './types';
import { getMe, startDeviceLogin, pollDeviceLogin, logout, type DeviceCodeInfo } from './api';

interface Props {
  children: (me: MeResponse) => ReactNode;
}

const TAGLINES = [
  'Your stuff, anywhere. ✨',
  'Drop, share, smile. 📁',
  'Like Google Drive, but yours. 🚀',
  'Unlimited everything. ∞',
  'Move fast. Lose nothing. 🎯',
  'Your cloud. Your rules. 🌩️',
];

function FloatingEmojis() {
  // stable per-mount random positions
  const items = useMemo(
    () =>
      ['📁', '📄', '🖼️', '🎵', '🎬', '📊', '✨', '☁️', '🚀', '💾', '🎨', '📸'].map((e, i) => ({
        e,
        left: 5 + ((i * 37) % 90),
        delay: (i * 0.6) % 8,
        duration: 12 + ((i * 1.7) % 8),
        size: 22 + ((i * 5) % 18),
      })),
    []
  );
  return (
    <div className="auth-bg" aria-hidden="true">
      {items.map((it, i) => (
        <span
          key={i}
          className="auth-bg-emoji"
          style={{
            left: `${it.left}%`,
            fontSize: `${it.size}px`,
            animationDelay: `${it.delay}s`,
            animationDuration: `${it.duration}s`,
          }}
        >
          {it.e}
        </span>
      ))}
    </div>
  );
}

export function AuthGate({ children }: Props) {
  const [me, setMe] = useState<MeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [flow, setFlow] = useState<DeviceCodeInfo | null>(null);
  const [polling, setPolling] = useState(false);
  const [copied, setCopied] = useState(false);
  const [taglineIdx, setTaglineIdx] = useState(() => Math.floor(Math.random() * TAGLINES.length));
  const [logoBounce, setLogoBounce] = useState(false);

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

  // rotate tagline every 4s
  useEffect(() => {
    const t = setInterval(() => setTaglineIdx(i => (i + 1) % TAGLINES.length), 4000);
    return () => clearInterval(t);
  }, []);

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
    setLogoBounce(true);
    setTimeout(() => setLogoBounce(false), 600);
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
        <FloatingEmojis />
        <div className="auth-card">
          <div className="auth-logo-wrap">
            <HardDrive size={48} className="logo-icon spin-slow" />
          </div>
          <p className="auth-loading">Loading your drive…</p>
        </div>
      </div>
    );
  }

  if (!me.authenticated) {
    return (
      <div className="auth-screen">
        <FloatingEmojis />
        <div className="auth-card">
          <div className={'auth-logo-wrap' + (logoBounce ? ' bounce' : '')}>
            <div className="auth-logo-glow" />
            <HardDrive size={48} className="logo-icon" />
          </div>
          <h1 className="auth-title">Adrive</h1>
          <p className="auth-tagline" key={taglineIdx}>{TAGLINES[taglineIdx]}</p>

          {flow ? (
            <>
              <p className="auth-step">
                <span className="auth-step-num">1</span>Copy this code
              </p>
              <div className="device-code-box" onClick={copyCode} role="button" tabIndex={0}>
                <span className="device-code">{flow.user_code}</span>
                <button onClick={copyCode} className="device-code-copy" title="Copy">
                  <Copy size={16} />
                </button>
              </div>
              {copied && <p className="device-code-copied">✓ Copied!</p>}

              <p className="auth-step">
                <span className="auth-step-num">2</span>Sign in on Microsoft
              </p>
              <a
                href={flow.verification_uri}
                target="_blank"
                rel="noopener noreferrer"
                className="auth-btn auth-btn-ms"
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
                ← Start over
              </button>
            </>
          ) : (
            <>
              <button onClick={startLogin} className="auth-btn auth-btn-shimmer">
                <LogIn size={18} /> Sign in with Microsoft
                <Sparkles size={14} className="auth-btn-sparkle" />
              </button>
              <p className="auth-perks">
                <span>∞ storage</span>
                <span>🔒 private</span>
                <span>⚡ fast</span>
              </p>
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
        <FloatingEmojis />
        <div className="auth-card">
          <div className="auth-logo-wrap">
            <span style={{ fontSize: 48 }}>🚫</span>
          </div>
          <h1 className="auth-title">Access denied</h1>
          <p className="auth-tagline">
            This Adrive is private. Signed in as <strong>{me.userDetails}</strong>.
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
