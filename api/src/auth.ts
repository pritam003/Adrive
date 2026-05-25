// Microsoft Entra ID Device Code Flow + JWT cookie auth.
// Pattern mirrors github.com/pritam003/photo-master-app:
//   - No client secret. App registration just needs "Allow public client flows" = Yes.
//   - User runs OAuth in their browser; we poll Microsoft for the token.
//   - We issue our own short-lived JWT in an httpOnly cookie.

import { HttpRequest, HttpResponseInit } from '@azure/functions';
import jwt from 'jsonwebtoken';

const TENANT_ID = process.env.AZURE_TENANT_ID || 'common';
const CLIENT_ID = process.env.MSAL_CLIENT_ID || '';
const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-change-me-please-32chars+';
const OWNER_USER_ID = process.env.OWNER_USER_ID || '';

export const COOKIE_NAME = 'auth_token';
const COOKIE_MAX_AGE_S = 7 * 24 * 60 * 60; // 7 days

// ---------- Microsoft device code flow ----------

export interface DeviceCodeResponse {
  device_code: string;
  user_code: string;
  verification_uri: string;
  expires_in: number;
  interval: number;
}

export async function initiateDeviceCodeFlow(): Promise<DeviceCodeResponse> {
  if (!CLIENT_ID) {
    throw new Error('MSAL_CLIENT_ID env var is not set');
  }
  const params = new URLSearchParams({
    client_id: CLIENT_ID,
    scope: 'User.Read openid profile email offline_access',
  });
  const resp = await fetch(
    `https://login.microsoftonline.com/${TENANT_ID}/oauth2/v2.0/devicecode`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString(),
    }
  );
  if (!resp.ok) {
    const text = await resp.text();
    throw new Error(`devicecode endpoint ${resp.status}: ${text}`);
  }
  const data = (await resp.json()) as DeviceCodeResponse;
  // Microsoft sometimes returns "https://login.microsoft.com/device" (MSA-only)
  // which shows "wrong page" for AAD accounts and vice-versa. The universal
  // "https://microsoft.com/devicelogin" page auto-detects account type and
  // works for both Microsoft personal and work/school accounts.
  return { ...data, verification_uri: 'https://microsoft.com/devicelogin' };
}

export class DeviceCodeExpiredError extends Error {
  code = 'expired_token' as const;
}

export async function pollForDeviceCodeToken(
  deviceCode: string
): Promise<{ access_token: string; id_token: string } | null> {
  if (!CLIENT_ID) throw new Error('MSAL_CLIENT_ID env var is not set');
  const params = new URLSearchParams({
    client_id: CLIENT_ID,
    device_code: deviceCode,
    grant_type: 'urn:ietf:params:oauth:grant-type:device_code',
  });
  const resp = await fetch(
    `https://login.microsoftonline.com/${TENANT_ID}/oauth2/v2.0/token`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString(),
    }
  );
  const data = (await resp.json()) as Record<string, unknown>;
  if (resp.status === 400) {
    const error = data.error as string | undefined;
    if (error === 'authorization_pending' || error === 'slow_down') return null;
    if (error === 'expired_token' || error === 'invalid_grant') {
      throw new DeviceCodeExpiredError('device code expired');
    }
    throw new Error(`device code flow error: ${error || JSON.stringify(data)}`);
  }
  if (!resp.ok) throw new Error(`token endpoint ${resp.status}: ${JSON.stringify(data)}`);
  return {
    access_token: data.access_token as string,
    id_token: data.id_token as string,
  };
}

export async function getMicrosoftUser(accessToken: string): Promise<{
  id: string;
  displayName: string;
  mail: string | null;
  userPrincipalName: string;
}> {
  const resp = await fetch('https://graph.microsoft.com/v1.0/me', {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!resp.ok) throw new Error(`graph /me ${resp.status}`);
  return (await resp.json()) as {
    id: string;
    displayName: string;
    mail: string | null;
    userPrincipalName: string;
  };
}

// ---------- JWT + cookies ----------

export interface AuthPayload {
  sub: string;
  name: string;
  email: string;
}

export function createToken(user: AuthPayload): string {
  return jwt.sign(user, JWT_SECRET, { expiresIn: '7d' });
}

export function verifyToken(token: string): AuthPayload | null {
  try {
    const payload = jwt.verify(token, JWT_SECRET) as jwt.JwtPayload;
    if (!payload.sub) return null;
    return {
      sub: payload.sub as string,
      name: (payload.name as string) || '',
      email: (payload.email as string) || '',
    };
  } catch {
    return null;
  }
}

function parseCookies(header: string | null): Record<string, string> {
  const out: Record<string, string> = {};
  if (!header) return out;
  for (const part of header.split(';')) {
    const idx = part.indexOf('=');
    if (idx === -1) continue;
    const k = part.substring(0, idx).trim();
    const v = part.substring(idx + 1).trim();
    if (k) out[k] = decodeURIComponent(v);
  }
  return out;
}

export function getUserFromRequest(req: HttpRequest): AuthPayload | null {
  const cookieHeader = req.headers.get('cookie');
  const cookies = parseCookies(cookieHeader);
  const token = cookies[COOKIE_NAME];
  if (!token) return null;
  return verifyToken(token);
}

export function buildAuthCookie(token: string): string {
  return [
    `${COOKIE_NAME}=${token}`,
    'Path=/',
    'HttpOnly',
    'Secure',
    'SameSite=Lax',
    `Max-Age=${COOKIE_MAX_AGE_S}`,
  ].join('; ');
}

export function clearedAuthCookie(): string {
  return [`${COOKIE_NAME}=`, 'Path=/', 'HttpOnly', 'Secure', 'SameSite=Lax', 'Max-Age=0'].join('; ');
}

// ---------- Authorization gate ----------

export { OWNER_USER_ID };

export function isOwner(req: HttpRequest): boolean {
  const user = getUserFromRequest(req);
  if (!user) return false;
  if (!OWNER_USER_ID) return true; // bootstrap mode: any authenticated user is owner
  return user.sub === OWNER_USER_ID;
}

export function requireOwner(req: HttpRequest): HttpResponseInit | null {
  const user = getUserFromRequest(req);
  if (!user) return { status: 401, jsonBody: { error: 'unauthorized' } };
  if (OWNER_USER_ID && user.sub !== OWNER_USER_ID) {
    return { status: 403, jsonBody: { error: 'forbidden' } };
  }
  return null;
}
