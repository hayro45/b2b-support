INSERT INTO tickets (
    id,
    organization_id,
    ticket_no,
    title,
    description,
    priority,
    status,
    requester_user_id,
    assignee_user_id
)
VALUES
(
    '10000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'TCK-2026-100001',
    'Payment callback timeout on checkout',
    'Customer reports intermittent payment callback timeout during peak traffic between 18:00 and 20:00.',
    'HIGH',
    'OPEN',
    '00000000-0000-0000-0000-000000000010',
    '00000000-0000-0000-0000-000000000011'
),
(
    '10000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'TCK-2026-100002',
    'Invoice PDF generation fails for TR locale',
    'Generated invoices return HTTP 500 when locale is tr-TR and line-item count is above 40.',
    'MEDIUM',
    'IN_PROGRESS',
    '00000000-0000-0000-0000-000000000010',
    '00000000-0000-0000-0000-000000000011'
),
(
    '10000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    'TCK-2026-100003',
    'Webhook signature mismatch for legacy client',
    'Legacy client integration started failing signature verification after key rotation.',
    'CRITICAL',
    'WAITING_CUSTOMER',
    '00000000-0000-0000-0000-000000000010',
    NULL
)
ON CONFLICT (id) DO NOTHING;
