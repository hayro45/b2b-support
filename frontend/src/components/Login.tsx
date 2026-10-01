import { useState } from "react";
import type { FormEvent } from "react";
import { fetchMe, login } from "../api";
import type { User } from "../types";
export function Login({
  message,
  onLogin,
}: {
  message: string;
  onLogin: (token: string, user: User, seconds: number) => void;
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const session = await login(email.trim(), password);
      const user = await fetchMe(session.accessToken);
      onLogin(session.accessToken, user, session.expiresInSeconds);
    } catch (reason) {
      setError(
        reason instanceof Error
          ? reason.message
          : "Unable to sign in. Please try again.",
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <main className="login-layout">
      <section className="login-story">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            S
          </span>
          Support Desk
        </div>
        <p className="eyebrow">Better support, together</p>
        <h1>
          Every request.
          <br />
          One shared workspace.
        </h1>
        <p>
          Keep your team and customers connected, from the first question to the
          final resolution.
        </p>
        <span className="story-caption">Customer operations / B2B</span>
      </section>
      <section className="login-panel">
        <div className="login-form">
          <p className="eyebrow">Welcome back</p>
          <h2>Sign in to your workspace</h2>
          <p className="muted">Use your organization account to continue.</p>
          <form className="form" onSubmit={submit} aria-busy={busy}>
            {(error || message) && (
              <p role="alert" className="error-banner">
                {error || message}
              </p>
            )}
            <label>
              Email address
              <input
                type="email"
                required
                autoFocus
                autoComplete="username"
                maxLength={254}
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </label>
            <label>
              Password
              <input
                type="password"
                required
                autoComplete="current-password"
                maxLength={256}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </label>
            <button type="submit" disabled={busy}>
              {busy ? "Signing in…" : "Sign in"}
            </button>
          </form>
          <p className="small muted">
            Need access? Contact your organization administrator.
          </p>
        </div>
      </section>
    </main>
  );
}
