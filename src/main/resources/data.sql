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