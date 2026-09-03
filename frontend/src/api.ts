import type { Ticket, TicketComment, TicketPage, TicketPriority, TicketStatus } from "./types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api";

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  const response = await fetch(`${API_BASE_URL}/v1/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ email, password })
  });

  if (!response.ok) {
    throw new Error("Login failed");
  }

  return response.json();
}

function authHeaders(token: string): HeadersInit {
  return {
    Authorization: `Bearer ${token}`
  };
}

export async function fetchTickets(
  token: string,
  params?: { status?: TicketStatus; priority?: TicketPriority; q?: string }
): Promise<TicketPage> {
  const searchParams = new URLSearchParams();
  if (params?.status) searchParams.set("status", params.status);
  if (params?.priority) searchParams.set("priority", params.priority);
  if (params?.q) searchParams.set("q", params.q);

  const query = searchParams.toString();
  const response = await fetch(`${API_BASE_URL}/v1/tickets${query ? `?${query}` : ""}`, {
    headers: authHeaders(token)
  });
  if (!response.ok) {
    throw new Error("Tickets could not be loaded");
  }
  return response.json();
}

export async function createTicket(payload: {
  title: string;
  description: string;
  priority: TicketPriority;
}, token: string): Promise<Ticket> {
  const response = await fetch(`${API_BASE_URL}/v1/tickets`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    throw new Error("Ticket could not be created");
  }
  return response.json();
}

export async function fetchComments(token: string, ticketId: string): Promise<TicketComment[]> {
  const response = await fetch(`${API_BASE_URL}/v1/tickets/${ticketId}/comments`, {
    headers: authHeaders(token)
  });

  if (!response.ok) {
    throw new Error("Comments could not be loaded");
  }

  return response.json();
}

export async function createComment(
  token: string,
  ticketId: string,
  payload: { body: string; internalNote: boolean }
): Promise<TicketComment> {
  const response = await fetch(`${API_BASE_URL}/v1/tickets/${ticketId}/comments`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    throw new Error("Comment could not be created");
  }

  return response.json();
}
