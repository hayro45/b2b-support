import { useCallback, useEffect, useState } from "react";
import * as api from "./api";
import type { TicketPage, User } from "./types";
import { label, priorities, statuses } from "./types";
import { Login } from "./components/Login";
import { NewTicket } from "./components/NewTicket";
import { TicketDetail } from "./components/TicketDetail";

type Session = { token: string; user: User; expiresAt: number };
export function App() {
  const [session, setSession] = useState<Session | null>(null);
  const [error, setError] = useState("");
  const [status, setStatus] = useState("");
  const [priority, setPriority] = useState("");
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<TicketPage | null>(null);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState("");
  const [creating, setCreating] = useState(false);
  const [revision, setRevision] = useState(0);
  const logout = useCallback((message = "") => {
    setSession(null);
    setSelected("");
    setCreating(false);
    setResult(null);
    setPage(0);
    setStatus("");
    setPriority("");
    setSearch("");
    setQuery("");
    setError(message);
  }, []);
  const onError = useCallback(
    (reason: unknown) => {
      if (reason instanceof DOMException && reason.name === "AbortError")
        return;
      if (reason instanceof api.ApiError && reason.status === 401)
        logout(reason.message);
      else
        setError(
          reason instanceof Error
            ? reason.message
            : "Unable to reach the server. Please try again.",
        );
    },
    [logout],
  );
  const refresh = useCallback(() => setRevision((value) => value + 1), []);
  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setQuery(search.trim());
      setPage(0);
    }, 300);
    return () => window.clearTimeout(timeout);
  }, [search]);
  useEffect(() => {
    if (!session) return;
    const timeout = window.setTimeout(
      () => logout("Your session has expired. Please sign in again."),
      Math.max(0, session.expiresAt - Date.now()),
    );
    return () => window.clearTimeout(timeout);
  }, [session, logout]);
  useEffect(() => {
    if (!session) return;
    const controller = new AbortController();
    setLoading(true);
    setError("");
    setResult(null);
    api
      .fetchTickets(
        session.token,
        { status, priority, q: query, page },
        controller.signal,
      )
      .then((data) => {
        if (!controller.signal.aborted) setResult(data);
      })
      .catch((reason) => {
        if (!controller.signal.aborted) onError(reason);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [session, status, priority, query, page, revision, onError]);
  if (!session)
    return (
      <Login
        message={error}
        onLogin={(token, user, seconds) => {
          setError("");
          setSession({ token, user, expiresAt: Date.now() + seconds * 1000 });
        }}
      />
    );
  const { user, token } = session;
  return (
    <div className="workspace">
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            S
          </span>
          <div>
            Support Desk
            <span className="brand-caption">Customer operations</span>
          </div>
        </div>
        <div className="account">
          <div>
            <strong>{user.fullName}</strong>
            <span>{label(user.role)}</span>
          </div>
          <button className="secondary" onClick={() => logout()}>
            Sign out
          </button>
        </div>
      </header>
      <main id="main" className="main">
        <div className="page-heading">
          <div>
            <p className="eyebrow">Workspace / Support</p>
            <h1>Ticket operations</h1>
            <p className="muted">
              Track requests, keep conversations together, and move work
              forward.
            </p>
          </div>
          <button
            onClick={() => {
              setCreating(!creating);
              setSelected("");
            }}
          >
            {creating ? "Cancel new ticket" : "+ New ticket"}
          </button>
        </div>
        {error && (
          <div role="alert" className="error-banner">
            {error}
            <button className="secondary" onClick={refresh}>
              Retry
            </button>
            <button
              className="secondary"
              onClick={() => setError("")}
              aria-label="Dismiss error"
            >
              Dismiss
            </button>
          </div>
        )}
        {creating && (
          <NewTicket
            token={token}
            onError={onError}
            onCreated={(ticket) => {
              setCreating(false);
              setSelected(ticket.id);
              setPage(0);
              refresh();
            }}
          />
        )}
        <div className="desk-grid">
          <section className="panel queue" aria-labelledby="queue-heading">
            <div className="panel-heading">
              <h2 id="queue-heading">Request queue</h2>
              <span className="count">
                {result ? `${result.totalElements} tickets` : "—"}
              </span>
            </div>
            <div className="filters">
              <label className="search">
                Search tickets
                <input
                  type="search"
                  placeholder="Search by title…"
                  value={search}
                  maxLength={180}
                  onChange={(event) => setSearch(event.target.value)}
                />
              </label>
              <label>
                Status
                <select
                  value={status}
                  onChange={(event) => {
                    setStatus(event.target.value);
                    setPage(0);
                  }}
                >
                  <option value="">All statuses</option>
                  {statuses.map((value) => (
                    <option key={value} value={value}>
                      {label(value)}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Priority
                <select
                  value={priority}
                  onChange={(event) => {
                    setPriority(event.target.value);
                    setPage(0);
                  }}
                >
                  <option value="">All priorities</option>
                  {priorities.map((value) => (
                    <option key={value} value={value}>
                      {label(value)}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <div className="ticket-list" aria-busy={loading}>
              {loading && (
                <p role="status" className="empty">
                  Loading requests…
                </p>
              )}
              {!loading && result?.content.length === 0 && (
                <div className="empty">
                  <h3>No matching requests</h3>
                  <p>Try another filter or create a new ticket.</p>
                </div>
              )}
              {!loading &&
                result?.content.map((ticket) => (
                  <button
                    key={ticket.id}
                    className={`ticket-row ${selected === ticket.id ? "selected" : ""}`}
                    aria-pressed={selected === ticket.id}
                    onClick={() => {
                      setSelected(ticket.id);
                      setCreating(false);
                    }}
                  >
                    <span className="ticket-meta">
                      <span>{ticket.ticketNo}</span>
                      <span
                        className={`badge priority-${ticket.priority.toLowerCase()}`}
                      >
                        {label(ticket.priority)}
                      </span>
                    </span>
                    <strong className="ticket-title">{ticket.title}</strong>
                    <span className="ticket-description">
                      {ticket.description}
                    </span>
                    <span className="ticket-meta">
                      <span
                        className={`status-dot status-${ticket.status.toLowerCase()}`}
                      >
                        {label(ticket.status)}
                      </span>
                      <time dateTime={ticket.createdAt}>
                        {new Date(ticket.createdAt).toLocaleDateString()}
                      </time>
                    </span>
                  </button>
                ))}
            </div>
            <nav className="pagination" aria-label="Ticket pages">
              <button
                className="secondary"
                disabled={loading || page === 0}
                onClick={() => setPage(page - 1)}
              >
                Previous
              </button>
              <span>
                Page {page + 1} of {Math.max(1, result?.totalPages ?? 1)}
              </span>
              <button
                className="secondary"
                disabled={loading || !result || page + 1 >= result.totalPages}
                onClick={() => setPage(page + 1)}
              >
                Next
              </button>
            </nav>
          </section>
          {selected ? (
            <TicketDetail
              key={`${token}-${selected}`}
              id={selected}
              token={token}
              staff={user.role !== "CUSTOMER"}
              onError={onError}
              onChanged={refresh}
              onClose={() => setSelected("")}
            />
          ) : (
            <aside className="panel detail-placeholder">
              <span className="placeholder-icon" aria-hidden="true">
                ↗
              </span>
              <h2>A clear view of every request</h2>
              <p>
                Select a ticket to read the conversation and review its details.
              </p>
            </aside>
          )}
        </div>
      </main>
      <footer>Support Desk · Organization workspace</footer>
    </div>
  );
}
