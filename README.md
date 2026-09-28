# Task App

En enkel Task/Todo-applikation med ett REST API i Java och Spring Boot samt en
frontend i HTML, CSS och JavaScript. Projektet demonstrerar ett komplett
CI/CD-flöde med GitHub Actions, från kodändring via Pull Request och
automatiska tester hela vägen till driftsättning i två separata miljöer.

## Live

| Miljö | URL |
|---|---|
| Development | https://task-app-dev.onrender.com |
| Production | https://task-app-prod-fz42.onrender.com |

Båda miljöerna körs på Renders gratisnivå och försätts i viloläge efter en
stunds inaktivitet. Första anropet efter en paus kan därför ta 30 till 60
sekunder innan sidan svarar.

## Vad applikationen gör

Användaren kan lista uppgifter, skapa nya uppgifter med titel och valfri
beskrivning, markera en uppgift som klar och ta bort en uppgift. Frontenden
anropar backendens REST API med `fetch()`, och all data lagras i databasen via
API:t.

### API

| Metod | Endpoint | Svar |
|---|---|---|
| GET | `/api/tasks` | 200 med alla uppgifter |
| GET | `/api/tasks/{id}` | 200, eller 404 om id saknas |
| POST | `/api/tasks` | 201 med `Location`-header, 400 vid ogiltig indata |
| PUT | `/api/tasks/{id}` | 200, eller 404 om id saknas |
| DELETE | `/api/tasks/{id}` | 204, eller 404 om id saknas |

## Teknisk stack

- Java 25 och Spring Boot 4.1
- Spring Data JPA med H2 in-memory-databas
- Maven
- HTML, CSS och JavaScript utan ramverk
- JUnit 5, Mockito och MockMvc för tester
- Playwright för end-to-end-tester
- Docker, GitHub Container Registry och Render för driftsättning
- GitHub Actions för CI/CD

Databasen är in-memory, vilket innebär att data nollställs vid omstart och vid
varje ny driftsättning. Det är ett medvetet val: uppgiftens fokus är CI/CD, inte
databasinfrastruktur.

## Repostruktur

```
task-app/
├── backend/            Spring Boot REST API
├── frontend/           HTML, CSS och JavaScript
├── e2e/                Playwright end-to-end-tester
├── Dockerfile          Bygger hela applikationen till en image
└── .github/workflows/  CI och CD
```

### Designbeslut: ett repo med separata mappar

Frontend och backend har olika tech stacks och hålls därför isär i egna mappar,
men de versioneras och släpps tillsammans i ett repo eftersom de är tätt
kopplade via API:t. En ändring i API:t kräver oftast en ändring i frontenden i
samma Pull Request, och end-to-end-testerna ska testa exakt den kombination som
sedan driftsätts.

Separata repon hade gett oberoende release-cykler, vilket det här projektet inte
behöver, till priset av att end-to-end-tester och production-deploy hade behövt
samordnas mellan repon. Vid bygget kopieras `frontend/` in som statiska resurser
i jar-filen, så hela applikationen driftsätts som en enda artefakt. Det gör att
frontend och backend ligger på samma origin, vilket i sin tur gör CORS
överflödigt.

## Kör lokalt

Kräver Java 25. Node 18 eller senare behövs bara för end-to-end-testerna.

```bash
cd backend
./mvnw spring-boot:run
```

Applikationen startar på http://localhost:8080 och serverar både frontenden och
API:t.

### Kör testerna

```bash
cd backend
./mvnw test                          # enhetstester
./mvnw verify -DskipUnitTests=true   # integrationstester
```

```bash
cd e2e
npm ci
npx playwright install chromium
npx playwright test
```

Playwright startar själv den byggda jar-filen om `BASE_URL` inte är satt. Bygg
därför applikationen först med `./mvnw clean package -DskipUnitTests=true`.

## Testnivåer

**Enhetstester** (`backend/src/test/java/se/taskapp/service`) testar
`TaskService` isolerat med ett mockat repository. De täcker samtliga
CRUD-operationer och båda fallen där en uppgift inte hittas. Ingen
Spring-kontext startas.

**Integrationstester** (`backend/src/test/java/se/taskapp/controller`) testar
controller, service, repository och databasen tillsammans utan något mockat. De
skickar riktiga HTTP-anrop via MockMvc mot H2, verifierar statuskoder och
JSON-svar, och läser i flera fall tillbaka data via API:t för att bekräfta att
den faktiskt persisterats.

**End-to-end-tester** (`e2e/tests`) kör ett riktigt användarflöde i en
webbläsare: skapa en uppgift, verifiera att den visas, markera den som klar,
ladda om sidan och verifiera att tillståndet finns kvar, ta bort uppgiften och
verifiera att den försvunnit. Ett andra test verifierar att en uppgift utan
titel inte skapas.

Enhetstester och integrationstester skiljs åt genom namnkonvention, `*Test` körs
av Surefire och `*IT` av Failsafe, vilket gör att pipelinen kan köra dem som
separata steg.

## Branch-strategi

```
feature/*  --PR-->  dev  --PR (release)-->  main
```

- `dev` är repots default branch
- All ny funktionalitet görs på en `feature/*`-branch som skapas från `dev`
- Ändringar når `dev` enbart via Pull Request
- `dev` mergas till `main` via en release-PR vid varje milstolpe
- Båda branches är skyddade av rulesets: direkta pushar är blockerade och CI
  måste vara grön innan merge

Tester ligger i samma Pull Request som koden de testar, i stället för i en egen
branch i efterhand.

## CI/CD-flödet

Två workflows delar på arbetet. `ci.yml` kör på Pull Requests och skyddar
branchen innan merge. `deploy.yml` kör på push till `dev` och `main` och äger
hela kedjan från build till driftsättning.

### Vid Pull Request

```
Build  ->  Unit tests  ->  Integration tests  ->  End-to-end tests
```

End-to-end-testerna körs här mot en lokalt startad instans på GitHubs runner, så
att trasiga användarflöden fångas innan de mergas.

### Vid push till dev

```
Build  ->  Unit tests  ->  Integration tests  ->  Docker image
  ->  Deploy development  ->  End-to-end tests mot development
```

### Vid push till main

```
Build  ->  Unit tests  ->  Integration tests  ->  Docker image
  ->  Deploy production  ->  Smoke test
```

Jobben är kopplade med `needs`, vilket gör att inget senare steg körs om ett
tidigare fallerar.

### Hur driftsättningen fungerar

GitHub Actions bygger en Docker-image och pushar den till GitHub Container
Registry, taggad med commitens SHA. Därefter anropas Renders API för att
driftsätta exakt den imagen, och pipelinen pollar deployens status tills den är
`live` eller fallerar.

Renders egen automatiska driftsättning från GitHub är avstängd. All driftsättning
sker från pipelinen, vilket innebär att samma verifierade image används i både
development och production.

Innan end-to-end-testerna körs mot development anropar pipelinen applikationen
upprepade gånger tills den svarar, eftersom gratisnivån behöver väckas ur
viloläge.

### Hur production skyddas

`main` kräver att checken `End-to-end tests against development` är grön.
Release-PR:en från `dev` har samma commit-SHA som senaste push till `dev`, så
resultatet från end-to-end-körningen mot development räknas på PR:en. Därmed kan
ingen kod nå production utan att först ha driftsatts till development och klarat
end-to-end-testerna där.

### Secrets och miljöer

Pipelinen använder GitHub Environments `development` och `production`, var och en
med sin egen `RENDER_SERVICE_ID` och `APP_URL`. API-nyckeln till Render ligger
som secret. Inga credentials finns i repot.