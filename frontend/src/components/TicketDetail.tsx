import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import * as api from "../api";
import type {
  Agent,
  Assignment,
  Ticket,
  TicketComment,
  TicketStatus,
} from "../types";
import { label, transitions } from "../types";
const date = (value: string) => new Date(value).toLocaleString();
export function TicketDetail({
  id,
  token,
  staff,
  onError,
  onChanged,
  onClose,
}: {
  id: string;
  token: string;
  staff: boolean;
  onError: (reason: unknown) => void;
  onChanged: () => void;
  onClose: () => void;
}) {
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [comments, setComments] = useState<TicketComment[]>([]);
  const [agents, setAgents] = useState<Agent[]>([]);
  const [history, setHistory] = useState<Assignment[]>([]);
  const [body, setBody] = useState("");
  const [internal, setInternal] = useState(false);
  const [status, setStatus] = useState("");
  const [assignee, setAssignee] = useState("");
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [revision, setRevision] = useState(0);
  const [failed, setFailed] = useState(false);
  const mounted = useRef(false);
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setFailed(false);
    Promise.all([
      api.fetchTicket(token, id, controller.signal),
      api.fetchComments(token, id, controller.signal),
      staff ? api.fetchAgents(token, controller.signal) : Promise.resolve([]),
      staff
        ? api.fetchHistory(token, id, controller.signal)
        : Promise.resolve([]),
    ])
      .then(([detail, conversation, users, assignments]) => {
        if (controller.signal.aborted) return;
        setTicket(detail);
        setComments(conversation);
        setAgents(users);
        setHistory(assignments);
        setStatus("");
        setAssignee(detail.assigneeUserId ?? "");
      })
      .catch((reason) => {
        if (!controller.signal.aborted) {
          setFailed(true);
          onError(reason);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [id, token, staff, onError, revision]);
  useEffect(() => {
    if (ticket && !loading) heading.current?.focus();
  }, [ticket, loading]);
  async function mutate(action: () => Promise<unknown>, reset?: () => void) {
    setBusy(true);
    try {
      await action();
      if (mounted.current) {
        reset?.();
        setRevision((value) => value + 1);
        onChanged();
      }
    } catch (reason) {
      if (mounted.current) onError(reason);
    } finally {
      if (mounted.current) setBusy(false);
    }
  }
  function comment(event: FormEvent) {
    event.preventDefault();
    void mutate(
      () =>
        api.createComment(token, id, {
          body: body.trim(),
          internalNote: staff && internal,
        }),
      () => {
        setBody("");
        setInternal(false);
      },
    );
  }
  const agentName = (userId: string | null) =>
    agents.find((agent) => agent.id === userId)?.fullName ??
    (userId ? "Former team member" : "Unassigned");
  return (
    <section
      className="panel detail"
      aria-label="Ticket details"
      aria-busy={loading || busy}
    >
      <div className="panel-heading">
        <span className="eyebrow">Request details</span>
        <button className="secondary" onClick={onClose}>
          Close details
        </button>
      </div>
      {loading && (
        <p role="status" className="empty">
          Loading ticket…
        </p>
      )}
      {!loading && failed && (
        <div className="empty">
          <p>Could not load ticket details.</p>
          <button onClick={() => setRevision((value) => value + 1)}>
            Retry details
          </button>
        </div>
      )}
      {!loading && !failed && ticket && (
        <>
          <p className="muted small">{ticket.ticketNo}</p>
          <h2 tabIndex={-1} ref={heading}>
            {ticket.title}
          </h2>
          <div className="row">
            <span className={`badge priority-${ticket.priority.toLowerCase()}`}>
              {label(ticket.priority)}
            </span>
            <span className="badge">{label(ticket.status)}</span>
          </div>
          <p className="description">{ticket.description}</p>
          <dl className="metadata">
            <div>
              <dt>Created</dt>
              <dd>{date(ticket.createdAt)}</dd>
            </div>
            <div>
              <dt>Updated</dt>
              <dd>{date(ticket.updatedAt)}</dd>
            </div>
            {staff && (
              <div>
                <dt>Assigned to</dt>
                <dd>{agentName(ticket.assigneeUserId)}</dd>
              </div>
            )}
          </dl>
          {staff && (
            <div className="management">
              <h3>Team actions</h3>
              <form
                className="row"
                onSubmit={(event) => {
                  event.preventDefault();
                  void mutate(() =>
                    api.changeStatus(token, id, status as TicketStatus),
                  );
                }}
              >
                <label>
                  Next status
                  <select
                    value={status}
                    onChange={(event) => setStatus(event.target.value)}
                    disabled={busy}
                  >
                    <option value="">Select transition</option>
                    {transitions[ticket.status].map((value) => (
                      <option key={value} value={value}>
                        {label(value)}
                      </option>
                    ))}
                  </select>
                </label>
                <button disabled={busy || !status}>Update status</button>
              </form>
              <form
                className="row"
                onSubmit={(event) => {
                  event.preventDefault();
                  void mutate(() => api.assignTicket(token, id, assignee));
                }}
              >
                <label>
                  Assign team member
                  <select
                    value={assignee}
                    onChange={(event) => setAssignee(event.target.value)}
                    disabled={busy}
                  >
                    <option value="">Select team member</option>
                    {agents.map((agent) => (
                      <option key={agent.id} value={agent.id}>
                        {agent.fullName} ({agent.email})
                      </option>
                    ))}
                  </select>
                </label>
                <button
                  disabled={
                    busy || !assignee || assignee === ticket.assigneeUserId
                  }
                >
                  Assign
                </button>
              </form>
            </div>
          )}
          <div className="conversation">
            <h3>
              Conversation <span className="count">{comments.length}</span>
            </h3>
            {comments.length === 0 && (
              <p className="muted small">
                No updates yet. Start the conversation below.
              </p>
            )}
            {comments
              .filter((comment) => staff || !comment.internalNote)
              .map((comment) => (
                <article
                  key={comment.id}
                  className={`comment ${comment.internalNote ? "internal" : ""}`}
                >
                  <div className="ticket-meta">
                    <strong>
                      {comment.internalNote ? "Internal note" : "Public reply"}
                    </strong>
                    <time dateTime={comment.createdAt}>
                      {date(comment.createdAt)}
                    </time>
                  </div>
                  <p>{comment.body}</p>
                </article>
              ))}
            <form className="form" onSubmit={comment}>
              <label>
                Reply
                <textarea
                  required
                  minLength={2}
                  maxLength={5000}
                  rows={3}
                  value={body}
                  onChange={(event) => setBody(event.target.value)}
                  disabled={busy}
                  placeholder="Share an update…"
                />
              </label>
              {staff && (
                <label className="checkbox">
                  <input
                    type="checkbox"
                    checked={internal}
                    onChange={(event) => setInternal(event.target.checked)}
                    disabled={busy}
                  />
                  Internal note — visible to your team only
                </label>
              )}
              <button disabled={busy || body.trim().length < 2}>
                {busy
                  ? "Saving…"
                  : internal
                    ? "Add internal note"
                    : "Send reply"}
              </button>
            </form>
          </div>
          {staff && (
            <details className="history">
              <summary>Assignment history ({history.length})</summary>
              {history.length === 0 ? (
                <p className="muted small">No assignment changes yet.</p>
              ) : (
                <ol>
                  {history.map((item) => (
                    <li key={item.id}>
                      <strong>
                        {agentName(item.fromUserId)} →{" "}
                        {agentName(item.toUserId)}
                      </strong>
                      <span>{date(item.changedAt)}</span>
                    </li>
                  ))}
                </ol>
              )}
            </details>
          )}
        </>
      )}
    </section>
  );
}
