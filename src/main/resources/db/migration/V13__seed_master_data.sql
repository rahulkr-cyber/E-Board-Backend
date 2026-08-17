-- =====================================================================
-- V13: Seed master data (Phase 2)
-- =====================================================================

INSERT INTO master_letter_categories (id, code, name, description, active, created_at, deleted) VALUES
('70000000-0000-0000-0000-000000000001', 'GENERAL',        'General',         'General correspondence', TRUE, NOW(), FALSE),
('70000000-0000-0000-0000-000000000002', 'COURT_CASE',     'Court Case',      'Court case related correspondence', TRUE, NOW(), FALSE),
('70000000-0000-0000-0000-000000000003', 'VIP_REFERENCE',  'VIP Reference',   'VIP / MP / MLA references', TRUE, NOW(), FALSE),
('70000000-0000-0000-0000-000000000004', 'RTI',            'RTI',             'Right to Information requests', TRUE, NOW(), FALSE),
('70000000-0000-0000-0000-000000000005', 'REVENUE_APPEAL', 'Revenue Appeal',  'Revenue appeals and revisions', TRUE, NOW(), FALSE),
('70000000-0000-0000-0000-000000000006', 'LAND_RECORD',    'Land Record',     'Land record related matters', TRUE, NOW(), FALSE),
('70000000-0000-0000-0000-000000000007', 'CABINET',        'Cabinet',         'Cabinet and government references', TRUE, NOW(), FALSE),
('70000000-0000-0000-0000-000000000008', 'CONFIDENTIAL',   'Confidential',    'Confidential correspondence', TRUE, NOW(), FALSE),
('70000000-0000-0000-0000-000000000009', 'ESTABLISHMENT',  'Establishment',   'Establishment and service matters', TRUE, NOW(), FALSE);

INSERT INTO master_priorities (id, code, name, sort_order, sla_days, active, created_at, deleted) VALUES
('80000000-0000-0000-0000-000000000001', 'LOW',       'Low',       1, 30, TRUE, NOW(), FALSE),
('80000000-0000-0000-0000-000000000002', 'NORMAL',    'Normal',    2, 15, TRUE, NOW(), FALSE),
('80000000-0000-0000-0000-000000000003', 'HIGH',      'High',      3, 7,  TRUE, NOW(), FALSE),
('80000000-0000-0000-0000-000000000004', 'URGENT',    'Urgent',    4, 3,  TRUE, NOW(), FALSE),
('80000000-0000-0000-0000-000000000005', 'IMMEDIATE', 'Immediate', 5, 1,  TRUE, NOW(), FALSE);

INSERT INTO master_document_types (id, code, name, description, active, created_at, deleted) VALUES
('90000000-0000-0000-0000-000000000001', 'LETTER',       'Letter',       'Official letter', TRUE, NOW(), FALSE),
('90000000-0000-0000-0000-000000000002', 'ORDER',        'Order',        'Official order', TRUE, NOW(), FALSE),
('90000000-0000-0000-0000-000000000003', 'REPORT',       'Report',       'Report document', TRUE, NOW(), FALSE),
('90000000-0000-0000-0000-000000000004', 'NOTE',         'Note',         'Office note', TRUE, NOW(), FALSE),
('90000000-0000-0000-0000-000000000005', 'MEMO',         'Memo',         'Memorandum', TRUE, NOW(), FALSE),
('90000000-0000-0000-0000-000000000006', 'CIRCULAR',     'Circular',     'Circular', TRUE, NOW(), FALSE),
('90000000-0000-0000-0000-000000000007', 'NOTIFICATION', 'Notification', 'Gazette / official notification', TRUE, NOW(), FALSE),
('90000000-0000-0000-0000-000000000008', 'ANNEXURE',     'Annexure',     'Supporting annexure', TRUE, NOW(), FALSE);

INSERT INTO master_languages (id, code, name, active, created_at, deleted) VALUES
('a0000000-0000-0000-0000-000000000001', 'HI', 'Hindi',   TRUE, NOW(), FALSE),
('a0000000-0000-0000-0000-000000000002', 'EN', 'English', TRUE, NOW(), FALSE),
('a0000000-0000-0000-0000-000000000003', 'UR', 'Urdu',    TRUE, NOW(), FALSE);

INSERT INTO master_system_settings (id, setting_key, setting_value, description, created_at, deleted) VALUES
('b0000000-0000-0000-0000-000000000001', 'DIARY_NUMBER_PREFIX',    'BOR',  'Prefix used for diary number generation (BOR/{YEAR}/{SEQ})', NOW(), FALSE),
('b0000000-0000-0000-0000-000000000002', 'FILE_NUMBER_PREFIX',     'BOR/FILE', 'Prefix used for file number generation', NOW(), FALSE),
('b0000000-0000-0000-0000-000000000003', 'DISPATCH_NUMBER_PREFIX', 'BOR/DISPATCH', 'Prefix used for dispatch number generation', NOW(), FALSE),
('b0000000-0000-0000-0000-000000000004', 'DEFAULT_SLA_DAYS',       '15',   'Default SLA days when priority has no SLA', NOW(), FALSE),
('b0000000-0000-0000-0000-000000000005', 'REMINDER_LEAD_DAYS',     '3',    'Days before due date to raise a reminder', NOW(), FALSE),
('b0000000-0000-0000-0000-000000000006', 'MAX_UPLOAD_SIZE_MB',     '20',   'Maximum attachment size in MB', NOW(), FALSE);
