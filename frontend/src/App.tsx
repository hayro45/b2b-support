import { FormEvent, useEffect, useMemo, useState } from "react";
import { createComment, createTicket, fetchComments, fetchTickets, login } from "./api";
import type { Ticket, TicketComment, TicketPriority, TicketStatus } from "./types";

const priorities: TicketPriority[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
const statuses: TicketStatus[] = ["OPEN", "IN_PROGRESS", "WAITING_CUSTOMER", "RESOLVED", "CLOSED"];

export function App() {
  const [token, setToken] = useState<string>("");
  const [email, setEmail] = useState("agent@demo.local");
  const [password, setPassword] = useState("demo12345");
  const [isLoggingIn, setIsLoggingIn] = useState(false);

  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [selectedTicketId, setSelectedTicketId] = useState<string>("");
  const [comments, setComments] = useState<TicketComment[]>([]);
  const [commentBody, setCommentBody] = useState("");
  const [internalNote, setInternalNote] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<TicketPriority>("MEDIUM");

  const [statusFilter, setStatusFilter] = useState<string>("");

  const createDisabled = useMemo(() => title.trim().length < 3 || description.trim().length < 10, [title, description]);

  async function loadTickets() {
    if (!token) {
      setTickets([]);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const page = await fetchTickets(token, { status: statusFilter ? (statusFilter as TicketStatus) : undefined });
      setTickets(page.content);
    } catch (e) {
      const message = e instanceof Error ? e.message : "Unknown error";
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    loadTickets();
  }, [statusFilter, token]);

  useEffect(() => {
    if (!token || !selectedTicketId) {
      setComments([]);
      return;
    }

    (async () => {
      try {
        const list = await fetchComments(token, selectedTicketId);
        setComments(list);
      } catch (e) {
        const message = e instanceof Error ? e.message : "Unknown error";
        setError(message);
      }
    })();
  }, [token, selectedTicketId]);

  async function onLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      setIsLoggingIn(true);
      setError(null);
      const response = await login(email, password);
      setToken(response.accessToken);
    } catch (e) {
      const message = e instanceof Error ? e.message : "Unknown error";
      setError(message);
      setToken("");
    } finally {
      setIsLoggingIn(false);
    }
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      setError("Please login first");
      return;
    }

    try {
      setError(null);
      await createTicket({ title, description, priority }, token);
      setTitle("");
      setDescription("");
      setPriority("MEDIUM");
      await loadTickets();
    } catch (e) {
      const message = e instanceof Error ? e.message : "Unknown error";
      setError(message);
    }
  }

  async function onCommentSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token || !selectedTicketId || commentBody.trim().length < 2) {
      return;
    }

    try {
      setError(null);
      await createComment(token, selectedTicketId, { body: commentBody, internalNote });
      setCommentBody("");
      setInternalNote(false);
      const list = await fetchComments(token, selectedTicketId);
      setComments(list);
    } catch (e) {
      const message = e instanceof Error ? e.message : "Unknown error";
      setError(message);
    }
  }

  return (
    <div className="layout">
      <header className="hero">
        <p className="kicker">B2B Support Desk MVP</p>
        <h1>Ticket Operations</h1>
        <p className="subtitle">Create, filter, and review support tickets from the same panel.</p>
      </header>

      <section className="card auth-card">
        <h2>Auth</h2>
        <form className="auth-form" onSubmit={onLogin}>
          <label>
            Email
            <input value={email} onChange={(e) => setEmail(e.target.value)} />
          </label>
          <label>
            Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
          </label>
          <button type="submit" disabled={isLoggingIn}>
            {isLoggingIn ? "Signing in..." : "Sign In"}
          </button>
          <p className="muted small">Demo: agent@demo.local / demo12345</p>
          {token && <p className="success small">Authenticated</p>}
        </form>
      </section>

      <main className="grid">
        <section className="card">
          <h2>Create Ticket</h2>
          <form onSubmit={onSubmit} className="form">
            <label>
              Title
              <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Payment webhook fails intermittently" />
            </label>

            <label>
              Description
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={5}
                placeholder="Include business impact, timeline, and sample error details"
              />
            </label>

            <label>
              Priority
              <select value={priority} onChange={(e) => setPriority(e.target.value as TicketPriority)}>
                {priorities.map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
            </label>

            <button type="submit" disabled={createDisabled || !token}>
              Create Ticket
            </button>
          </form>
        </section>

        <section className="card">
          <div className="row between">
            <h2>Ticket List</h2>
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="">All Statuses</option>
              {statuses.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </div>

          {isLoading && <p className="muted">Loading...</p>}
          {error && <p className="error">{error}</p>}

          <div className="ticket-list">
            {tickets.map((ticket) => (
              <article key={ticket.id} className="ticket-item">
                <div className="row between">
                  <strong>{ticket.ticketNo}</strong>
                  <span className={`badge ${ticket.priority.toLowerCase()}`}>{ticket.priority}</span>
                </div>
                <h3>{ticket.title}</h3>
                <p>{ticket.description}</p>
                <div className="row between muted small">
                  <span>Status: {ticket.status}</span>
                  <span>{new Date(ticket.createdAt).toLocaleString()}</span>
                </div>
                <button className="ghost-btn" onClick={() => setSelectedTicketId(ticket.id)}>
                  Open Comments
                </button>
              </article>
            ))}

            {!isLoading && tickets.length === 0 && <p className="muted">No tickets found.</p>}
          </div>
        </section>

        <section className="card">
          <h2>Comments</h2>
          {!selectedTicketId && <p className="muted">Select a ticket to read and add comments.</p>}

          {selectedTicketId && (
            <>
              <form className="form" onSubmit={onCommentSubmit}>
                <label>
                  Comment
                  <textarea
                    rows={4}
                    value={commentBody}
                    onChange={(e) => setCommentBody(e.target.value)}
                    placeholder="Write your update"
                  />
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={internalNote}
                    onChange={(e) => setInternalNote(e.target.checked)}
                  />
                  Internal note
                </label>
                <button type="submit" disabled={!token || commentBody.trim().length < 2}>
                  Add Comment
                </button>
              </form>

              <div className="ticket-list">
                {comments.map((comment) => (
                  <article key={comment.id} className="ticket-item">
                    <p>{comment.body}</p>
                    <div className="row between muted small">
                      <span>{comment.internalNote ? "Internal" : "Public"}</span>
                      <span>{new Date(comment.createdAt).toLocaleString()}</span>
                    </div>
                  </article>
                ))}
                {comments.length === 0 && <p className="muted">No comments yet.</p>}
              </div>
            </>
          )}
        </section>
      </main>
    </div>
  );
}
