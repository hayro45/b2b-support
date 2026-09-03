export type TicketPriority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type TicketStatus = "OPEN" | "IN_PROGRESS" | "WAITING_CUSTOMER" | "RESOLVED" | "CLOSED";

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
