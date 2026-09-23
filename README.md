# Task App

En enkel Task/Todo-applikation med ett REST API i Java/Spring Boot och en
frontend i HTML, CSS och JavaScript. Projektet demonstrerar ett komplett
CI/CD-flöde med GitHub Actions.

Under utveckling — README fylls på löpande.

## Teknisk stack

- Java 25, Spring Boot 4.1
- Spring Data JPA med H2 in-memory-databas
- Maven
- GitHub Actions

## Struktur

```
backend/    Spring Boot REST API
frontend/   HTML, CSS, JavaScript
e2e/        Playwright-tester
```

Frontend och backend ligger i samma repo eftersom de är tätt kopplade via
API:t och släpps tillsammans, men hålls isär i egna mappar.

## Kör lokalt

Kräver Java 25.

```bash
cd backend
./mvnw spring-boot:run
```

Applikationen startar på http://localhost:8080

## Branch-strategi

- `feature/*` skapas från `dev` och mergas via Pull Request till `dev`
- `dev` mergas via Pull Request till `main` vid release
- Båda branches är skyddade och kräver grön CI