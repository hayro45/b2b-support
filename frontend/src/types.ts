export type TicketPriority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export interface User {
  userId: string;
  organizationId: string;
  email: string;
  fullName: string;
  role: "CUSTOMER" | "AGENT" | "ADMIN";
}
export interface Agent {
  id: string;
  fullName: string;
  email: string;
}
export interface Assignment {
  id: string;
  fromUserId: string | null;
  toUserId: string;
  changedByUserId: string;
  changedAt: string;
}
export const priorities: TicketPriority[] = [
  "LOW",
  "MEDIUM",
  "HIGH",
  "CRITICAL",
];
export const statuses: TicketStatus[] = [
  "OPEN",
  "IN_PROGRESS",
  "WAITING_CUSTOMER",
  "RESOLVED",
  "CLOSED",
];
export const transitions: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ["IN_PROGRESS", "CLOSED"],
  IN_PROGRESS: ["WAITING_CUSTOMER", "RESOLVED", "CLOSED"],
  WAITING_CUSTOMER: ["IN_PROGRESS", "RESOLVED", "CLOSED"],
  RESOLVED: ["IN_PROGRESS", "CLOSED"],
  CLOSED: ["OPEN"],
};
export const label = (value: string) =>
  value
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/^./, (c) => c.toUpperCase());
export type TicketStatus =
  "OPEN" | "IN_PROGRESS" | "WAITING_CUSTOMER" | "RESOLVED" | "CLOSED";

export interface Ticket {
  id: string;
  organizationId: string;
  ticketNo: string;
  title: string;
  description: string;
  priority: TicketPriority;
  status: TicketStatus;
  requesterUserId: string | null;
  assigneeUserId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TicketPage {
  content: Ticket[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface TicketComment {
  id: string;
  ticketId: string;
  authorUserId: string;
  body: string;
  internalNote: boolean;
  createdAt: string;
}
