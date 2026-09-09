
<img width="484" height="136" alt="notebridge" src="https://github.com/user-attachments/assets/daaf48f1-5ae6-424f-9bd8-a1c7765ff8f2" />

# Notebridge

Notebridge turns a local collection of study notes into cited practice quizzes. Upload a folder of notes, let the app index the material and surface questionable claims for review, then generate and take quizzes grounded in the eligible excerpts.

Your original note files are not edited. The application stores its index, review queue, generated quizzes, and quiz-attempt history in PostgreSQL.

## What it does

- Imports Markdown, plain-text, PDF, and DOCX files from a selected local folder.
- Extracts text, splits it into chunks, and stores embeddings in PostgreSQL with pgvector for topic-based retrieval.
- Detects potential factual conflicts after a sync and keeps flagged chunks out of quiz generation until they are reviewed.
- Generates evidence-grounded multiple-choice or multi-select quizzes, with citations and explanations.
- Scores one completed attempt per quiz and keeps it in local history.

## Tech stack

- Java 21 and Spring Boot
- Thymeleaf server-rendered UI
- PostgreSQL 17 with the pgvector extension
- Flyway database migrations
- Spring AI with the OpenAI API
- Apache Tika for document-text extraction

## Requirements

The quickest way to run the project is Docker Compose. It needs:

- Docker Desktop (or Docker Engine with Compose)
- An OpenAI API key with access to the configured model

For a non-containerized run, install Java 21, use a PostgreSQL instance with pgvector available, and provide the same environment variables described below.

## Configuration

Copy the example environment file and replace the placeholder API key:

```powershell
Copy-Item .env.example .env
```

Required and optional variables:

| Variable | Purpose | Default |
| --- | --- | --- |
| `OPENAI_API_KEY` | OpenAI API key used for embeddings, quiz generation, and review suggestions. | Required |
| `OPENAI_CHAT_MODEL` | Model used for structured quiz and review responses. | `gpt-4o-mini` in application config; the example sets `gpt-5.4-mini` |
| `POSTGRES_DB` | PostgreSQL database name. | `notebridge` |
| `POSTGRES_USER` | PostgreSQL username. | `notebridge` |
| `POSTGRES_PASSWORD` | PostgreSQL password. | `notebridge` |

`.env` is ignored by Git; do not commit an API key. The Compose configuration also exposes PostgreSQL on port `5432`, so change the port mapping if it conflicts with another local database.

## Run with Docker Compose

```powershell
docker compose up --build
```

When both services are healthy, open [http://localhost:8080](http://localhost:8080). Database migrations run automatically at startup.

To stop the services while retaining indexed data and quiz history:

```powershell
docker compose down
```

The data is stored in the named Docker volume `notebridge-data`. Removing that volume permanently removes the application's local database data.

## Run locally

Start a PostgreSQL database with pgvector, create the configured database and user, and set the connection and OpenAI variables. The application reads these optional datasource overrides:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/notebridge"
$env:SPRING_DATASOURCE_USERNAME = "notebridge"
$env:SPRING_DATASOURCE_PASSWORD = "change-me-for-local-use"
$env:OPENAI_API_KEY = "your-api-key"
```

Then run the Maven wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux, use `./mvnw spring-boot:run` instead. The app is available at [http://localhost:8080](http://localhost:8080).

## How to use it

1. Open **Add notes** and select a folder containing supported notes. The browser uploads the selected files; folder structure is retained as relative paths.
2. Review any items on the **Reviews** page. Flagged chunks are excluded from quizzes until the review is dismissed.
3. Create a quiz. Optionally provide a topic, then select difficulty, question type, and question count.
4. Complete the quiz to see answers, explanations, and source citations. Completed attempts appear in **History**.

Re-syncing the same file with unchanged content skips re-indexing. Updating a file replaces its stored chunks and embeddings, then runs conflict detection for newly changed content.

## Project layout

```text
src/main/java/com/prasadsumit/notebridge/
  ai/           OpenAI integration and structured AI outputs
  ingestion/    file extraction, chunking, indexing, and embeddings
  persistence/  JPA entities and repositories
  quiz/         quiz generation, validation, scoring, and history
  review/       potential-conflict detection and review workflow
  web/          MVC controllers
src/main/resources/
  db/migration/ Flyway schema migrations
  templates/    Thymeleaf pages
  static/       CSS and browser-side JavaScript
compose.yaml    application plus PostgreSQL/pgvector services
```

## Tests

Run the test suite with:

```powershell
.\mvnw.cmd test
```

## Health endpoint

Spring Boot Actuator exposes health information at [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health). This is also used by the Docker Compose health check.

