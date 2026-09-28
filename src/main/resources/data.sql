-- ============================================================
-- Test seed data for Job Copilot
-- Runs on every startup. Seeded tables are cleared first so
-- restarts don't cause duplicate key errors.
--
-- Intentionally NOT cleared (persists across restarts):
--   job_listings, fit_analyses, cvs  ← scheduler + user data
--
-- H2 Console: http://localhost:8084/h2-console
--   JDBC URL:  jdbc:h2:file:./data/jobcopilot
--   User:      sa  |  Password: (empty)
-- To wipe everything: delete the ./data/ folder and restart.
-- ============================================================

-- ── CLEAR SEEDED TABLES (safe to re-run on every restart) ────
DELETE FROM role_categories;

-- Demo users + school context. Uses a test-only hashed seed credential.
INSERT INTO allowed_emails (email)
VALUES ('demo.advisor@demo.edu')
ON CONFLICT (email) DO NOTHING;

INSERT INTO allowed_emails (email)
VALUES ('demo.student@demo.edu')
ON CONFLICT (email) DO NOTHING;

INSERT INTO allowed_emails (email)
VALUES ('maya.patel@demo.edu')
ON CONFLICT (email) DO NOTHING;

INSERT INTO allowed_emails (email)
VALUES
  ('alex.chen@demo.edu'),
  ('priya.shah@demo.edu'),
  ('jordan.lee@demo.edu'),
  ('emma.rodriguez@demo.edu'),
  ('noah.kim@demo.edu'),
  ('sophia.nguyen@demo.edu'),
  ('ethan.brooks@demo.edu'),
  ('aisha.khan@demo.edu')
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (id, email, password_hash, name, created_at)
VALUES (
  'aaaaaaaa-1111-1111-1111-111111111111',
  'demo.advisor@demo.edu',
  '$2a$10$kpbKCfi.3ObgQnO0UYT3MuHDgwSSxX.zH88Yj8.25PuxkpEQrphDS',
  'Demo Advisor',
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    name = EXCLUDED.name;

INSERT INTO users (id, email, password_hash, name, created_at)
VALUES (
  '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa',
  'demo.student@demo.edu',
  '$2a$10$kpbKCfi.3ObgQnO0UYT3MuHDgwSSxX.zH88Yj8.25PuxkpEQrphDS',
  'Riya Mehta',
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    name = EXCLUDED.name;

INSERT INTO users (id, email, password_hash, name, created_at)
VALUES (
  '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab',
  'maya.patel@demo.edu',
  '$2a$10$kpbKCfi.3ObgQnO0UYT3MuHDgwSSxX.zH88Yj8.25PuxkpEQrphDS',
  'Maya Patel',
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    name = EXCLUDED.name;

INSERT INTO users (id, email, password_hash, name, created_at)
VALUES
  ('73c62d58-0754-4e4b-a5ba-d7b0293fd4ac', 'alex.chen@demo.edu', '$2a$10$kpbKCfi.3ObgQnO0UYT3MuHDgwSSxX.zH88Yj8.25PuxkpEQrphDS', 'Alex Chen', NOW()),
  ('73c62d58-0754-4e4b-a5ba-d7b0293fd4ad', 'priya.shah@demo.edu', '$2a$10$kpbKCfi.3ObgQnO0UYT3MuHDgwSSxX.zH88Yj8.25PuxkpEQrphDS', 'Priya Shah', NOW()),
  ('73c62d58-0754-4e4b-a5ba-d7b0293fd4ae', 'jordan.lee@demo.edu', '$2a$10$kpbKCfi.3ObgQnO0UYT3MuHDgwSSxX.zH88Yj8.25PuxkpEQrphDS', 'Jordan Lee', NOW()),
  ('73c62d58-0754-4e4b-a5ba-d7b0293fd4af', 'emma.rodriguez@demo.edu', '$2a$10$kpbKCfi.3ObgQnO0UYT3MuHDgwSSxX.zH88Yj8.25PuxkpEQrphDS', 'Emma Rodriguez', NOW()),
  ('73c62d58-0754-4e4b-a5ba-d7b0293fd4b0', 'noah.kim@demo.edu', '$2a$10$kpbKCfi.3ObgQnO0UYT3MuHDgwSSxX.zH88Yj8.25PuxkpEQrphDS', 'Noah Kim', NOW()),
  ('73c62d58-0754-4e4b-a5ba-d7b0293fd4b1', 'sophia.nguyen@demo.edu', '$2a$10$kpbKCfi.3ObgQnO0UYT3MuHDgwSSxX.zH88Yj8.25PuxkpEQrphDS', 'Sophia Nguyen', NOW()),
  ('73c62d58-0754-4e4b-a5ba-d7b0293fd4b2', 'ethan.brooks@demo.edu', '$2a$10$kpbKCfi.3ObgQnO0UYT3MuHDgwSSxX.zH88Yj8.25PuxkpEQrphDS', 'Ethan Brooks', NOW()),
  ('73c62d58-0754-4e4b-a5ba-d7b0293fd4b3', 'aisha.khan@demo.edu', '$2a$10$kpbKCfi.3ObgQnO0UYT3MuHDgwSSxX.zH88Yj8.25PuxkpEQrphDS', 'Aisha Khan', NOW())
ON CONFLICT (id) DO UPDATE
SET email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    name = EXCLUDED.name;

INSERT INTO schools (id, name, slug, domain, is_active, created_at)
VALUES (
  '11111111-1111-1111-1111-111111111111',
  'Demo University',
  'demo-university',
  'demo.edu',
  TRUE,
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    slug = EXCLUDED.slug,
    domain = EXCLUDED.domain,
    is_active = EXCLUDED.is_active;

INSERT INTO programs (id, school_id, name, degree_type, is_active, created_at)
VALUES (
  '22222222-2222-2222-2222-222222222222',
  '11111111-1111-1111-1111-111111111111',
  'MS Business Analytics',
  'MS',
  TRUE,
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET school_id = EXCLUDED.school_id,
    name = EXCLUDED.name,
    degree_type = EXCLUDED.degree_type,
    is_active = EXCLUDED.is_active;

INSERT INTO cohorts (id, program_id, name, start_date, end_date, expected_graduation_date, is_active, created_at)
VALUES (
  '33333333-3333-3333-3333-333333333333',
  '22222222-2222-2222-2222-222222222222',
  'Fall 2026',
  DATE '2026-08-24',
  DATE '2027-05-15',
  DATE '2027-05-15',
  TRUE,
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET program_id = EXCLUDED.program_id,
    name = EXCLUDED.name,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    expected_graduation_date = EXCLUDED.expected_graduation_date,
    is_active = EXCLUDED.is_active;

INSERT INTO advisors (id, user_id, name, email, school_id, title, role, is_active, created_at)
VALUES (
  '44444444-4444-4444-4444-444444444444',
  'aaaaaaaa-1111-1111-1111-111111111111',
  'Demo Advisor',
  'demo.advisor@demo.edu',
  '11111111-1111-1111-1111-111111111111',
  'Career Advisor',
  'SCHOOL_ADMIN',
  TRUE,
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    name = EXCLUDED.name,
    email = EXCLUDED.email,
    school_id = EXCLUDED.school_id,
    title = EXCLUDED.title,
    role = EXCLUDED.role,
    is_active = EXCLUDED.is_active;

INSERT INTO student_school_affiliations (id, user_id, school_id, program_id, cohort_id, student_identifier, status, job_search_status, job_search_status_updated_at, created_at, updated_at)
VALUES (
  '55555555-5555-5555-5555-555555555555',
  '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa',
  '11111111-1111-1111-1111-111111111111',
  '22222222-2222-2222-2222-222222222222',
  '33333333-3333-3333-3333-333333333333',
  'DEMO-STUDENT-001',
  'ACTIVE',
  'ACTIVE',
  NOW(),
  NOW(),
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    school_id = EXCLUDED.school_id,
    program_id = EXCLUDED.program_id,
    cohort_id = EXCLUDED.cohort_id,
    student_identifier = EXCLUDED.student_identifier,
    status = EXCLUDED.status,
    job_search_status = EXCLUDED.job_search_status,
    job_search_status_updated_at = EXCLUDED.job_search_status_updated_at,
    updated_at = NOW();

INSERT INTO student_school_affiliations (id, user_id, school_id, program_id, cohort_id, student_identifier, status, job_search_status, job_search_status_updated_at, created_at, updated_at)
VALUES (
  '55555555-5555-5555-5555-555555555556',
  '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab',
  '11111111-1111-1111-1111-111111111111',
  '22222222-2222-2222-2222-222222222222',
  '33333333-3333-3333-3333-333333333333',
  'DEMO-STUDENT-002',
  'ACTIVE',
  'ACTIVE',
  NOW(),
  NOW(),
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    school_id = EXCLUDED.school_id,
    program_id = EXCLUDED.program_id,
    cohort_id = EXCLUDED.cohort_id,
    student_identifier = EXCLUDED.student_identifier,
    status = EXCLUDED.status,
    job_search_status = EXCLUDED.job_search_status,
    job_search_status_updated_at = EXCLUDED.job_search_status_updated_at,
    updated_at = NOW();

INSERT INTO student_consents (id, affiliation_id, user_id, advisor_visibility_enabled, advisor_visibility_level, consented_at, revoked_at, created_at, updated_at)
VALUES (
  '66666666-6666-6666-6666-666666666666',
  '55555555-5555-5555-5555-555555555555',
  '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa',
  TRUE,
  'FULL',
  NOW(),
  NULL,
  NOW(),
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET affiliation_id = EXCLUDED.affiliation_id,
    user_id = EXCLUDED.user_id,
    advisor_visibility_enabled = EXCLUDED.advisor_visibility_enabled,
    advisor_visibility_level = EXCLUDED.advisor_visibility_level,
    consented_at = EXCLUDED.consented_at,
    revoked_at = EXCLUDED.revoked_at,
    updated_at = NOW();

INSERT INTO student_school_affiliations (id, user_id, school_id, program_id, cohort_id, student_identifier, status, job_search_status, job_search_status_updated_at, created_at, updated_at)
VALUES
  ('55555555-5555-5555-5555-555555555557', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ac', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'DEMO-STUDENT-003', 'ACTIVE', 'ACTIVE', NOW(), NOW(), NOW()),
  ('55555555-5555-5555-5555-555555555558', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ad', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'DEMO-STUDENT-004', 'ACTIVE', 'ACTIVE', NOW(), NOW(), NOW()),
  ('55555555-5555-5555-5555-555555555559', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ae', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'DEMO-STUDENT-005', 'ACTIVE', 'ACTIVE', NOW(), NOW(), NOW()),
  ('55555555-5555-5555-5555-55555555555a', '73c62d58-0754-4e4b-a5ba-d7b0293fd4af', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'DEMO-STUDENT-006', 'ACTIVE', 'ACTIVE', NOW(), NOW(), NOW()),
  ('55555555-5555-5555-5555-55555555555b', '73c62d58-0754-4e4b-a5ba-d7b0293fd4b0', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'DEMO-STUDENT-007', 'ACTIVE', 'ACTIVE', NOW(), NOW(), NOW()),
  ('55555555-5555-5555-5555-55555555555c', '73c62d58-0754-4e4b-a5ba-d7b0293fd4b1', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'DEMO-STUDENT-008', 'ACTIVE', 'ACTIVE', NOW(), NOW(), NOW()),
  ('55555555-5555-5555-5555-55555555555d', '73c62d58-0754-4e4b-a5ba-d7b0293fd4b2', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'DEMO-STUDENT-009', 'ACTIVE', 'LANDED', NOW(), NOW(), NOW()),
  ('55555555-5555-5555-5555-55555555555e', '73c62d58-0754-4e4b-a5ba-d7b0293fd4b3', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'DEMO-STUDENT-010', 'ACTIVE', 'ACTIVE', NOW(), NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    school_id = EXCLUDED.school_id,
    program_id = EXCLUDED.program_id,
    cohort_id = EXCLUDED.cohort_id,
    student_identifier = EXCLUDED.student_identifier,
    status = EXCLUDED.status,
    job_search_status = EXCLUDED.job_search_status,
    job_search_status_updated_at = EXCLUDED.job_search_status_updated_at,
    updated_at = NOW();

INSERT INTO student_consents (id, affiliation_id, user_id, advisor_visibility_enabled, advisor_visibility_level, consented_at, revoked_at, created_at, updated_at)
VALUES (
  '66666666-6666-6666-6666-666666666667',
  '55555555-5555-5555-5555-555555555556',
  '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab',
  TRUE,
  'LIMITED',
  NOW(),
  NULL,
  NOW(),
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET affiliation_id = EXCLUDED.affiliation_id,
    user_id = EXCLUDED.user_id,
    advisor_visibility_enabled = EXCLUDED.advisor_visibility_enabled,
    advisor_visibility_level = EXCLUDED.advisor_visibility_level,
    consented_at = EXCLUDED.consented_at,
    revoked_at = EXCLUDED.revoked_at,
    updated_at = NOW();

-- ── SEED JOB APPLICATIONS ────────────────────────────────────
INSERT INTO student_consents (id, affiliation_id, user_id, advisor_visibility_enabled, advisor_visibility_level, consented_at, revoked_at, created_at, updated_at)
VALUES
  ('66666666-6666-6666-6666-666666666668', '55555555-5555-5555-5555-555555555557', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ac', TRUE, 'FULL', NOW(), NULL, NOW(), NOW()),
  ('66666666-6666-6666-6666-666666666669', '55555555-5555-5555-5555-555555555558', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ad', TRUE, 'FULL', NOW(), NULL, NOW(), NOW()),
  ('66666666-6666-6666-6666-66666666666a', '55555555-5555-5555-5555-555555555559', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ae', TRUE, 'LIMITED', NOW(), NULL, NOW(), NOW()),
  ('66666666-6666-6666-6666-66666666666b', '55555555-5555-5555-5555-55555555555a', '73c62d58-0754-4e4b-a5ba-d7b0293fd4af', TRUE, 'FULL', NOW(), NULL, NOW(), NOW()),
  ('66666666-6666-6666-6666-66666666666c', '55555555-5555-5555-5555-55555555555b', '73c62d58-0754-4e4b-a5ba-d7b0293fd4b0', TRUE, 'LIMITED', NOW(), NULL, NOW(), NOW()),
  ('66666666-6666-6666-6666-66666666666d', '55555555-5555-5555-5555-55555555555c', '73c62d58-0754-4e4b-a5ba-d7b0293fd4b1', TRUE, 'FULL', NOW(), NULL, NOW(), NOW()),
  ('66666666-6666-6666-6666-66666666666e', '55555555-5555-5555-5555-55555555555d', '73c62d58-0754-4e4b-a5ba-d7b0293fd4b2', TRUE, 'LIMITED', NOW(), NULL, NOW(), NOW()),
  ('66666666-6666-6666-6666-66666666666f', '55555555-5555-5555-5555-55555555555e', '73c62d58-0754-4e4b-a5ba-d7b0293fd4b3', TRUE, 'FULL', NOW(), NULL, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET affiliation_id = EXCLUDED.affiliation_id,
    user_id = EXCLUDED.user_id,
    advisor_visibility_enabled = EXCLUDED.advisor_visibility_enabled,
    advisor_visibility_level = EXCLUDED.advisor_visibility_level,
    consented_at = EXCLUDED.consented_at,
    revoked_at = EXCLUDED.revoked_at,
    updated_at = NOW();

-- ── SEED JOB APPLICATIONS ────────────────────────────────────
-- Fixed UUIDs + ON CONFLICT DO NOTHING = safe to re-run on every restart.
-- userId: 73c62d58-0754-4e4b-a5ba-d7b0293fd4aa
-- 20 apps across all statuses, spread over 4 weeks for velocity chart.
-- 2 stale Applied apps (>30 days) for ghosting rate.

-- Week 4 (last 7 days) — 5 apps
INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000001', 'Google', 'Software Engineer', 'Alice Brown', 'alice@google.com', 'Applied', 'No', 'Software Engineering', NULL, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000002', 'Meta', 'Product Manager', 'James Lee', 'james@meta.com', 'Interview', 'Yes', 'Product Management', NOW() + INTERVAL '3 days', NOW() - INTERVAL '5 days', NOW() - INTERVAL '1 day', NULL, NOW() - INTERVAL '1 day', NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000003', 'Stripe', 'Data Engineer', 'Sara Kim', 'sara@stripe.com', 'Applied', 'No', 'Data & Analytics', NULL, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000004', 'Anthropic', 'Senior Product Manager', 'Claire Zhang', 'claire@anthropic.com', 'Interview', 'Yes', 'Product Management', NOW() + INTERVAL '5 days', NOW() - INTERVAL '6 days', NOW() - INTERVAL '2 days', NULL, NOW() - INTERVAL '2 days', NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000005', 'OpenAI', 'Software Engineer', 'David Park', 'david@openai.com', 'Referral Received', 'Yes', 'Software Engineering', NULL, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

-- Week 3 (7–14 days ago) — 5 apps
INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000006', 'Figma', 'Product Designer', 'Dan Park', 'dan@figma.com', 'Offer', 'Yes', 'Design & UX', NULL, NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 days', NULL, NOW() - INTERVAL '4 days', NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000007', 'Notion', 'Senior Engineer', 'Priya Patel', 'priya@notion.so', 'Interview', 'No', 'Software Engineering', NOW() + INTERVAL '7 days', NOW() - INTERVAL '10 days', NOW() - INTERVAL '6 days', NULL, NOW() - INTERVAL '6 days', NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000008', 'Databricks', 'Data Analyst', 'Laura White', 'laura@databricks.com', 'Applied', 'No', 'Data & Analytics', NULL, NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000009', 'Salesforce', 'Platform Developer', 'Ethan Lewis', 'ethan@salesforce.com', 'Rejected', 'No', 'Software Engineering', NULL, NOW() - INTERVAL '12 days', NOW() - INTERVAL '8 days', NULL, NOW() - INTERVAL '8 days', NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000010', 'Brex', 'Backend Engineer', 'Noah Davis', 'noah@brex.com', 'Interview', 'Yes', 'Software Engineering', NOW() + INTERVAL '4 days', NOW() - INTERVAL '13 days', NOW() - INTERVAL '9 days', NULL, NOW() - INTERVAL '9 days', NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

-- Week 2 (14–21 days ago) — 5 apps
INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000011', 'Airbnb', 'Platform Engineer', 'Tom Chen', 'tom@airbnb.com', 'Rejected', 'Yes', 'Software Engineering', NULL, NOW() - INTERVAL '15 days', NOW() - INTERVAL '11 days', NULL, NOW() - INTERVAL '11 days', NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000012', 'Rippling', 'Software Engineer', 'Emma Clark', 'emma@rippling.com', 'Closed', 'No', 'Software Engineering', NULL, NOW() - INTERVAL '16 days', NOW() - INTERVAL '10 days', NULL, NOW() - INTERVAL '12 days', NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000013', 'Linear', 'Software Engineer', 'Liam Wilson', 'liam@linear.app', 'Referral Received', 'Yes', 'Software Engineering', NULL, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000014', 'Vercel', 'Infrastructure Engineer', 'Ava Johnson', 'ava@vercel.com', 'Offer', 'No', 'Software Engineering', NULL, NOW() - INTERVAL '20 days', NOW() - INTERVAL '5 days', NULL, NOW() - INTERVAL '14 days', NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000015', 'Palantir', 'Forward Deployed Engineer', 'Mike Stone', 'mike@palantir.com', 'Rejected', 'No', 'Strategy & Operations', NULL, NOW() - INTERVAL '21 days', NOW() - INTERVAL '14 days', NULL, NOW() - INTERVAL '16 days', NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

-- Week 1 (21–28 days ago) — 3 apps + 2 stale Applied (ghosting rate)
INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000016', 'Amazon', 'SDE II', 'Rachel Adams', 'rachel@amazon.com', 'Rejected', 'No', 'Software Engineering', NULL, NOW() - INTERVAL '22 days', NOW() - INTERVAL '16 days', NULL, NOW() - INTERVAL '18 days', NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000017', 'Microsoft', 'Software Engineer II', 'Kevin Moore', 'kevin@microsoft.com', 'Closed', 'Yes', 'Software Engineering', NULL, NOW() - INTERVAL '25 days', NOW() - INTERVAL '18 days', NULL, NOW() - INTERVAL '19 days', NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000018', 'Snowflake', 'Data Platform Engineer', 'Jack Scott', 'jack@snowflake.com', 'Applied', 'No', 'Data & Analytics', NULL, NOW() - INTERVAL '27 days', NOW() - INTERVAL '27 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

-- Stale Applied — no update in 35 days (triggers ghosting rate)
INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000019', 'Apple', 'iOS Engineer', 'Sophie Turner', 'sophie@apple.com', 'Applied', 'No', 'Software Engineering', NULL, NOW() - INTERVAL '35 days', NOW() - INTERVAL '35 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('bbbbbbbb-0000-0000-0000-000000000020', 'Uber', 'Software Engineer', 'Ben Harris', 'ben@uber.com', 'Applied', 'No', 'Software Engineering', NULL, NOW() - INTERVAL '35 days', NOW() - INTERVAL '35 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO NOTHING;

-- Additional advisor demo student: active consulting search with high activity and no responses.
-- userId: 73c62d58-0754-4e4b-a5ba-d7b0293fd4ab
INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000001', 'McKinsey & Company', 'Business Analyst', NULL, NULL, 'Applied', 'No', 'Consulting', NULL, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000002', 'Bain & Company', 'Associate Consultant', NULL, NULL, 'Applied', 'No', 'Consulting', NULL, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000003', 'Boston Consulting Group', 'Associate', NULL, NULL, 'Applied', 'No', 'Consulting', NULL, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000004', 'Deloitte', 'Strategy Consultant', NULL, NULL, 'Applied', 'No', 'Consulting', NULL, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000005', 'PwC', 'Management Consultant', NULL, NULL, 'Applied', 'No', 'Consulting', NULL, NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000006', 'EY-Parthenon', 'Strategy Associate', NULL, NULL, 'Applied', 'No', 'Consulting', NULL, NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000007', 'Kearney', 'Business Analyst', NULL, NULL, 'Applied', 'No', 'Consulting', NULL, NOW() - INTERVAL '13 days', NOW() - INTERVAL '13 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000008', 'Oliver Wyman', 'Consultant', NULL, NULL, 'Applied', 'No', 'Consulting', NULL, NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000009', 'Accenture Strategy', 'Strategy Analyst', NULL, NULL, 'Applied', 'No', 'Consulting', NULL, NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000010', 'LEK Consulting', 'Associate Consultant', NULL, NULL, 'Applied', 'No', 'Consulting', NULL, NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000011', 'Roland Berger', 'Junior Consultant', NULL, NULL, 'Applied', 'No', 'Consulting', NULL, NOW() - INTERVAL '22 days', NOW() - INTERVAL '22 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000012', 'Strategy&', 'Strategy Consultant', NULL, NULL, 'Applied', 'No', 'Consulting', NULL, NOW() - INTERVAL '24 days', NOW() - INTERVAL '24 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

-- Maya test case: recent volume plus three interviews and no offers.
-- This should produce two support signals: high activity/no responses and interviews/no offers.
INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000013', 'Vertex Partners', 'Associate Consultant', NULL, NULL, 'Interview', 'No', 'Consulting', NOW() + INTERVAL '4 days', NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000014', 'Northstar Advisory', 'Strategy Consultant', NULL, NULL, 'Interview', 'No', 'Consulting', NOW() + INTERVAL '6 days', NOW() - INTERVAL '6 days', NOW() - INTERVAL '5 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES ('cccccccc-0000-0000-0000-000000000015', 'BlueOak Consulting', 'Business Analyst', NULL, NULL, 'Interview', 'No', 'Consulting', NOW() + INTERVAL '8 days', NOW() - INTERVAL '9 days', NOW() - INTERVAL '8 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab')
ON CONFLICT (id) DO NOTHING;

-- ── DEMO APPLICATION DISTRIBUTION ─────────────────────────────
-- Redistribute fixed demo applications across the advisor cohort.
-- This runs after inserts so existing local databases are reshaped on restart too.
UPDATE job_applications
SET user_id = '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'
WHERE id IN (
  'bbbbbbbb-0000-0000-0000-000000000001',
  'bbbbbbbb-0000-0000-0000-000000000006',
  'bbbbbbbb-0000-0000-0000-000000000014'
);

UPDATE job_applications
SET user_id = '73c62d58-0754-4e4b-a5ba-d7b0293fd4ac'
WHERE id IN (
  'bbbbbbbb-0000-0000-0000-000000000005',
  'bbbbbbbb-0000-0000-0000-000000000007',
  'bbbbbbbb-0000-0000-0000-000000000009',
  'bbbbbbbb-0000-0000-0000-000000000011',
  'bbbbbbbb-0000-0000-0000-000000000012'
);

UPDATE job_applications
SET user_id = '73c62d58-0754-4e4b-a5ba-d7b0293fd4ad'
WHERE id IN (
  'bbbbbbbb-0000-0000-0000-000000000002',
  'bbbbbbbb-0000-0000-0000-000000000004',
  'bbbbbbbb-0000-0000-0000-000000000010'
);

UPDATE job_applications
SET user_id = '73c62d58-0754-4e4b-a5ba-d7b0293fd4ae'
WHERE id IN (
  'bbbbbbbb-0000-0000-0000-000000000003',
  'bbbbbbbb-0000-0000-0000-000000000008',
  'bbbbbbbb-0000-0000-0000-000000000018'
);

UPDATE job_applications
SET user_id = '73c62d58-0754-4e4b-a5ba-d7b0293fd4b1'
WHERE id IN (
  'bbbbbbbb-0000-0000-0000-000000000013',
  'bbbbbbbb-0000-0000-0000-000000000016',
  'bbbbbbbb-0000-0000-0000-000000000017'
);

UPDATE job_applications
SET user_id = '73c62d58-0754-4e4b-a5ba-d7b0293fd4b2',
    application_status = 'Offer',
    first_responded_at = COALESCE(first_responded_at, NOW() - INTERVAL '10 days')
WHERE id = 'bbbbbbbb-0000-0000-0000-000000000015';

UPDATE job_applications
SET user_id = '73c62d58-0754-4e4b-a5ba-d7b0293fd4b3'
WHERE id IN (
  'bbbbbbbb-0000-0000-0000-000000000019',
  'bbbbbbbb-0000-0000-0000-000000000020'
);

UPDATE job_applications
SET user_id = '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab'
WHERE id IN (
  'cccccccc-0000-0000-0000-000000000001',
  'cccccccc-0000-0000-0000-000000000002',
  'cccccccc-0000-0000-0000-000000000003',
  'cccccccc-0000-0000-0000-000000000004',
  'cccccccc-0000-0000-0000-000000000005',
  'cccccccc-0000-0000-0000-000000000006',
  'cccccccc-0000-0000-0000-000000000007',
  'cccccccc-0000-0000-0000-000000000008',
  'cccccccc-0000-0000-0000-000000000009',
  'cccccccc-0000-0000-0000-000000000010'
);

UPDATE job_applications
SET user_id = '73c62d58-0754-4e4b-a5ba-d7b0293fd4af'
WHERE id IN (
  'cccccccc-0000-0000-0000-000000000011',
  'cccccccc-0000-0000-0000-000000000012'
);

-- ── STUDENT INTELLIGENCE DEMO DATA ───────────────────────────
-- Keep the intelligence demo deterministic. These tables are global enough that old
-- experiment rows make the review UI hard to reason about.
DELETE FROM student_insights
WHERE insight_type IN ('RECURRING_CAPABILITY_GAP', 'LOW_FIT_TARGETING', 'ROLE_CATEGORY_FIT_COMPARISON', 'ROLE_CATEGORY_TRACTION');

DELETE FROM gap_mentions
WHERE user_id IN (
  '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa',
  '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab',
  '73c62d58-0754-4e4b-a5ba-d7b0293fd4ac',
  '73c62d58-0754-4e4b-a5ba-d7b0293fd4ad'
);

DELETE FROM capability_concept_aliases;
DELETE FROM capability_concepts;

-- Riya Mehta has enough structured evidence to show student-facing intelligence.
INSERT INTO fit_analyses (id, company, job_title, job_description_text, cv_id, fit_score, recommendation, confidence, weightage_reasoning, analyzed_at, user_id)
VALUES
  ('eeeeeeee-0000-0000-0000-000000000001', 'Northstar AI', 'Chief of Staff', 'Own CEO operating cadence, board materials, investor updates, and cross-functional special projects.', NULL, 52, 'Ignore', 'High', 'Role is heavily executive-facing and requires senior operating experience.', NOW() - INTERVAL '18 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('eeeeeeee-0000-0000-0000-000000000002', 'Brightline SaaS', 'Strategy & Operations Associate', 'Prepare leadership updates, build operating dashboards, and drive special projects across GTM teams.', NULL, 58, 'Ignore', 'High', 'Role requires leadership-ready communication and GTM operating analytics.', NOW() - INTERVAL '14 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('eeeeeeee-0000-0000-0000-000000000003', 'OrbitCloud', 'Business Operations Manager', 'Build KPI dashboards, synthesize executive decisions, and improve operating cadence for customer success.', NULL, 61, 'Optimize & Apply', 'Medium', 'Candidate has analytics foundation but lacks visible executive-facing ownership.', NOW() - INTERVAL '10 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('eeeeeeee-0000-0000-0000-000000000004', 'SignalWorks', 'Founder Associate', 'Support founder with board prep, investor memo writing, strategic planning, and ambiguous special projects.', NULL, 54, 'Ignore', 'High', 'Role expects founder leverage and senior communication artifacts.', NOW() - INTERVAL '6 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('eeeeeeee-0000-0000-0000-000000000005', 'HelioHealth', 'Strategic Operations Analyst', 'Analyze operating metrics, write leadership memos, and coordinate cross-functional execution rhythms.', NULL, 57, 'Ignore', 'Medium', 'Role combines analytics with executive synthesis and operating ownership.', NOW() - INTERVAL '3 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('eeeeeeee-0000-0000-0000-000000000006', 'Vertex Labs', 'Operations Lead', 'Own operating reviews, cross-functional planning, and executive reporting for a growing technology business.', NULL, 78, 'Optimize & Apply', 'Medium', 'Earlier role family showed stronger alignment with the current CV evidence.', NOW() - INTERVAL '45 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('eeeeeeee-0000-0000-0000-000000000007', 'Cedar Health', 'Business Operations Associate', 'Build planning processes, analyze business performance, and coordinate leadership priorities.', NULL, 74, 'Optimize & Apply', 'Medium', 'Earlier role had a stronger match across operating analytics and execution.', NOW() - INTERVAL '52 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('eeeeeeee-0000-0000-0000-000000000008', 'Atlas Commerce', 'Strategy Associate', 'Support strategic planning, business analysis, and leadership decision-making across growth initiatives.', NULL, 76, 'Optimize & Apply', 'Medium', 'Earlier strategy role aligned well with the existing analytical and planning evidence.', NOW() - INTERVAL '60 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('eeeeeeee-0000-0000-0000-000000000009', 'Nimbus Product', 'Associate Product Manager', 'Define product priorities, analyze user needs, and coordinate roadmap decisions with cross-functional teams.', NULL, 48, 'Ignore', 'High', 'The role requires more direct product ownership than the CV currently demonstrates.', NOW() - INTERVAL '42 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('eeeeeeee-0000-0000-0000-000000000010', 'Harbor Product', 'Product Analyst', 'Analyze product metrics, support roadmap planning, and translate user research into recommendations.', NULL, 52, 'Ignore', 'Medium', 'Product analytics evidence is present, but direct product decision-making is limited.', NOW() - INTERVAL '38 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('eeeeeeee-0000-0000-0000-000000000011', 'SparkPM', 'Product Operations Associate', 'Coordinate product launches, improve product workflows, and support prioritization across teams.', NULL, 56, 'Optimize & Apply', 'Medium', 'The role is closer to the current profile but still expects clearer product operations ownership.', NOW() - INTERVAL '16 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO UPDATE
SET company = EXCLUDED.company,
    job_title = EXCLUDED.job_title,
    job_description_text = EXCLUDED.job_description_text,
    fit_score = EXCLUDED.fit_score,
    recommendation = EXCLUDED.recommendation,
    confidence = EXCLUDED.confidence,
    weightage_reasoning = EXCLUDED.weightage_reasoning,
    analyzed_at = EXCLUDED.analyzed_at,
    user_id = EXCLUDED.user_id;

-- Additional applied-role analyses for cohort intelligence testing.
INSERT INTO fit_analyses (id, company, job_title, job_description_text, cv_id, fit_score, recommendation, confidence, weightage_reasoning, analyzed_at, user_id)
VALUES
  ('ffffffff-0000-0000-0000-000000000001', 'McKinsey & Company', 'Business Analyst', 'Solve ambiguous client problems with structured issue trees, market sizing, and executive-ready recommendations.', NULL, 63, 'Optimize & Apply', 'Medium', 'Strong analytics base, but case structuring evidence is thin.', NOW() - INTERVAL '19 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab'),
  ('ffffffff-0000-0000-0000-000000000002', 'Bain & Company', 'Associate Consultant', 'Build hypothesis-driven workplans, synthesize client interviews, and size market opportunities.', NULL, 59, 'Ignore', 'High', 'Role expects clearer consulting problem structuring.', NOW() - INTERVAL '16 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab'),
  ('ffffffff-0000-0000-0000-000000000003', 'Boston Consulting Group', 'Associate', 'Frame ambiguous business cases, quantify market opportunities, and communicate recommendations to partners.', NULL, 57, 'Ignore', 'High', 'Case framing and market sizing are not explicit enough.', NOW() - INTERVAL '12 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab'),
  ('ffffffff-0000-0000-0000-000000000004', 'Deloitte', 'Strategy Consultant', 'Structure client workstreams, build market models, and convert stakeholder inputs into recommendations.', NULL, 62, 'Optimize & Apply', 'Medium', 'Client-ready synthesis is present, but consulting artifacts are not named.', NOW() - INTERVAL '8 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab'),
  ('ffffffff-0000-0000-0000-000000000101', 'OpenAI', 'Software Engineer', 'Design reliable backend services, own observability, and debug distributed production systems.', NULL, 68, 'Optimize & Apply', 'Medium', 'Engineering foundation is present, production reliability evidence is lighter.', NOW() - INTERVAL '13 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ac'),
  ('ffffffff-0000-0000-0000-000000000102', 'Notion', 'Senior Engineer', 'Own backend systems, incident response, observability, and cross-team technical design.', NULL, 64, 'Optimize & Apply', 'Medium', 'The role expects more explicit production ownership.', NOW() - INTERVAL '9 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ac'),
  ('ffffffff-0000-0000-0000-000000000201', 'Meta', 'Product Manager', 'Own roadmap tradeoffs, define product metrics, and drive discovery through user research.', NULL, 60, 'Optimize & Apply', 'Medium', 'Analytical background is strong, but roadmap ownership is light.', NOW() - INTERVAL '15 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ad'),
  ('ffffffff-0000-0000-0000-000000000202', 'Anthropic', 'Senior Product Manager', 'Lead roadmap prioritization, user discovery, launch planning, and cross-functional product decisions.', NULL, 56, 'Ignore', 'High', 'Role expects clearer end-to-end PM ownership.', NOW() - INTERVAL '7 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ad')
ON CONFLICT (id) DO UPDATE
SET company = EXCLUDED.company,
    job_title = EXCLUDED.job_title,
    job_description_text = EXCLUDED.job_description_text,
    fit_score = EXCLUDED.fit_score,
    recommendation = EXCLUDED.recommendation,
    confidence = EXCLUDED.confidence,
    weightage_reasoning = EXCLUDED.weightage_reasoning,
    analyzed_at = EXCLUDED.analyzed_at,
    user_id = EXCLUDED.user_id;

DELETE FROM fit_analysis_gaps
WHERE fit_analysis_id IN (
  'eeeeeeee-0000-0000-0000-000000000001',
  'eeeeeeee-0000-0000-0000-000000000002',
  'eeeeeeee-0000-0000-0000-000000000003',
  'eeeeeeee-0000-0000-0000-000000000004',
  'eeeeeeee-0000-0000-0000-000000000005'
);

INSERT INTO fit_analysis_gaps (fit_analysis_id, gap, category, severity)
VALUES
  ('eeeeeeee-0000-0000-0000-000000000001', 'No evidence of preparing board materials for senior leaders', 'Experience Depth', 'High'),
  ('eeeeeeee-0000-0000-0000-000000000001', 'Limited ownership of ambiguous cross-functional operating projects', 'Seniority', 'Medium'),
  ('eeeeeeee-0000-0000-0000-000000000002', 'Leadership updates and executive-ready synthesis are not visible in the CV', 'Skills', 'High'),
  ('eeeeeeee-0000-0000-0000-000000000002', 'GTM KPI dashboarding experience is not clearly shown', 'Skills', 'Medium'),
  ('eeeeeeee-0000-0000-0000-000000000003', 'Executive decision synthesis is weaker than the role expects', 'Experience Depth', 'High'),
  ('eeeeeeee-0000-0000-0000-000000000003', 'Customer success operating metrics are not clearly demonstrated', 'Domain', 'Medium'),
  ('eeeeeeee-0000-0000-0000-000000000004', 'No clear examples of investor memo or board-prep writing', 'Skills', 'High'),
  ('eeeeeeee-0000-0000-0000-000000000004', 'Founder-facing special projects ownership is not evident', 'Seniority', 'Medium'),
  ('eeeeeeee-0000-0000-0000-000000000005', 'Leadership memo writing is not explicit in the CV', 'Skills', 'High'),
  ('eeeeeeee-0000-0000-0000-000000000005', 'Cross-functional operating cadence ownership is limited', 'Experience Depth', 'Medium');

DELETE FROM gap_mentions
WHERE fit_analysis_id IN (
  'eeeeeeee-0000-0000-0000-000000000001',
  'eeeeeeee-0000-0000-0000-000000000002',
  'eeeeeeee-0000-0000-0000-000000000003',
  'eeeeeeee-0000-0000-0000-000000000004',
  'eeeeeeee-0000-0000-0000-000000000005'
);

INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until, first_responded_at, fit_analysis_id, cv_id, user_id)
VALUES
  ('dddddddd-0000-0000-0000-000000000001', 'Northstar AI', 'Chief of Staff', NULL, NULL, 'Interview', 'Yes', 'Strategy & Operations', NOW() + INTERVAL '5 days', NOW() - INTERVAL '20 days', NOW() - INTERVAL '4 days', NULL, NOW() - INTERVAL '6 days', 'eeeeeeee-0000-0000-0000-000000000001', NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('dddddddd-0000-0000-0000-000000000002', 'Brightline SaaS', 'Strategy & Operations Associate', NULL, NULL, 'Interview', 'No', 'Strategy & Operations', NOW() + INTERVAL '8 days', NOW() - INTERVAL '18 days', NOW() - INTERVAL '3 days', NULL, NOW() - INTERVAL '5 days', 'eeeeeeee-0000-0000-0000-000000000002', NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('dddddddd-0000-0000-0000-000000000003', 'OrbitCloud', 'Business Operations Manager', NULL, NULL, 'Applied', 'No', 'Strategy & Operations', NULL, NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days', NULL, NULL, 'eeeeeeee-0000-0000-0000-000000000003', NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('dddddddd-0000-0000-0000-000000000004', 'SignalWorks', 'Founder Associate', NULL, NULL, 'Applied', 'No', 'Strategy & Operations', NULL, NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days', NULL, NULL, 'eeeeeeee-0000-0000-0000-000000000004', NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('dddddddd-0000-0000-0000-000000000005', 'HelioHealth', 'Strategic Operations Analyst', NULL, NULL, 'Referral Received', 'Yes', 'Strategy & Operations', NULL, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days', NULL, NULL, 'eeeeeeee-0000-0000-0000-000000000005', NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('dddddddd-0000-0000-0000-000000000011', 'Vertex Labs', 'Operations Lead', NULL, NULL, 'Applied', 'No', 'Strategy & Operations', NULL, NOW() - INTERVAL '45 days', NOW() - INTERVAL '45 days', NULL, NULL, 'eeeeeeee-0000-0000-0000-000000000006', NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('dddddddd-0000-0000-0000-000000000012', 'Cedar Health', 'Business Operations Associate', NULL, NULL, 'Applied', 'No', 'Strategy & Operations', NULL, NOW() - INTERVAL '52 days', NOW() - INTERVAL '52 days', NULL, NULL, 'eeeeeeee-0000-0000-0000-000000000007', NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('dddddddd-0000-0000-0000-000000000013', 'Atlas Commerce', 'Strategy Associate', NULL, NULL, 'Applied', 'No', 'Strategy & Operations', NULL, NOW() - INTERVAL '60 days', NOW() - INTERVAL '60 days', NULL, NULL, 'eeeeeeee-0000-0000-0000-000000000008', NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('dddddddd-0000-0000-0000-000000000006', 'Nimbus Product', 'Associate Product Manager', NULL, NULL, 'Applied', 'No', 'Product Management', NULL, NOW() - INTERVAL '42 days', NOW() - INTERVAL '42 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('dddddddd-0000-0000-0000-000000000007', 'Harbor Product', 'Product Analyst', NULL, NULL, 'Applied', 'No', 'Product Management', NULL, NOW() - INTERVAL '38 days', NOW() - INTERVAL '38 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('dddddddd-0000-0000-0000-000000000008', 'SparkPM', 'Product Operations Associate', NULL, NULL, 'Applied', 'No', 'Product Management', NULL, NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('dddddddd-0000-0000-0000-000000000009', 'LaunchLab', 'Product Manager Intern', NULL, NULL, 'Applied', 'No', 'Product Management', NULL, NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa'),
  ('dddddddd-0000-0000-0000-000000000010', 'Roadmaply', 'Product Strategy Intern', NULL, NULL, 'Applied', 'No', 'Product Management', NULL, NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days', NULL, NULL, NULL, NULL, '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa')
ON CONFLICT (id) DO UPDATE
SET company = EXCLUDED.company,
    job_title = EXCLUDED.job_title,
    application_status = EXCLUDED.application_status,
    referral = EXCLUDED.referral,
    role_category = EXCLUDED.role_category,
    interview_date = EXCLUDED.interview_date,
    created_at = EXCLUDED.created_at,
    updated_at = EXCLUDED.updated_at,
    snoozed_until = EXCLUDED.snoozed_until,
    first_responded_at = EXCLUDED.first_responded_at,
    fit_analysis_id = EXCLUDED.fit_analysis_id,
    user_id = EXCLUDED.user_id;

UPDATE job_applications SET fit_analysis_id = 'ffffffff-0000-0000-0000-000000000001' WHERE id = 'cccccccc-0000-0000-0000-000000000001';
UPDATE job_applications SET fit_analysis_id = 'ffffffff-0000-0000-0000-000000000002' WHERE id = 'cccccccc-0000-0000-0000-000000000002';
UPDATE job_applications SET fit_analysis_id = 'ffffffff-0000-0000-0000-000000000003' WHERE id = 'cccccccc-0000-0000-0000-000000000003';
UPDATE job_applications SET fit_analysis_id = 'ffffffff-0000-0000-0000-000000000004' WHERE id = 'cccccccc-0000-0000-0000-000000000004';
UPDATE job_applications SET fit_analysis_id = 'ffffffff-0000-0000-0000-000000000101' WHERE id = 'bbbbbbbb-0000-0000-0000-000000000005';
UPDATE job_applications SET fit_analysis_id = 'ffffffff-0000-0000-0000-000000000102' WHERE id = 'bbbbbbbb-0000-0000-0000-000000000007';
UPDATE job_applications SET fit_analysis_id = 'ffffffff-0000-0000-0000-000000000201' WHERE id = 'bbbbbbbb-0000-0000-0000-000000000002';
UPDATE job_applications SET fit_analysis_id = 'ffffffff-0000-0000-0000-000000000202' WHERE id = 'bbbbbbbb-0000-0000-0000-000000000004';
UPDATE job_applications SET fit_analysis_id = 'eeeeeeee-0000-0000-0000-000000000009' WHERE id = 'dddddddd-0000-0000-0000-000000000006';
UPDATE job_applications SET fit_analysis_id = 'eeeeeeee-0000-0000-0000-000000000010' WHERE id = 'dddddddd-0000-0000-0000-000000000007';
UPDATE job_applications SET fit_analysis_id = 'eeeeeeee-0000-0000-0000-000000000011' WHERE id = 'dddddddd-0000-0000-0000-000000000008';

-- Pending evidence for the manual normalizer. Repeated ideas are intentionally
-- phrased differently so the LLM has to cluster concepts instead of matching strings.
INSERT INTO gap_mentions (id, user_id, fit_analysis_id, application_id, company, job_title, role_category, raw_gap_text, raw_category, raw_severity, normalization_status, mapping_confidence, created_at)
VALUES
  ('77777777-0000-0000-0000-000000000001', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa', 'eeeeeeee-0000-0000-0000-000000000001', 'dddddddd-0000-0000-0000-000000000001', 'Northstar AI', 'Chief of Staff', 'Strategy & Operations', 'No evidence of preparing board materials for senior leaders', 'Experience Depth', 'High', 'PENDING', 0.0, NOW() - INTERVAL '18 days'),
  ('77777777-0000-0000-0000-000000000002', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa', 'eeeeeeee-0000-0000-0000-000000000002', 'dddddddd-0000-0000-0000-000000000002', 'Brightline SaaS', 'Strategy & Operations Associate', 'Strategy & Operations', 'Leadership updates and executive-ready synthesis are not visible in the CV', 'Skills', 'High', 'PENDING', 0.0, NOW() - INTERVAL '14 days'),
  ('77777777-0000-0000-0000-000000000003', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa', 'eeeeeeee-0000-0000-0000-000000000003', 'dddddddd-0000-0000-0000-000000000003', 'OrbitCloud', 'Business Operations Manager', 'Strategy & Operations', 'Executive decision synthesis is weaker than the role expects', 'Experience Depth', 'High', 'PENDING', 0.0, NOW() - INTERVAL '10 days'),
  ('77777777-0000-0000-0000-000000000004', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa', 'eeeeeeee-0000-0000-0000-000000000004', 'dddddddd-0000-0000-0000-000000000004', 'SignalWorks', 'Founder Associate', 'Strategy & Operations', 'No clear examples of investor memo or board-prep writing', 'Skills', 'High', 'PENDING', 0.0, NOW() - INTERVAL '6 days'),
  ('77777777-0000-0000-0000-000000000005', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa', 'eeeeeeee-0000-0000-0000-000000000005', 'dddddddd-0000-0000-0000-000000000005', 'HelioHealth', 'Strategic Operations Analyst', 'Strategy & Operations', 'Leadership memo writing is not explicit in the CV', 'Skills', 'High', 'PENDING', 0.0, NOW() - INTERVAL '3 days'),
  ('77777777-0000-0000-0000-000000000006', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa', 'eeeeeeee-0000-0000-0000-000000000001', 'dddddddd-0000-0000-0000-000000000001', 'Northstar AI', 'Chief of Staff', 'Strategy & Operations', 'Limited ownership of ambiguous cross-functional operating projects', 'Seniority', 'Medium', 'PENDING', 0.0, NOW() - INTERVAL '18 days'),
  ('77777777-0000-0000-0000-000000000007', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa', 'eeeeeeee-0000-0000-0000-000000000005', 'dddddddd-0000-0000-0000-000000000005', 'HelioHealth', 'Strategic Operations Analyst', 'Strategy & Operations', 'Cross-functional operating cadence ownership is limited', 'Experience Depth', 'Medium', 'PENDING', 0.0, NOW() - INTERVAL '3 days'),
  ('77777777-0000-0000-0000-000000000008', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab', 'ffffffff-0000-0000-0000-000000000001', 'cccccccc-0000-0000-0000-000000000001', 'McKinsey & Company', 'Business Analyst', 'Consulting', 'Case interview issue-tree structuring is not demonstrated', 'Skills', 'High', 'PENDING', 0.0, NOW() - INTERVAL '19 days'),
  ('77777777-0000-0000-0000-000000000009', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab', 'ffffffff-0000-0000-0000-000000000002', 'cccccccc-0000-0000-0000-000000000002', 'Bain & Company', 'Associate Consultant', 'Consulting', 'Hypothesis-driven case structuring is not visible', 'Skills', 'High', 'PENDING', 0.0, NOW() - INTERVAL '16 days'),
  ('77777777-0000-0000-0000-000000000010', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab', 'ffffffff-0000-0000-0000-000000000003', 'cccccccc-0000-0000-0000-000000000003', 'Boston Consulting Group', 'Associate', 'Consulting', 'Ambiguous business case framing is not backed by examples', 'Skills', 'High', 'PENDING', 0.0, NOW() - INTERVAL '12 days'),
  ('77777777-0000-0000-0000-000000000011', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab', 'ffffffff-0000-0000-0000-000000000004', 'cccccccc-0000-0000-0000-000000000004', 'Deloitte', 'Strategy Consultant', 'Consulting', 'Client workstream structuring is not explicit', 'Experience Depth', 'High', 'PENDING', 0.0, NOW() - INTERVAL '8 days'),
  ('77777777-0000-0000-0000-000000000012', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab', 'ffffffff-0000-0000-0000-000000000001', 'cccccccc-0000-0000-0000-000000000001', 'McKinsey & Company', 'Business Analyst', 'Consulting', 'Market sizing examples are missing from the CV', 'Skills', 'Medium', 'PENDING', 0.0, NOW() - INTERVAL '19 days'),
  ('77777777-0000-0000-0000-000000000013', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab', 'ffffffff-0000-0000-0000-000000000003', 'cccccccc-0000-0000-0000-000000000003', 'Boston Consulting Group', 'Associate', 'Consulting', 'Market opportunity sizing is not quantified', 'Skills', 'Medium', 'PENDING', 0.0, NOW() - INTERVAL '12 days'),
  ('77777777-0000-0000-0000-000000000014', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab', 'ffffffff-0000-0000-0000-000000000004', 'cccccccc-0000-0000-0000-000000000004', 'Deloitte', 'Strategy Consultant', 'Consulting', 'Market model ownership is not clearly evidenced', 'Skills', 'Medium', 'PENDING', 0.0, NOW() - INTERVAL '8 days'),
  ('77777777-0000-0000-0000-000000000015', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ac', 'ffffffff-0000-0000-0000-000000000101', 'bbbbbbbb-0000-0000-0000-000000000005', 'OpenAI', 'Software Engineer', 'Software Engineering', 'Production observability ownership is not demonstrated', 'Experience Depth', 'Medium', 'PENDING', 0.0, NOW() - INTERVAL '13 days'),
  ('77777777-0000-0000-0000-000000000016', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ac', 'ffffffff-0000-0000-0000-000000000102', 'bbbbbbbb-0000-0000-0000-000000000007', 'Notion', 'Senior Engineer', 'Software Engineering', 'Incident response ownership is not clearly shown', 'Experience Depth', 'Medium', 'PENDING', 0.0, NOW() - INTERVAL '9 days'),
  ('77777777-0000-0000-0000-000000000017', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ad', 'ffffffff-0000-0000-0000-000000000201', 'bbbbbbbb-0000-0000-0000-000000000002', 'Meta', 'Product Manager', 'Product Management', 'Product roadmap ownership is not demonstrated', 'Experience Depth', 'High', 'PENDING', 0.0, NOW() - INTERVAL '15 days'),
  ('77777777-0000-0000-0000-000000000018', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ad', 'ffffffff-0000-0000-0000-000000000202', 'bbbbbbbb-0000-0000-0000-000000000004', 'Anthropic', 'Senior Product Manager', 'Product Management', 'Roadmap prioritization decisions are not evidenced', 'Experience Depth', 'High', 'PENDING', 0.0, NOW() - INTERVAL '7 days')
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    fit_analysis_id = EXCLUDED.fit_analysis_id,
    application_id = EXCLUDED.application_id,
    company = EXCLUDED.company,
    job_title = EXCLUDED.job_title,
    role_category = EXCLUDED.role_category,
    raw_gap_text = EXCLUDED.raw_gap_text,
    raw_category = EXCLUDED.raw_category,
    raw_severity = EXCLUDED.raw_severity,
    normalization_status = EXCLUDED.normalization_status,
    capability_phrase = NULL,
    domain_context = NULL,
    artifact = NULL,
    evidence_type = NULL,
    concept_id = NULL,
    concept_name = NULL,
    mapping_confidence = EXCLUDED.mapping_confidence,
    created_at = EXCLUDED.created_at;

-- Seed one validated recurring gap for the demo student. The five mentions
-- represent the same broad capability across five recent applied roles.
INSERT INTO capability_concepts (id, name, description, status, created_at)
VALUES (
  '66666666-0000-0000-0000-000000000001',
  'Executive synthesis',
  'Turning complex operating information into concise, decision-ready communication for senior stakeholders.',
  'APPROVED',
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    status = EXCLUDED.status;

UPDATE gap_mentions
SET normalization_status = 'MAPPED',
    capability_phrase = 'Executive synthesis',
    domain_context = 'Executive communication and operating decisions',
    artifact = 'Leadership memo, board material, or executive update',
    evidence_type = 'written communication',
    concept_id = '66666666-0000-0000-0000-000000000001',
    concept_name = 'Executive synthesis',
    mapping_confidence = 0.95
WHERE id IN (
  '77777777-0000-0000-0000-000000000001',
  '77777777-0000-0000-0000-000000000002',
  '77777777-0000-0000-0000-000000000003',
  '77777777-0000-0000-0000-000000000004',
  '77777777-0000-0000-0000-000000000005'
);

INSERT INTO capability_concepts (id, name, description, status, created_at)
VALUES (
  '66666666-0000-0000-0000-000000000002',
  'Cross-functional operating ownership',
  'Owning complex operating work across teams from planning through execution.',
  'APPROVED',
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    status = EXCLUDED.status;

INSERT INTO gap_mentions (id, user_id, fit_analysis_id, application_id, company, job_title, role_category, raw_gap_text, raw_category, raw_severity, normalization_status, capability_phrase, domain_context, artifact, evidence_type, concept_id, concept_name, mapping_confidence, created_at)
VALUES (
  '77777777-0000-0000-0000-000000000019',
  '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa',
  'eeeeeeee-0000-0000-0000-000000000003',
  'dddddddd-0000-0000-0000-000000000003',
  'OrbitCloud',
  'Business Operations Manager',
  'Strategy & Operations',
  'Cross-functional operating ownership is not clearly demonstrated',
  'Experience Depth',
  'Medium',
  'MAPPED',
  'Cross-functional operating ownership',
  'Cross-functional execution',
  'Operating cadence or special-project ownership',
  'ownership',
  '66666666-0000-0000-0000-000000000002',
  'Cross-functional operating ownership',
  0.95,
  NOW() - INTERVAL '10 days'
)
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    fit_analysis_id = EXCLUDED.fit_analysis_id,
    application_id = EXCLUDED.application_id,
    company = EXCLUDED.company,
    job_title = EXCLUDED.job_title,
    role_category = EXCLUDED.role_category,
    raw_gap_text = EXCLUDED.raw_gap_text,
    raw_category = EXCLUDED.raw_category,
    raw_severity = EXCLUDED.raw_severity,
    normalization_status = EXCLUDED.normalization_status,
    capability_phrase = EXCLUDED.capability_phrase,
    domain_context = EXCLUDED.domain_context,
    artifact = EXCLUDED.artifact,
    evidence_type = EXCLUDED.evidence_type,
    concept_id = EXCLUDED.concept_id,
    concept_name = EXCLUDED.concept_name,
    mapping_confidence = EXCLUDED.mapping_confidence,
    created_at = EXCLUDED.created_at;

UPDATE gap_mentions
SET normalization_status = 'MAPPED',
    capability_phrase = 'Cross-functional operating ownership',
    domain_context = 'Cross-functional execution',
    artifact = 'Operating cadence or special-project ownership',
    evidence_type = 'ownership',
    concept_id = '66666666-0000-0000-0000-000000000002',
    concept_name = 'Cross-functional operating ownership',
    mapping_confidence = 0.95
WHERE id IN (
  '77777777-0000-0000-0000-000000000006',
  '77777777-0000-0000-0000-000000000007',
  '77777777-0000-0000-0000-000000000019'
);

-- Anonymous peer-evidence fixtures for Career Intelligence.
-- Three students have five interview-reached applications; three others have
-- five resolved non-interview applications. All applications share one JD
-- grounded capability so the student-facing comparison is visible locally.
INSERT INTO fit_analyses (id, company, job_title, job_description_text, cv_id, fit_score, recommendation, confidence, weightage_reasoning, analyzed_at, user_id)
VALUES
  ('12121212-0000-0000-0000-000000000001', 'Cedar Peak', 'Strategy & Operations Associate', 'Lead stakeholder planning and communicate operating decisions to senior leaders.', NULL, 72, 'Optimize & Apply', 'Medium', 'Strong stakeholder communication requirement.', NOW() - INTERVAL '28 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab'),
  ('12121212-0000-0000-0000-000000000002', 'Northline', 'Business Operations Analyst', 'Coordinate cross-functional planning and communicate operating decisions to senior leaders.', NULL, 69, 'Optimize & Apply', 'Medium', 'Strong stakeholder communication requirement.', NOW() - INTERVAL '25 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab'),
  ('12121212-0000-0000-0000-000000000003', 'HarborPoint', 'Strategy Associate', 'Lead stakeholder planning and communicate operating decisions to senior leaders.', NULL, 74, 'Optimize & Apply', 'Medium', 'Strong stakeholder communication requirement.', NOW() - INTERVAL '22 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ac'),
  ('12121212-0000-0000-0000-000000000004', 'LumenWorks', 'Operations Associate', 'Coordinate cross-functional planning and communicate operating decisions to senior leaders.', NULL, 71, 'Optimize & Apply', 'Medium', 'Strong stakeholder communication requirement.', NOW() - INTERVAL '19 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ac'),
  ('12121212-0000-0000-0000-000000000005', 'Summit Labs', 'Business Operations Manager', 'Lead stakeholder planning and communicate operating decisions to senior leaders.', NULL, 76, 'Optimize & Apply', 'Medium', 'Strong stakeholder communication requirement.', NOW() - INTERVAL '16 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ad'),
  ('12121212-0000-0000-0000-000000000006', 'Atlas Systems', 'Strategy & Operations Associate', 'Lead stakeholder planning and communicate operating decisions to senior leaders.', NULL, 58, 'Ignore', 'Medium', 'Stakeholder communication requirement is not evidenced.', NOW() - INTERVAL '28 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ae'),
  ('12121212-0000-0000-0000-000000000007', 'Brightline', 'Business Operations Analyst', 'Coordinate cross-functional planning and communicate operating decisions to senior leaders.', NULL, 55, 'Ignore', 'Medium', 'Stakeholder communication requirement is not evidenced.', NOW() - INTERVAL '25 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ae'),
  ('12121212-0000-0000-0000-000000000008', 'Westbrook', 'Strategy Associate', 'Lead stakeholder planning and communicate operating decisions to senior leaders.', NULL, 57, 'Ignore', 'Medium', 'Stakeholder communication requirement is not evidenced.', NOW() - INTERVAL '22 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4af'),
  ('12121212-0000-0000-0000-000000000009', 'Riverstone', 'Operations Associate', 'Coordinate cross-functional planning and communicate operating decisions to senior leaders.', NULL, 54, 'Ignore', 'Medium', 'Stakeholder communication requirement is not evidenced.', NOW() - INTERVAL '19 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4af'),
  ('12121212-0000-0000-0000-000000000010', 'MapleWorks', 'Business Operations Manager', 'Lead stakeholder planning and communicate operating decisions to senior leaders.', NULL, 59, 'Ignore', 'Medium', 'Stakeholder communication requirement is not evidenced.', NOW() - INTERVAL '16 days', '73c62d58-0754-4e4b-a5ba-d7b0293fd4b0')
ON CONFLICT (id) DO UPDATE
SET company = EXCLUDED.company,
    job_title = EXCLUDED.job_title,
    job_description_text = EXCLUDED.job_description_text,
    fit_score = EXCLUDED.fit_score,
    recommendation = EXCLUDED.recommendation,
    confidence = EXCLUDED.confidence,
    weightage_reasoning = EXCLUDED.weightage_reasoning,
    analyzed_at = EXCLUDED.analyzed_at,
    user_id = EXCLUDED.user_id;

INSERT INTO job_applications (id, company, job_title, application_status, referral, role_category, interview_date, created_at, updated_at, first_responded_at, fit_analysis_id, user_id)
VALUES
  ('13131313-0000-0000-0000-000000000001', 'Cedar Peak', 'Strategy & Operations Associate', 'Interview', 'No', 'Strategy & Operations', NOW() + INTERVAL '3 days', NOW() - INTERVAL '28 days', NOW() - INTERVAL '10 days', NOW() - INTERVAL '12 days', '12121212-0000-0000-0000-000000000001', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab'),
  ('13131313-0000-0000-0000-000000000002', 'Northline', 'Business Operations Analyst', 'Interview', 'No', 'Strategy & Operations', NOW() + INTERVAL '5 days', NOW() - INTERVAL '25 days', NOW() - INTERVAL '8 days', NOW() - INTERVAL '10 days', '12121212-0000-0000-0000-000000000002', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ab'),
  ('13131313-0000-0000-0000-000000000003', 'HarborPoint', 'Strategy Associate', 'Interview', 'Yes', 'Strategy & Operations', NOW() + INTERVAL '4 days', NOW() - INTERVAL '22 days', NOW() - INTERVAL '7 days', NOW() - INTERVAL '9 days', '12121212-0000-0000-0000-000000000003', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ac'),
  ('13131313-0000-0000-0000-000000000004', 'LumenWorks', 'Operations Associate', 'Interview', 'No', 'Strategy & Operations', NOW() + INTERVAL '6 days', NOW() - INTERVAL '19 days', NOW() - INTERVAL '6 days', NOW() - INTERVAL '8 days', '12121212-0000-0000-0000-000000000004', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ac'),
  ('13131313-0000-0000-0000-000000000005', 'Summit Labs', 'Business Operations Manager', 'Interview', 'No', 'Strategy & Operations', NOW() + INTERVAL '7 days', NOW() - INTERVAL '16 days', NOW() - INTERVAL '5 days', NOW() - INTERVAL '7 days', '12121212-0000-0000-0000-000000000005', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ad'),
  ('13131313-0000-0000-0000-000000000006', 'Atlas Systems', 'Strategy & Operations Associate', 'Rejected', 'No', 'Strategy & Operations', NULL, NOW() - INTERVAL '28 days', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', '12121212-0000-0000-0000-000000000006', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ae'),
  ('13131313-0000-0000-0000-000000000007', 'Brightline', 'Business Operations Analyst', 'Rejected', 'No', 'Strategy & Operations', NULL, NOW() - INTERVAL '25 days', NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days', '12121212-0000-0000-0000-000000000007', '73c62d58-0754-4e4b-a5ba-d7b0293fd4ae'),
  ('13131313-0000-0000-0000-000000000008', 'Westbrook', 'Strategy Associate', 'Rejected', 'No', 'Strategy & Operations', NULL, NOW() - INTERVAL '22 days', NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days', '12121212-0000-0000-0000-000000000008', '73c62d58-0754-4e4b-a5ba-d7b0293fd4af'),
  ('13131313-0000-0000-0000-000000000009', 'Riverstone', 'Operations Associate', 'Rejected', 'No', 'Strategy & Operations', NULL, NOW() - INTERVAL '19 days', NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days', '12121212-0000-0000-0000-000000000009', '73c62d58-0754-4e4b-a5ba-d7b0293fd4af'),
  ('13131313-0000-0000-0000-000000000010', 'MapleWorks', 'Business Operations Manager', 'Rejected', 'No', 'Strategy & Operations', NULL, NOW() - INTERVAL '16 days', NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days', '12121212-0000-0000-0000-000000000010', '73c62d58-0754-4e4b-a5ba-d7b0293fd4b0')
ON CONFLICT (id) DO UPDATE
SET company = EXCLUDED.company,
    job_title = EXCLUDED.job_title,
    application_status = EXCLUDED.application_status,
    referral = EXCLUDED.referral,
    role_category = EXCLUDED.role_category,
    interview_date = EXCLUDED.interview_date,
    created_at = EXCLUDED.created_at,
    updated_at = EXCLUDED.updated_at,
    first_responded_at = EXCLUDED.first_responded_at,
    fit_analysis_id = EXCLUDED.fit_analysis_id,
    user_id = EXCLUDED.user_id;

DELETE FROM fit_analysis_requirements
WHERE fit_analysis_id IN (
  'eeeeeeee-0000-0000-0000-000000000001', 'eeeeeeee-0000-0000-0000-000000000002',
  'eeeeeeee-0000-0000-0000-000000000003', 'eeeeeeee-0000-0000-0000-000000000004',
  'eeeeeeee-0000-0000-0000-000000000005', '12121212-0000-0000-0000-000000000001',
  '12121212-0000-0000-0000-000000000002', '12121212-0000-0000-0000-000000000003',
  '12121212-0000-0000-0000-000000000004', '12121212-0000-0000-0000-000000000005',
  '12121212-0000-0000-0000-000000000006', '12121212-0000-0000-0000-000000000007',
  '12121212-0000-0000-0000-000000000008', '12121212-0000-0000-0000-000000000009',
  '12121212-0000-0000-0000-000000000010'
);
DELETE FROM fit_analysis_requirement_evidence
WHERE fit_analysis_id IN (
  'eeeeeeee-0000-0000-0000-000000000001', 'eeeeeeee-0000-0000-0000-000000000002',
  'eeeeeeee-0000-0000-0000-000000000003', 'eeeeeeee-0000-0000-0000-000000000004',
  'eeeeeeee-0000-0000-0000-000000000005', '12121212-0000-0000-0000-000000000001',
  '12121212-0000-0000-0000-000000000002', '12121212-0000-0000-0000-000000000003',
  '12121212-0000-0000-0000-000000000004', '12121212-0000-0000-0000-000000000005',
  '12121212-0000-0000-0000-000000000006', '12121212-0000-0000-0000-000000000007',
  '12121212-0000-0000-0000-000000000008', '12121212-0000-0000-0000-000000000009',
  '12121212-0000-0000-0000-000000000010'
);

INSERT INTO fit_analysis_requirements (fit_analysis_id, requirement_key, requirement_text, capability_phrase, importance_tier, relevance_mode, evidence_type, source_excerpt)
SELECT fit_analysis_id, 'REQ-1', 'Communicate operating decisions to senior stakeholders', 'Stakeholder management', 'CORE', 'DIRECT', 'ownership', 'communicate operating decisions to senior leaders'
FROM (VALUES
  ('eeeeeeee-0000-0000-0000-000000000001'::uuid), ('eeeeeeee-0000-0000-0000-000000000002'::uuid), ('eeeeeeee-0000-0000-0000-000000000003'::uuid), ('eeeeeeee-0000-0000-0000-000000000004'::uuid), ('eeeeeeee-0000-0000-0000-000000000005'::uuid),
  ('12121212-0000-0000-0000-000000000001'::uuid), ('12121212-0000-0000-0000-000000000002'::uuid), ('12121212-0000-0000-0000-000000000003'::uuid), ('12121212-0000-0000-0000-000000000004'::uuid), ('12121212-0000-0000-0000-000000000005'::uuid), ('12121212-0000-0000-0000-000000000006'::uuid), ('12121212-0000-0000-0000-000000000007'::uuid), ('12121212-0000-0000-0000-000000000008'::uuid), ('12121212-0000-0000-0000-000000000009'::uuid), ('12121212-0000-0000-0000-000000000010'::uuid)
) AS fixture(fit_analysis_id);

INSERT INTO fit_analysis_requirement_evidence (fit_analysis_id, requirement_key, evidence_status, evidence_type, evidence_text, artifact, confidence)
SELECT fit_analysis_id, 'REQ-1', CASE WHEN fit_analysis_id IN (
  '12121212-0000-0000-0000-000000000001'::uuid, '12121212-0000-0000-0000-000000000002'::uuid, '12121212-0000-0000-0000-000000000003'::uuid, '12121212-0000-0000-0000-000000000004'::uuid, '12121212-0000-0000-0000-000000000005'::uuid
) THEN 'Strong' ELSE 'Missing' END,
  'ownership', CASE WHEN fit_analysis_id IN (
  '12121212-0000-0000-0000-000000000001'::uuid, '12121212-0000-0000-0000-000000000002'::uuid, '12121212-0000-0000-0000-000000000003'::uuid, '12121212-0000-0000-0000-000000000004'::uuid, '12121212-0000-0000-0000-000000000005'::uuid
) THEN 'Led cross-functional stakeholder planning' ELSE NULL END,
  CASE WHEN fit_analysis_id IN (
  '12121212-0000-0000-0000-000000000001'::uuid, '12121212-0000-0000-0000-000000000002'::uuid, '12121212-0000-0000-0000-000000000003'::uuid, '12121212-0000-0000-0000-000000000004'::uuid, '12121212-0000-0000-0000-000000000005'::uuid
) THEN 'Stakeholder planning' ELSE NULL END,
  'High'
FROM (VALUES
  ('eeeeeeee-0000-0000-0000-000000000001'::uuid), ('eeeeeeee-0000-0000-0000-000000000002'::uuid), ('eeeeeeee-0000-0000-0000-000000000003'::uuid), ('eeeeeeee-0000-0000-0000-000000000004'::uuid), ('eeeeeeee-0000-0000-0000-000000000005'::uuid),
  ('12121212-0000-0000-0000-000000000001'::uuid), ('12121212-0000-0000-0000-000000000002'::uuid), ('12121212-0000-0000-0000-000000000003'::uuid), ('12121212-0000-0000-0000-000000000004'::uuid), ('12121212-0000-0000-0000-000000000005'::uuid), ('12121212-0000-0000-0000-000000000006'::uuid), ('12121212-0000-0000-0000-000000000007'::uuid), ('12121212-0000-0000-0000-000000000008'::uuid), ('12121212-0000-0000-0000-000000000009'::uuid), ('12121212-0000-0000-0000-000000000010'::uuid)
) AS fixture(fit_analysis_id);

INSERT INTO student_insights (id, user_id, insight_type, severity, confidence, status, title, summary, recommendation, evidence_json, generated_at)
VALUES
  ('99999999-0000-0000-0000-000000000001', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa', 'RECURRING_CAPABILITY_GAP', 'HIGH', 'MEDIUM', 'ACTIVE', 'Your recent applications keep asking for Executive synthesis', 'This capability has appeared as a gap across 5 applications with saved fit analysis, including Chief of Staff, Strategy & Operations Associate, and Business Operations Manager.', 'Before applying to more similar roles, add one or two concrete CV bullets that show this capability in action.', '{"capability":"Executive synthesis","applicationCount":5,"roleCategory":"Strategy & Operations","examples":[{"company":"Northstar AI","jobTitle":"Chief of Staff","roleCategory":"Strategy & Operations","rawGapText":"No evidence of preparing board materials for senior leaders"},{"company":"Brightline SaaS","jobTitle":"Strategy & Operations Associate","roleCategory":"Strategy & Operations","rawGapText":"Leadership updates and executive-ready synthesis are not visible in the CV"},{"company":"OrbitCloud","jobTitle":"Business Operations Manager","roleCategory":"Strategy & Operations","rawGapText":"Executive decision synthesis is weaker than the role expects"},{"company":"SignalWorks","jobTitle":"Founder Associate","roleCategory":"Strategy & Operations","rawGapText":"No clear examples of investor memo or board-prep writing"},{"company":"HelioHealth","jobTitle":"Strategic Operations Analyst","roleCategory":"Strategy & Operations","rawGapText":"Leadership memo writing is not explicit in the CV"}]}', NOW()),
  ('99999999-0000-0000-0000-000000000004', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa', 'RECURRING_CAPABILITY_GAP', 'MEDIUM', 'MEDIUM', 'ACTIVE', 'Your recent applications keep asking for Cross-functional operating ownership', 'This capability has appeared as a gap across 3 applications with saved fit analysis, including Chief of Staff, Strategic Operations Analyst, and Business Operations Manager.', 'Before applying to more similar roles, add one or two concrete CV bullets that show this capability in action.', '{"capability":"Cross-functional operating ownership","applicationCount":3,"roleCategory":"Strategy & Operations","examples":[{"company":"Northstar AI","jobTitle":"Chief of Staff","roleCategory":"Strategy & Operations","rawGapText":"Limited ownership of ambiguous cross-functional operating projects"},{"company":"HelioHealth","jobTitle":"Strategic Operations Analyst","roleCategory":"Strategy & Operations","rawGapText":"Cross-functional operating cadence ownership is limited"},{"company":"OrbitCloud","jobTitle":"Business Operations Manager","roleCategory":"Strategy & Operations","rawGapText":"Cross-functional operating ownership is not clearly demonstrated"}]}', NOW()),
  ('99999999-0000-0000-0000-000000000002', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa', 'ROLE_CATEGORY_FIT_COMPARISON', 'MEDIUM', 'LOW', 'ACTIVE', 'Strategy & Operations is currently your strongest-fit role category', 'Your median fit score is 61 across 8 applied roles, compared with 52 across 3 Product Management applied roles.', 'Use Strategy & Operations as a reference point while strengthening your CV evidence for Product Management.', '{"bestCategory":"Strategy & Operations","bestApplications":8,"bestMedianFitScore":61,"comparedCategory":"Product Management","comparedApplications":3,"comparedMedianFitScore":52,"fitScoreGap":9}', NOW()),
  ('99999999-0000-0000-0000-000000000003', '73c62d58-0754-4e4b-a5ba-d7b0293fd4aa', 'ROLE_CATEGORY_TRACTION', 'MEDIUM', 'MEDIUM', 'ACTIVE', 'Strategy & Operations is showing stronger traction than Product Management', 'Strategy & Operations has a 40% response rate across 5 seeded applications, compared with 0% for Product Management.', 'Use this as an early signal. Consider prioritizing the role type that is already producing recruiter traction while you improve weaker segments.', '{"bestCategory":"Strategy & Operations","bestApplications":5,"bestResponseRate":0.4,"worstCategory":"Product Management","worstApplications":5,"worstResponseRate":0.0}', NOW())
ON CONFLICT (id) DO UPDATE
SET insight_type = EXCLUDED.insight_type,
    severity = EXCLUDED.severity,
    confidence = EXCLUDED.confidence,
    status = EXCLUDED.status,
    title = EXCLUDED.title,
    summary = EXCLUDED.summary,
    recommendation = EXCLUDED.recommendation,
    evidence_json = EXCLUDED.evidence_json,
    generated_at = EXCLUDED.generated_at;

-- ── DEFAULT ROLE CATEGORIES ──────────────────────────────────
INSERT INTO role_categories (id, name, is_default, is_active) VALUES (gen_random_uuid(), 'Software Engineering', TRUE, TRUE);
INSERT INTO role_categories (id, name, is_default, is_active) VALUES (gen_random_uuid(), 'Data & Analytics', TRUE, TRUE);
INSERT INTO role_categories (id, name, is_default, is_active) VALUES (gen_random_uuid(), 'Product Management', TRUE, TRUE);
INSERT INTO role_categories (id, name, is_default, is_active) VALUES (gen_random_uuid(), 'Design & UX', TRUE, TRUE);
INSERT INTO role_categories (id, name, is_default, is_active) VALUES (gen_random_uuid(), 'Strategy & Operations', TRUE, TRUE);
INSERT INTO role_categories (id, name, is_default, is_active) VALUES (gen_random_uuid(), 'Consulting', TRUE, TRUE);
INSERT INTO role_categories (id, name, is_default, is_active) VALUES (gen_random_uuid(), 'Investment Banking', TRUE, TRUE);
INSERT INTO role_categories (id, name, is_default, is_active) VALUES (gen_random_uuid(), 'Finance & Accounting', TRUE, TRUE);
INSERT INTO role_categories (id, name, is_default, is_active) VALUES (gen_random_uuid(), 'Marketing', TRUE, TRUE);
INSERT INTO role_categories (id, name, is_default, is_active) VALUES (gen_random_uuid(), 'Sales', TRUE, TRUE);
INSERT INTO role_categories (id, name, is_default, is_active) VALUES (gen_random_uuid(), 'Research', TRUE, TRUE);
INSERT INTO role_categories (id, name, is_default, is_active) VALUES (gen_random_uuid(), 'Other', TRUE, TRUE);

-- ── REFERRAL applications ────────────────────────────────────
-- Channel Effectiveness: Referral (Yes): 13 applied, 5 reached Interview/Offer → yield ~38%
-- [DISABLED for email pipeline testing — re-enable when needed]

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Google', 'Software Engineer', 'Alice Brown', 'alice@google.com', 'Offer', 'Yes', 'Software Engineering', '2026-02-10 10:00:00', '2026-01-15 09:00:00', '2026-02-10 10:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Meta', 'Backend Engineer', 'James Lee', 'james@meta.com', 'Offer', 'Yes', 'Software Engineering', '2026-02-12 14:00:00', '2026-01-20 10:00:00', '2026-02-12 14:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Stripe', 'Full Stack Engineer', 'Sara Kim', 'sara@stripe.com', 'Interview', 'Yes', 'Software Engineering', '2026-03-01 11:00:00', '2026-02-01 09:00:00', '2026-02-15 11:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Airbnb', 'Platform Engineer', 'Tom Chen', 'tom@airbnb.com', 'Interview', 'Yes', 'Software Engineering', '2026-03-05 15:00:00', '2026-02-05 10:00:00', '2026-02-18 15:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Notion', 'Senior Engineer', 'Priya Patel', 'priya@notion.so', 'Interview', 'Yes', 'Software Engineering', NULL, '2026-02-08 09:00:00', '2026-02-08 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Figma', 'Software Engineer', 'Dan Park', 'dan@figma.com', 'Applied', 'Yes', 'Software Engineering', NULL, '2026-02-10 09:00:00', '2026-02-10 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Databricks', 'Data Engineer', 'Laura White', 'laura@databricks.com', 'Applied', 'Yes', 'Data & Analytics', NULL, '2026-02-11 09:00:00', '2026-02-11 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Palantir', 'Forward Deployed Engineer', 'Mike Stone', 'mike@palantir.com', 'Applied', 'Yes', 'Strategy & Operations', NULL, '2026-02-12 09:00:00', '2026-02-12 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Rippling', 'Software Engineer', 'Emma Clark', 'emma@rippling.com', 'Rejected', 'Yes', 'Software Engineering', NULL, '2026-02-03 09:00:00', '2026-02-03 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Brex', 'Backend Engineer', 'Noah Davis', 'noah@brex.com', 'Rejected', 'Yes', 'Software Engineering', NULL, '2026-02-04 09:00:00', '2026-02-04 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Plaid', 'API Engineer', 'Olivia Martin', 'olivia@plaid.com', 'Rejected', 'Yes', 'Software Engineering', NULL, '2026-02-06 09:00:00', '2026-02-06 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Linear', 'Software Engineer', 'Liam Wilson', 'liam@linear.app', 'Closed', 'Yes', 'Software Engineering', NULL, '2026-01-28 09:00:00', '2026-01-28 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Vercel', 'Infrastructure Engineer', 'Ava Johnson', 'ava@vercel.com', 'Closed', 'Yes', 'Software Engineering', NULL, '2026-01-30 09:00:00', '2026-01-30 09:00:00', NULL);

-- Referral received but not yet applied (triggers pending actions)
-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Scale AI', 'ML Engineer', 'Chris Taylor', 'chris@scale.com', 'Referral Received', 'Yes', 'Data & Analytics', NULL, '2026-02-17 09:00:00', '2026-02-17 09:00:00', NULL);

-- ── DIRECT applications ──────────────────────────────────────
-- Channel Effectiveness: Direct (No): 20 applied, 3 reached Interview/Offer → yield ~15%
-- [DISABLED for email pipeline testing — re-enable when needed]

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Amazon', 'SDE II', 'Rachel Adams', 'rachel@amazon.com', 'Offer', 'No', 'Software Engineering', '2026-02-08 10:00:00', '2026-01-10 09:00:00', '2026-02-08 10:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Microsoft', 'Software Engineer II', 'Kevin Moore', 'kevin@microsoft.com', 'Interview', 'No', 'Software Engineering', '2026-03-02 13:00:00', '2026-01-25 09:00:00', '2026-02-20 13:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Apple', 'iOS Engineer', 'Sophie Turner', 'sophie@apple.com', 'Interview', 'No', 'Software Engineering', '2026-03-03 09:00:00', '2026-01-28 09:00:00', '2026-02-22 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Uber', 'Software Engineer', 'Ben Harris', 'ben@uber.com', 'Applied', 'No', 'Software Engineering', NULL, '2026-02-14 09:00:00', '2026-02-14 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Lyft', 'Backend Engineer', 'Mia Robinson', 'mia@lyft.com', 'Applied', 'No', 'Software Engineering', NULL, '2026-02-13 09:00:00', '2026-02-13 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Salesforce', 'Platform Developer', 'Ethan Lewis', 'ethan@salesforce.com', 'Applied', 'No', 'Software Engineering', NULL, '2026-02-12 09:00:00', '2026-02-12 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Shopify', 'Ruby Engineer', 'Charlotte Hall', 'charlotte@shopify.com', 'Applied', 'No', 'Software Engineering', NULL, '2026-02-11 09:00:00', '2026-02-11 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Twilio', 'API Engineer', 'Lucas Young', 'lucas@twilio.com', 'Rejected', 'No', 'Software Engineering', NULL, '2026-02-01 09:00:00', '2026-02-01 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Okta', 'Software Engineer', 'Grace Walker', 'grace@okta.com', 'Rejected', 'No', 'Software Engineering', NULL, '2026-01-31 09:00:00', '2026-01-31 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Snowflake', 'Data Platform Engineer', 'Jack Scott', 'jack@snowflake.com', 'Rejected', 'No', 'Data & Analytics', NULL, '2026-01-29 09:00:00', '2026-01-29 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Cloudflare', 'Network Engineer', 'Isabella King', 'isabella@cloudflare.com', 'Rejected', 'No', 'Software Engineering', NULL, '2026-01-27 09:00:00', '2026-01-27 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Datadog', 'Site Reliability Engineer', 'Henry Wright', 'henry@datadog.com', 'Rejected', 'No', 'Software Engineering', NULL, '2026-01-25 09:00:00', '2026-01-25 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'MongoDB', 'Senior Engineer', 'Amelia Lopez', 'amelia@mongodb.com', 'Closed', 'No', 'Software Engineering', NULL, '2026-01-20 09:00:00', '2026-01-20 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'HashiCorp', 'DevOps Engineer', 'William Hill', 'william@hashicorp.com', 'Closed', 'No', 'Software Engineering', NULL, '2026-01-18 09:00:00', '2026-01-18 09:00:00', NULL);

-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Elastic', 'Search Engineer', 'Sofia Green', 'sofia@elastic.co', 'Closed', 'No', 'Software Engineering', NULL, '2026-01-15 09:00:00', '2026-01-15 09:00:00', NULL);

-- ── STALE APPLIED — trigger "Should we close this?" pending action ────────────
-- [DISABLED for email pipeline testing — re-enable when needed]

-- 31 days stale
-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Pinterest', 'Frontend Engineer', 'Ryan Scott', 'ryan@pinterest.com', 'Applied', 'No', 'Software Engineering', NULL, '2026-01-01 09:00:00', '2026-01-24 09:00:00', NULL);

-- 45 days stale
-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Atlassian', 'Software Engineer', 'Nina Brown', 'nina@atlassian.com', 'Applied', 'Yes', 'Software Engineering', NULL, '2025-12-20 09:00:00', '2026-01-10 09:00:00', NULL);

-- 60 days stale
-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Asana', 'Product Manager', 'Mark Evans', 'mark@asana.com', 'Applied', 'No', 'Product Management', NULL, '2025-12-01 09:00:00', '2025-12-25 09:00:00', NULL);

-- Stale but currently snoozed
-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES (gen_random_uuid(), 'Notion', 'Growth Engineer', 'Kelly White', 'kelly@notion.so', 'Applied', 'No', 'Strategy & Operations', NULL, '2025-12-10 09:00:00', '2026-01-05 09:00:00', '2026-03-01 00:00:00');

-- ── APPLICATION UPDATES (audit trail) ────────────────────────
-- [DISABLED for email pipeline testing — re-enable when needed]

-- Anthropic — full journey: Applied → Interview → Offer (3 updates)
-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 'Anthropic', 'Senior Product Manager', 'Claire Zhang', 'claire@anthropic.com', 'Offer', 'Yes', 'Product Management', '2026-02-20 14:00:00', '2026-01-25 09:00:00', '2026-03-01 10:00:00', NULL);

-- INSERT INTO application_updates (application_id, timestamp, summary) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', '2026-01-25 09:05:00', 'Application submitted for Senior Product Manager role at Anthropic via referral from ex-colleague.');
-- INSERT INTO application_updates (application_id, timestamp, summary) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', '2026-02-03 11:30:00', 'Interview scheduled with Claire Zhang (Recruiting) for a 45-minute intro call on February 10th at 3PM.');
-- INSERT INTO application_updates (application_id, timestamp, summary) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', '2026-03-01 10:00:00', 'Offer received for Senior Product Manager role — details shared via DocuSign, response requested by March 8th.');

-- OpenAI — rejected after interview (2 updates)
-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES ('aaaaaaaa-0000-0000-0000-000000000002', 'OpenAI', 'Product Manager', 'David Park', 'david@openai.com', 'Rejected', 'No', 'Product Management', '2026-02-14 10:00:00', '2026-01-30 09:00:00', '2026-02-28 14:00:00', NULL);

-- INSERT INTO application_updates (application_id, timestamp, summary) VALUES ('aaaaaaaa-0000-0000-0000-000000000002', '2026-01-30 09:10:00', 'Application submitted directly for Product Manager role at OpenAI.');
-- INSERT INTO application_updates (application_id, timestamp, summary) VALUES ('aaaaaaaa-0000-0000-0000-000000000002', '2026-02-28 14:00:00', 'Application rejected after final round — feedback indicated the team was looking for stronger experience in safety-focused product work.');

-- Mistral — interview stage (2 updates)
-- INSERT INTO job_applications (id, company, job_title, recruiter_name, recruiter_email, application_status, referral, role_category, interview_date, created_at, updated_at, snoozed_until)
-- VALUES ('aaaaaaaa-0000-0000-0000-000000000003', 'Mistral AI', 'Software Engineer', 'Léa Moreau', 'lea@mistral.ai', 'Interview', 'No', 'Software Engineering', '2026-03-10 15:00:00', '2026-02-10 09:00:00', '2026-02-20 09:00:00', NULL);

-- INSERT INTO application_updates (application_id, timestamp, summary) VALUES ('aaaaaaaa-0000-0000-0000-000000000003', '2026-02-10 09:15:00', 'Application submitted for Software Engineer role at Mistral AI.');
-- INSERT INTO application_updates (application_id, timestamp, summary) VALUES ('aaaaaaaa-0000-0000-0000-000000000003', '2026-02-20 09:00:00', 'Technical interview scheduled with Léa Moreau for March 10th at 3PM — focus on distributed systems.');
