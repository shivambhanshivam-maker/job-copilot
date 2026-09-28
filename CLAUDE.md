# Job Copilot — Claude Context

## Tech Stack
- **Java 21**, Spring Boot 3, Spring Data JPA
- **Spring AI** (OpenAI, model: `gpt-5.6-luna`) — all LLM calls go through `ChatClient`
- **H2 file-based DB** at `./data/jobcopilot` (persists across restarts)
- **Gmail API** for email polling (OAuth2, tokens stored in `./tokens/`)

## Running the App
- Server: `http://localhost:8084`
- H2 Console: `http://localhost:8084/h2-console`
  - JDBC URL: `jdbc:h2:file:./data/jobcopilot` | User: `sa` | Password: (empty)
- To wipe DB: delete `./data/` folder and restart

## Package Structure
`com.shivam.jobcopilot` with standard subpackages: `controller`, `service`, `entity`, `dto`, `repository`, `scheduler`

## Spring AI Pattern
`ChatClient` is always built once in the constructor via `ChatClient.Builder`:
```java
this.chatClient = chatClientBuilder.defaultSystem("...").build();
```
Never inject `ChatClient` directly — always inject `ChatClient.Builder`.

## Email Pipeline (2 LLM calls per job-related email)
1. **Phase 1 — `EmailClassificationService`**: Extracts structured fields from raw email (company, jobTitle, status, interviewDate, etc.) → returns `JobApplicationEmail`
2. **Phase 2 — `EmailMergeDecisionService`**: Given the extracted email + existing applications + fit analyses at that company → returns `MergeDecision` (which app to update/create, what fields to set/clear, which fitAnalysis to link)

**`MergeDecision` contract:**
- `fieldsToSet` — only non-null, non-blank values explicitly present in the email
- `fieldsToClear` — intentional erasure due to status transition (e.g. `interviewDate` after Offer/Rejected)
- `fitAnalysisId` — semantically matched fit analysis for this role (fuzzy title match)
- These are two distinct mechanisms. Never put null in `fieldsToSet`; use `fieldsToClear` for intentional clearing.

**LLM call optimisation:** When no existing applications exist for a company, skip Phase 2 entirely and CREATE directly.

## data.sql
All `job_applications` and `application_updates` inserts are **commented out** for email pipeline testing.
Role categories always load. To restore seed data: uncomment the relevant blocks in `data.sql`.

## Key Rules
- `roleCategory: "Other"` means unknown — never overwrite a real existing category with it
- `company` and `jobTitle` are never cleared via `fieldsToClear`
- FitAnalysis company/jobTitle come from the **caller** (not LLM extraction) to ensure reliable auto-linking
- `tryLinkFitAnalysis()` is only used for manual application creation — email path uses the merge decision's `fitAnalysisId`
