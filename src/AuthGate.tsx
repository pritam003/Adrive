import { useState, useEffect, ReactNode } from 'react';
import { HardDrive, LogIn } from 'lucide-react';
import type { MeResponse } from './types';
import { getMe } from './api';

interface Props {
  children: (me: MeResponse) => ReactNode;
}

export function AuthGate({ children }: Props) {
  const [me, setMe] = useState<MeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getMe()
      .then((m) => setMe(m))
      .catch((e) => setError(String(e)));
  }, []);

  if (error) return <div className="auth-screen"><p>Error: {error}</p></div>;
  if (!me) return <div className="auth-screen"><p>Loading…</p></div>;

  // Bootstrap mode: if no OWNER_USER_ID configured server-side, allow anonymous use.
  if (!me.authenticated && me.ownerConfigured) {
    return (
      <div className="auth-screen">
        <div className="auth-card">
          <HardDrive size={48} className="logo-icon" />
          <h1>Adrive</h1>
          <p>Sign in with your Microsoft account to access your drive.</p>
          <a href="/.auth/login/aad" className="auth-btn">
            <LogIn size={18} /> Sign in with Microsoft
          </a>
        </div>
      </div>
    );
  }

  if (me.ownerConfigured && me.isOwner === false) {
    return (
      <div className="auth-screen">
        <div className="auth-card">
          <h1>Access denied</h1>
          <p>This Adrive instance is private. Signed in as <strong>{me.userDetails}</strong>.</p>
          <a href="/.auth/logout" className="auth-btn">Sign out</a>
        </div>
      </div>
    );
  }

  return <>{children(me)}</>;
}