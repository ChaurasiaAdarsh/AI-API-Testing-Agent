# Contributing Guidelines

Thank you for your interest in improving the AI API Testing & Contract Intelligence Agent.

## Development Workflow

1. Fork the repository and create your feature branch:
   ```bash
   git checkout -b feat/my-enhancement
   ```

2. Prerequisites:
   - JDK 21+
   - Node.js 20+
   - Ollama with target model (e.g., `llama3.2`)
   - Docker & Docker Compose (optional for local multi-service runner)

3. Building and Verification:
   - **Backend**:
     ```bash
     cd backend
     mvn clean test
     ```
   - **Frontend**:
     ```bash
     cd frontend
     npm install
     npm run build
     ```

4. Commit Guidelines:
   We follow standard Conventional Commits:
   - `feat:` New capabilities or endpoints
   - `fix:` Bug fixes
   - `docs:` Documentation changes
   - `refactor:` Code improvements without behavioral changes
   - `test:` Adding or updating test cases
   - `ci:` Continuous integration changes

5. Submitting Pull Requests:
   - Ensure all automated tests pass before opening a PR.
   - Describe changes and include relevant issue references in the PR description.
