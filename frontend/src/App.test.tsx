import { describe, expect, it, vi } from "vitest";
import { act, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { App } from "./App";
import { ApiError, request } from "./api";
import type { Ticket, User } from "./types";

const ticket: Ticket = {
  id: "ticket1",
  organizationId: "org1",
  ticketNo: "TCK-123",
  title: "Payment webhook failure",
  description: "Our payment webhook returns errors.",
  priority: "HIGH",
  status: "OPEN",
  requesterUserId: "customer1",
  assigneeUserId: null,
  createdAt: "2026-10-01T12:00:00Z",
  updatedAt: "2026-10-01T12:00:00Z",
};
const user: User = {
  userId: "user1",
  organizationId: "org1",
  fullName: "Alex Support",
  email: "alex@example.com",
  role: "AGENT",
};
const json = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json" },
  });
function setup(role: User["role"] = "AGENT") {
  let currentTicket = { ...ticket };
  const changes: Array<{ url: string; data: Record<string, unknown> }> = [];
  const fetchMock = vi.fn(
    async (url: string | URL | Request, options?: RequestInit) => {
      const path = String(url);
      if (
        options?.method === "PATCH" ||
        (options?.method === "POST" && !path.endsWith("/login"))
      ) {
        const data = JSON.parse(options.body as string);
        changes.push({ url: path, data });
        if (path.endsWith("/status"))
          currentTicket = { ...currentTicket, status: data.status };
        if (path.endsWith("/assign"))
          currentTicket = {
            ...currentTicket,
            assigneeUserId: data.assigneeUserId,
          };
        return json(currentTicket);
      }
      if (path.endsWith("/login"))
        return json({ accessToken: "memory-token", expiresInSeconds: 3600 });
      if (path.endsWith("/me")) return json({ ...user, role });
      if (path.endsWith("/agents"))
        return json([
          { id: "agent1", fullName: "Jamie Agent", email: "jamie@example.com" },
        ]);
      if (path.endsWith("/comments") || path.endsWith("/assignment-history"))
        return json([]);
      if (path.includes("/tickets?"))
        return json({
          content: [currentTicket],
          totalElements: 11,
          totalPages: 2,
          number: 0,
          size: 10,
        });
      return json(currentTicket);
    },
  );
  vi.stubGlobal("fetch", fetchMock);
  return { fetchMock, changes };
}
async function signIn() {
  const events = userEvent.setup();
  await events.type(screen.getByLabelText("Email address"), "alex@example.com");
  await events.type(screen.getByLabelText("Password"), "password123");
  await events.click(screen.getByRole("button", { name: "Sign in" }));
  await screen.findByRole("heading", { name: "Ticket operations" });
  return events;
}
async function openTicket(events: ReturnType<typeof userEvent.setup>) {
  await events.click(await screen.findByRole("button", { name: /TCK-123/ }));
  return screen.findByRole("region", { name: "Ticket details" });
}
describe("support desk workflows", () => {
  it("authenticates with me, shows identity, and clears the workspace on logout", async () => {
    const { fetchMock } = setup();
    render(<App />);
    const events = await signIn();
    expect(screen.getByText("Alex Support")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/auth/me",
      expect.objectContaining({
        headers: { Authorization: "Bearer memory-token" },
      }),
    );
    expect(localStorage.getItem("token")).toBeNull();
    await events.click(screen.getByRole("button", { name: "Sign out" }));
    expect(screen.getByRole("button", { name: "Sign in" })).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: "Ticket operations" }),
    ).not.toBeInTheDocument();
  });
  it("customers can reply publicly without staff actions or history requests", async () => {
    const { fetchMock, changes } = setup("CUSTOMER");
    render(<App />);
    const events = await signIn();
    const detail = await openTicket(events);
    await within(detail).findByRole("heading", { name: ticket.title });
    expect(
      within(detail).queryByLabelText("Next status"),
    ).not.toBeInTheDocument();
    expect(within(detail).queryByRole("checkbox")).not.toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(
        ([url]) =>
          String(url).endsWith("/assignment-history") ||
          String(url).endsWith("/agents"),
      ),
    ).toBe(false);
    await events.type(
      within(detail).getByLabelText("Reply"),
      "Please investigate.",
    );
    await events.click(
      within(detail).getByRole("button", { name: "Send reply" }),
    );
    await waitFor(() =>
      expect(changes[0]?.data).toEqual({
        body: "Please investigate.",
        internalNote: false,
      }),
    );
  });
  it("staff can perform allowed status changes and assign active team members", async () => {
    const { changes } = setup();
    render(<App />);
    const events = await signIn();
    const detail = await openTicket(events);
    const status = await within(detail).findByLabelText("Next status");
    expect(
      within(status).queryByRole("option", { name: "Resolved" }),
    ).not.toBeInTheDocument();
    await events.selectOptions(status, "IN_PROGRESS");
    await events.click(
      within(detail).getByRole("button", { name: "Update status" }),
    );
    await waitFor(() =>
      expect(changes[0]?.data).toEqual({ status: "IN_PROGRESS" }),
    );
    await waitFor(() =>
      expect(within(detail).getByLabelText("Next status")).not.toBeDisabled(),
    );
    await events.selectOptions(
      within(detail).getByLabelText("Assign team member"),
      "agent1",
    );
    await events.click(within(detail).getByRole("button", { name: "Assign" }));
    await waitFor(() =>
      expect(changes[1]?.data).toEqual({ assigneeUserId: "agent1" }),
    );
  });
  it("combines filters, debounces search, paginates, and resets page on filter change", async () => {
    const { fetchMock } = setup();
    render(<App />);
    const events = await signIn();
    await screen.findByRole("button", { name: /TCK-123/ });
    await events.click(screen.getByRole("button", { name: "Next" }));
    await waitFor(() =>
      expect(
        fetchMock.mock.calls.some(([url]) => String(url).includes("page=1")),
      ).toBe(true),
    );
    await events.selectOptions(screen.getByLabelText("Status"), "OPEN");
    await events.selectOptions(screen.getByLabelText("Priority"), "HIGH");
    await events.type(screen.getByLabelText("Search tickets"), "webhook");
    await waitFor(() =>
      expect(
        fetchMock.mock.calls.some(([url]) => {
          const parsed = new URL(String(url), "http://test");
          return (
            parsed.searchParams.get("q") === "webhook" &&
            parsed.searchParams.get("status") === "OPEN" &&
            parsed.searchParams.get("priority") === "HIGH" &&
            parsed.searchParams.get("page") === "0"
          );
        }),
      ).toBe(true),
    );
  });
  it("creates a validated request and opens its detail", async () => {
    const { changes } = setup();
    render(<App />);
    const events = await signIn();
    await events.click(screen.getByRole("button", { name: "+ New ticket" }));
    expect(
      screen.getByRole("button", { name: "Create ticket" }),
    ).toBeDisabled();
    await events.type(screen.getByLabelText("Title"), "Webhook outage");
    await events.type(
      screen.getByLabelText("Description"),
      "The payment service is unavailable.",
    );
    await events.click(screen.getByRole("button", { name: "Create ticket" }));
    await waitFor(() =>
      expect(changes[0]?.data).toEqual({
        title: "Webhook outage",
        description: "The payment service is unavailable.",
        priority: "MEDIUM",
      }),
    );
    expect(
      await screen.findByRole("region", { name: "Ticket details" }),
    ).toBeInTheDocument();
  });
  it("ignores an older queue response after filters change", async () => {
    const { fetchMock } = setup();
    render(<App />);
    const events = await signIn();
    await screen.findByRole("button", { name: /TCK-123/ });
    let resolveOld!: (response: Response) => void;
    let oldSignal: AbortSignal | null | undefined;
    fetchMock.mockImplementationOnce((_url, options) => {
      oldSignal = options?.signal;
      return new Promise<Response>((resolve) => {
        resolveOld = resolve;
      });
    });
    await events.selectOptions(screen.getByLabelText("Status"), "OPEN");
    await events.selectOptions(screen.getByLabelText("Priority"), "HIGH");
    await screen.findByRole("button", { name: /TCK-123/ });
    expect(oldSignal?.aborted).toBe(true);
    await act(async () =>
      resolveOld(
        json({
          content: [{ ...ticket, title: "Stale request" }],
          totalElements: 1,
          totalPages: 1,
          number: 0,
          size: 10,
        }),
      ),
    );
    expect(screen.queryByText("Stale request")).not.toBeInTheDocument();
  });
  it("returns to sign-in when an authenticated API request receives 401", async () => {
    const { fetchMock } = setup();
    render(<App />);
    await signIn();
    fetchMock.mockImplementationOnce(async () =>
      json({ message: "unauthorized" }, 401),
    );
    await userEvent.click(
      await screen.findByRole("button", { name: /TCK-123/ }),
    );
    expect(
      await screen.findByRole("button", { name: "Sign in" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Your session has expired",
    );
  });
  it("keeps login failures readable and does not open the workspace", async () => {
    setup();
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => json({}, 401)),
    );
    render(<App />);
    const events = userEvent.setup();
    await events.type(
      screen.getByLabelText("Email address"),
      "alex@example.com",
    );
    await events.type(screen.getByLabelText("Password"), "wrong");
    await events.click(screen.getByRole("button", { name: "Sign in" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Email or password is incorrect.",
    );
  });
});
describe("API errors", () => {
  it("preserves backend validation and gracefully handles a non-JSON proxy error", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(json({ message: "Invalid transition" }, 400))
        .mockResolvedValueOnce(new Response("Bad gateway", { status: 502 })),
    );
    await expect(request("/tickets", "token")).rejects.toThrow(
      "Invalid transition",
    );
    await expect(request("/tickets", "token")).rejects.toEqual(
      new ApiError("Request failed (502). Please try again.", 502),
    );
  });
});
