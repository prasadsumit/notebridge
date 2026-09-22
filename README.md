
<img width="484" height="136" alt="notebridge" src="https://github.com/user-attachments/assets/daaf48f1-5ae6-424f-9bd8-a1c7765ff8f2" />

# Notebridge

Notebridge turns a local collection of study notes into cited practice quizzes. Upload a folder of notes, let the app index the material and surface questionable claims for review, then generate and take quizzes grounded in the eligible excerpts.

Your original note files are not edited. The application stores its index, review queue, generated quizzes, and quiz-attempt history in PostgreSQL.

## What it does

- Imports Markdown, plain-text, PDF, DOCX, and EPUB files from a selected local folder.
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

The application is available as a pre-built Docker image, so you do not need to install Java, Maven, PostgreSQL, or build the application locally.

### Prerequisites

Make sure you have:

* [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
* Docker Compose available

You can verify the installation with:

```bash
docker --version
docker compose version
```

### 1. Download the Docker Compose configuration

Download the following files from this repository or from GitHub:

```text
compose.yaml
.env.example
```

You do not need to clone the complete source code repository.

### 2. Configure environment variables

Create a `.env` file from the provided example in GitHub and add you OpenAI API key. You can also change the PostgreSQL credentials if you want.

### 3. Start the application

From the directory containing `compose.yaml`, run:

```bash
docker compose up -d
```

Docker Compose will automatically:

* Pull the application image from Docker Hub
* Pull the PostgreSQL image
* Create the required Docker network
* Create the PostgreSQL persistent volume
* Configure the required environment variables
* Start PostgreSQL
* Start the application once the database is ready

No manual database setup is required.

### 4. Access the application

Once the containers are running, open:

```text
http://localhost:8080
```

### Check container status

Run:

```bash
docker compose ps
```

You can also view the containers from Docker Desktop. The application and PostgreSQL containers should appear grouped under the same Compose project.

### View application logs

To view all logs:

```bash
docker compose logs -f
```

To view only the application logs:

```bash
docker compose logs -f app
```

To view only the PostgreSQL logs:

```bash
docker compose logs -f db
```

Press `Ctrl+C` to stop following the logs.

### Stop the application

To stop and remove the containers:

```bash
docker compose down
```

The PostgreSQL data is stored in a Docker volume and will remain available the next time you start the application.

Start it again with:

```bash
docker compose up -d
```

### Reset the database

If you want to completely remove the application containers **and all PostgreSQL data**, run:

```bash
docker compose down -v
```

Then start the application again:

```bash
docker compose up -d
```

> **Warning:** `docker compose down -v` permanently deletes the PostgreSQL Docker volume and all data stored in it.

### Update to the latest image

If a newer Docker image is available, run:

```bash
docker compose pull
docker compose up -d
```

Docker Compose will download the updated image and recreate the required containers.


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

## License

Notebridge is licensed under the [GNU Affero General Public License v3.0](LICENSE) (`AGPL-3.0-only`).
