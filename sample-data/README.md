# Sample Data

## sample-openapi.yaml

This OpenAPI 3.x specification describes the **User Management API** built into the application as a demo at `/demo/users` endpoints.

### Schema Constraints for Testing

| Field | Constraint | Tests Generated |
|-------|-----------|----------------|
| name | minLength=2, maxLength=100 | length=1 (fail), length=2 (pass), length=100 (pass), length=101 (fail) |
| age | minimum=18, maximum=120 | age=17 (fail), age=18 (pass), age=120 (pass), age=121 (fail) |
| role | enum: user, admin, moderator | invalid value "superuser" (fail) |
| email | format: email | missing@domain (may fail) |
| name | required | missing name field (fail with 400) |

### Intentional Demo Defects

| Defect | Endpoint | Expected Contract | Actual Behavior |
|--------|---------|------------------|----------------|
| Wrong status code | `POST /demo/users` | Returns `201 Created` | Returns `200 OK` |
| Missing required field | `PUT /demo/users/{id}` | Returns `updatedAt` field | Omits `updatedAt` from response |

These defects are configurable via `app.demo.defects-enabled=true/false` or `APP_DEMO_DEFECTS_ENABLED=true/false`.

### How to Use

1. Upload this file in the AI API Tester UI (Specification tab)
2. Set target URL to `http://localhost:8080`
3. Click "Generate AI Tests" (requires Ollama running)
4. Review the generated test cases
5. Click "Execute Tests"
6. Check the Violations tab to see the detected defects
7. Click individual failed tests to see AI analysis
