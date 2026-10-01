import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import { createTicket } from "../api";
import type { Ticket, TicketPriority } from "../types";
import { label, priorities } from "../types";
export function NewTicket({
  token,
  onCreated,
  onError,
}: {
  token: string;
  onCreated: (ticket: Ticket) => void;
  onError: (reason: unknown) => void;
}) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<TicketPriority>("MEDIUM");
  const [busy, setBusy] = useState(false);
  const mounted = useRef(false);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);
  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    try {
      const ticket = await createTicket(
        { title: title.trim(), description: description.trim(), priority },
        token,
      );
      if (mounted.current) onCreated(ticket);
    } catch (reason) {
      if (mounted.current) onError(reason);
    } finally {
      if (mounted.current) setBusy(false);
    }
  }
  return (
    <section className="panel new-ticket">
      <h2>New support request</h2>
      <form className="form" onSubmit={submit} aria-busy={busy}>
        <label>
          Title
          <input
            autoFocus
            required
            minLength={3}
            maxLength={180}
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="A short summary of the issue"
          />
        </label>
        <label>
          Description
          <textarea
            required
            minLength={10}
            maxLength={5000}
            rows={4}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="Describe the issue, business impact, and steps to reproduce."
          />
        </label>
        <div className="row">
          <label>
            Priority
            <select
              value={priority}
              onChange={(event) =>
                setPriority(event.target.value as TicketPriority)
              }
            >
              {priorities.map((value) => (
                <option key={value} value={value}>
                  {label(value)}
                </option>
              ))}
            </select>
          </label>
          <button
            disabled={
              busy || title.trim().length < 3 || description.trim().length < 10
            }
          >
            {busy ? "Creating…" : "Create ticket"}
          </button>
        </div>
      </form>
    </section>
  );
}
