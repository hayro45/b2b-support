import type {
  Agent,
  Assignment,
  Ticket,
  TicketComment,
  TicketPage,
  TicketPriority,
  TicketStatus,
  User,
} from "./types";
const base = import.meta.env.VITE_API_BASE_URL ?? "/api";
export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
  }
}
export async function request<T>(
  path: string,
  token?: string,
  options: RequestInit = {},
): Promise<T> {
  const response = await fetch(`${base}/v1${path}`, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new ApiError(
      response.status === 401
        ? path === "/auth/login"
          ? "Email or password is incorrect."
          : "Your session has expired. Please sign in again."
        : error?.message ||
            `Request failed (${response.status}). Please try again.`,
      response.status,
    );
  }
  return response.json();
}
const body = (data: unknown, method = "POST"): RequestInit => ({
  method,
  body: JSON.stringify(data),
});
export const login = (email: string, password: string) =>
  request<{ accessToken: string; expiresInSeconds: number }>(
    "/auth/login",
    undefined,
    body({ email, password }),
  );
export const fetchMe = (token: string) => request<User>("/auth/me", token);
export const fetchAgents = (token: string, signal?: AbortSignal) =>
  request<Agent[]>("/auth/agents", token, { signal });
export const fetchTickets = (
  token: string,
  params: { status?: string; priority?: string; q?: string; page: number },
  signal?: AbortSignal,
) => {
  const query = new URLSearchParams({
    page: String(params.page),
    size: "10",
    sort: "createdAt,desc",
  });
  for (const key of ["status", "priority", "q"] as const)
    if (params[key]) query.set(key, params[key]);
  return request<TicketPage>(`/tickets?${query}`, token, { signal });
};
export const fetchTicket = (token: string, id: string, signal?: AbortSignal) =>
  request<Ticket>(`/tickets/${id}`, token, { signal });
export const createTicket = (
  data: { title: string; description: string; priority: TicketPriority },
  token: string,
) => request<Ticket>("/tickets", token, body(data));
export const fetchComments = (
  token: string,
  id: string,
  signal?: AbortSignal,
) => request<TicketComment[]>(`/tickets/${id}/comments`, token, { signal });
export const createComment = (
  token: string,
  id: string,
  data: { body: string; internalNote: boolean },
) => request<TicketComment>(`/tickets/${id}/comments`, token, body(data));
export const fetchHistory = (token: string, id: string, signal?: AbortSignal) =>
  request<Assignment[]>(`/tickets/${id}/assignment-history`, token, { signal });
export const changeStatus = (token: string, id: string, status: TicketStatus) =>
  request<Ticket>(`/tickets/${id}/status`, token, body({ status }, "PATCH"));
export const assignTicket = (
  token: string,
  id: string,
  assigneeUserId: string,
) =>
  request<Ticket>(
    `/tickets/${id}/assign`,
    token,
    body({ assigneeUserId }, "PATCH"),
  );
