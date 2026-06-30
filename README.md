# Job Copilot

An AI-powered job search management platform that tracks applications, analyzes CV fit, monitors email communications, and surfaces analytics — all in one place.

## Features

- **Application tracking** — Full pipeline from Applied → Interview → Offer, with snooze, notes, and recruiter details
- **AI fit analysis** — Scores your CV against a job description across skills, experience, domain, impact, and presentation with actionable CV adjustment suggestions
- **Email auto-update** — Connects Gmail and Outlook via OAuth2; a two-phase LLM pipeline extracts application events from emails and updates the tracker automatically
- **Job discovery** — Nightly fetch via JSearch API matches listings to your preferred roles and locations, running fit analysis on each
- **Analytics dashboard** — Channel effectiveness, funnel conversion, application velocity, ghosting rate, response time, and offer rate
- **AI chatbot** — Conversational assistant with tool access to your applications, fit analyses, and pending actions
- **CV management** — Upload multiple CVs (PDF/Word), set a default, and view AI-generated CV-specific recommendations

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.2 |
| AI | Spring AI 1.0.0 + OpenAI `gpt-5.2` |
| Database | PostgreSQL (Neon serverless) / H2 for local dev |
| Auth | JWT (JJWT 0.12.6) + AES-256-GCM token encryption |
| Email | Gmail API (Google OAuth2) + Microsoft Graph API (MSAL4J) |
| Job Search | RapidAPI JSearch |
| CV Parsing | Apache PDFBox + Apache POI |
| Build | Maven |

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL database (or use the H2 local profile)
- OpenAI API key
- (Optional) Google Cloud project with Gmail API enabled
- (Optional) Azure AD app registration with Mail.Read permission
- (Optional) RapidAPI key for JSearch

## Environment Variables

Create an `application-local.properties` file (gitignored) or set these as environment variables:

```properties
# Database
spring.datasource.url=jdbc:postgresql://<host>/<db>
spring.datasource.username=<user>
spring.datasource.password=<password>

# OpenAI
spring.ai.openai.api-key=sk-proj-...
spring.ai.openai.chat.options.model=gpt-5.2

# JWT
jwt.secret=<min-32-char-secret>
jwt.expiration-ms=86400000

# AES token encryption key (base64-encoded 32 bytes)
token.encryption.key=<base64-key>

# Frontend origin (for CORS)
app.frontend.url=http://localhost:4200
app.cors.allowed-origins=http://localhost:4200

# Gmail OAuth2
gmail.credentials.json=<path-to-credentials.json>
gmail.oauth.redirect-uri=http://localhost:8084/api/gmail/callback

# Outlook OAuth2
outlook.client.id=<azure-app-client-id>
outlook.client.secret=<azure-app-client-secret>
outlook.oauth.redirect-uri=http://localhost:8084/api/outlook/callback

# Job discovery
jsearch.api.key=<rapidapi-key>
```

### Local development with H2

Skip the PostgreSQL variables and instead set:

```properties
spring.datasource.url=jdbc:h2:file:./data/jobcopilot
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

H2 Console is available at `http://localhost:8084/h2-console` when running locally.

To reset the database, stop the server and delete the `./data/` folder.

## Running the App

```bash
mvn spring-boot:run
```

The server starts on `http://localhost:8084`.

To build a runnable JAR:

```bash
mvn clean package
java -jar target/job-copilot-0.0.1-SNAPSHOT.jar
```

## API Overview

| Area | Base Path | Notes |
|---|---|---|
| Auth | `/auth` | `POST /signup`, `POST /login` — signup requires allowlisted email |
| Applications | `/job-applications` | CRUD, snooze, pending actions |
| Fit Analysis | `/match`, `/fit-analyses` | Streaming SSE for analysis, re-analysis, toggle adjustments |
| Analytics | `/analytics` | Channel, funnel, velocity, pipeline, performance metrics |
| Gmail | `/api/gmail` | OAuth connect/disconnect, connection status |
| Outlook | `/api/outlook` | OAuth connect/disconnect, connection status |
| CVs | `/cvs` | Upload, list, set default |
| Job Listings | `/job-listings` | Discovered jobs with linked fit analyses |
| Chatbot | `/chatbot` | Streaming conversational AI |
| User Prefs | `/user-preferences` | Role categories, locations, experience level |

All endpoints except `/auth/**`, `/role-categories`, and the OAuth callbacks require a `Bearer <jwt>` token in the `Authorization` header.

## Email Processing Pipeline

When a job-related email arrives (Gmail or Outlook polling runs every 60 seconds):

1. **Phase 1 — Classification**: LLM extracts structured fields (company, job title, status, recruiter email, interview date) from the raw email.
2. **Phase 2 — Merge decision**: LLM compares the extracted email against existing applications at that company and returns a `MergeDecision` specifying which application to create or update, which fields to set, which to clear, and which fit analysis to link.

If no existing applications exist for a company, Phase 2 is skipped and a new application is created directly.

## Signup Allowlist

Registration is restricted to emails listed in the `allowed_emails` table. Add an email via the H2 console or a direct SQL insert before signing up:

```sql
INSERT INTO allowed_emails (email) VALUES ('you@example.com');
```

## Seed Data

`src/main/resources/data.sql` seeds role categories on every startup. Sample job applications and updates are commented out by default (for email pipeline testing). Uncomment the relevant blocks to restore them.

## Frontend

This repository is the backend API only. The Angular frontend lives in a separate repository and is deployed to Vercel. During local development it runs on `http://localhost:4200`.