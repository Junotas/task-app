# Task App

En enkel Task/Todo-applikation med ett REST API i Java och Spring Boot och en
frontend i HTML, CSS och JavaScript. Applikationen i sig är avsiktligt liten.
Projektets fokus är CI/CD: hur en kodändring tar sig via Pull Request och
automatiska tester hela vägen till produktion, och vad som måste vara sant innan
den får komma dit.

## Live

| | Development | Production |
|---|---|---|
| Applikation | https://task-app-frontend-dev.onrender.com | https://task-app-frontend-prod.onrender.com |
| API | https://task-app-dev.onrender.com/api/tasks | https://task-app-prod-fz42.onrender.com/api/tasks |

Alla fyra tjänsterna körs på Renders gratisnivå och försätts i viloläge efter
en stunds inaktivitet. Första anropet efter en paus kan ta 30 till 60 sekunder.

## Vad applikationen gör

Användaren kan lista uppgifter, skapa nya med titel och valfri beskrivning,
markera en uppgift som klar och ta bort en uppgift. Frontenden anropar
backendens REST API med `fetch()`.

| Metod | Endpoint | Svar |
|---|---|---|
| GET | `/api/tasks` | 200 med alla uppgifter |
| GET | `/api/tasks/{id}` | 200, 404 om id saknas, 400 om id inte är ett tal |
| POST | `/api/tasks` | 201 med `Location`-header, 400 vid ogiltig indata, 415 om det inte är JSON |
| PUT | `/api/tasks/{id}` | 200, 404 om id saknas, 400 vid ogiltig indata |
| DELETE | `/api/tasks/{id}` | 204, 404 om id saknas |

Alla felsvar har samma format: `{ "status": 400, "message": "...", "timestamp": "..." }`.

## Arkitektur

```
            Webbläsare
                │
                ▼
   ┌─────────────────────────┐        ┌─────────────────────────┐
   │ Frontend                │  CORS  │ Backend                 │
   │ nginx, statiska filer   │ ─────▶ │ Spring Boot, REST API   │
   │ config.js från API_URL  │        │ ALLOWED_ORIGINS         │
   └─────────────────────────┘        └─────────────────────────┘
```

Frontend och backend är två separata Docker-images som byggs, testas och
driftsätts oberoende av varandra. Varje miljö har en tjänst av varje på Render.

Frontendens image är identisk i alla miljöer. Vilken backend den pratar med
bestäms när containern startar: ett skript skriver `config.js` utifrån
miljövariabeln `API_URL`. Backenden släpper bara igenom anrop från de origins som
står i `ALLOWED_ORIGINS`.

## Teknisk stack

- Java 25 och Spring Boot 4.1
- Spring Data JPA med H2 in-memory-databas
- Maven
- HTML, CSS och JavaScript utan ramverk, serverat av nginx
- JUnit 5, Mockito och MockMvc för backendtester
- Playwright för end-to-end-tester
- Docker, GitHub Container Registry och Render för driftsättning
- GitHub Actions för CI/CD

## Repostruktur

```
task-app/
├── backend/                 Spring Boot REST API och dess Dockerfile
├── frontend/                HTML, CSS, JavaScript, nginx-konfiguration och Dockerfile
├── e2e/                     Playwright-tester
└── .github/
    ├── workflows/
    │   ├── ci.yml           Körs på Pull Requests
    │   └── deploy.yml       Körs på push till dev och main
    └── actions/
        ├── render-deploy/   Deployar en image till Render och väntar tills den är live
        └── wait-for-url/    Väntar tills en tjänst svarar
```

## Kör lokalt

Kräver Java 25 och Node 18 eller senare. På Windows används `mvnw.cmd` i stället
för `mvnw`.

Backenden, i en terminal:

```bash
cd backend
ALLOWED_ORIGINS=http://localhost:5173 ./mvnw spring-boot:run
```

Frontenden, i en annan terminal:

```bash
node frontend/dev-server.mjs
```

Öppna http://localhost:5173. Frontenden anropar backenden på
http://localhost:8080.

### Tester

Enhetstester respektive integrationstester:

```bash
cd backend
./mvnw test
./mvnw verify -DskipUnitTests=true
```

End-to-end:

```bash
cd backend && ./mvnw clean package -DskipUnitTests=true && cd ../e2e
npm ci
npx playwright install chromium
npx playwright test
```

Playwright startar själv backenden och frontenden på var sin port och kör
testerna över CORS, på samma sätt som i molnet. Sätts `BASE_URL` körs testerna i
stället mot en redan driftsatt miljö.

## Testnivåer

**Enhetstester** testar logik isolerat. `TaskServiceTest` testar servicelagret
med ett mockat repository. `TaskRequestValidationTest` testar valideringen av
indata direkt, utan Spring: tomma titlar, titlar som bara ser tomma ut (hårt
mellanslag, osynliga tecken), dolda kontrolltecken, längdgränser, svenska tecken,
emoji, HTML och SQL-liknande text.

**Integrationstester** mockar ingenting. De skickar riktiga HTTP-anrop genom
controller, service, repository och databas. `TaskControllerIT` täcker alla
endpoints. `TaskControllerEdgeCaseIT` täcker sådant en användare inte borde skicka:
trasig JSON, tom body, fel typer, fel content-type, id som inte är tal, och
kontrollerar att ogiltiga uppdateringar inte ändrar något. `CorsIT` sätter
`ALLOWED_ORIGINS` precis som Render gör och kontrollerar att rätt origins släpps
igenom och att andra stoppas.

**End-to-end-tester** kör ett riktigt användarflöde i en webbläsare: skapa,
markera som klar, ladda om och se att tillståndet finns kvar, ta bort, ladda om.
Omladdningarna bevisar att frontenden faktiskt pratar med backenden och inte bara
uppdaterar sidan lokalt.

Enhetstester och integrationstester skiljs åt med namnkonvention: `*Test` körs av
Surefire och `*IT` av Failsafe, så att pipelinen kan visa dem som separata steg.

## Branch-strategi

```
feature/*  ──PR──▶  dev  ──PR (release)──▶  main
```

- `dev` är default branch, och all ny kod når den via Pull Request
- `dev` mergas till `main` via en release-PR
- Båda branches är skyddade. Direkta pushar är blockerade och CI måste vara grön
- `main` kräver dessutom att end-to-end-testerna mot development har gått igenom

## CI/CD

### Bara det som ändrats körs

Båda workflows börjar med att avgöra vilka delar av repot som har ändrats, och
kör sedan bara det som berörs:

| Ändring i | Backendtester | Ny image och deploy | End-to-end |
|---|---|---|---|
| `backend/` | ja | backend | ja |
| `frontend/` | nej | frontend | ja |
| `e2e/` | nej | nej | ja, mot befintlig miljö |
| `.github/` | ja | båda | ja |
| övrigt, till exempel README | nej | nej | nej |

Filtreringen görs per jobb och inte per workflow. Ett helt workflow som hoppas
över rapporterar aldrig sina checks, och då väntar en skyddad branch på dem för
alltid. Ett jobb som hoppas över via ett villkor räknas däremot som godkänt.

### Pull Request (`ci.yml`)

```
Detect changes ─┬─▶ Build and test ─┬─▶ End-to-end tests
                └─▶ Frontend image ─┘
```

`Frontend image` bygger nginx-imagen, startar den och kontrollerar att den
serverar sidan och skriver rätt `config.js`. End-to-end-testerna körs mot
backend och frontend som startas lokalt på runnern.

### Push till dev (`deploy.yml`)

```
Build and test ─▶ Backend image ─▶ Deploy backend ─┐
                                                   ├─▶ End-to-end mot dev ─▶ Mark verified
                  Frontend image ─▶ Deploy frontend┘
```

Varje ändrad del byggs till en image, taggad med commitens SHA, och deployas.
Backenden deployas alltid före frontenden, så att frontenden aldrig anropar ett
API som inte finns än. När end-to-end-testerna mot den driftsatta miljön har gått
igenom får de testade imagerna taggen `dev-verified`.

### Push till main (`deploy.yml`)

```
Deploy backend ─▶ Deploy frontend ─▶ Smoke test
```

Ingenting byggs om. Production får imagerna med taggen `dev-verified`, alltså
byte för byte samma imagerna som just testades i development. Smoke-testet
kontrollerar att frontenden serveras, att den pekar på rätt backend, och att
backenden accepterar anrop från just den frontenden. Det skriver aldrig någon
data.

### Hur en deploy går till

GitHub Actions anropar Renders API med den image som ska köras och följer sedan
deployens status tills den är `live` eller har misslyckats. Utan den väntan hade
jobbet blivit grönt så fort anropet skickats, och testerna efteråt hade kört mot
den gamla versionen. Renders egen automatiska driftsättning används inte: all
driftsättning sker från pipelinen.

### Hur production skyddas

Två saker samverkar. Release-PR:en till `main` kräver att checken `End-to-end
tests against development` är grön, och den sätts bara när koden har driftsatts
och testats i development. Och det som driftsätts i production är de imagerna som
klarade den kontrollen, inte nya byggen av samma kod.

### Secrets och miljöer

GitHub Environments `development` och `production` innehåller samma namn med
olika värden:

| Namn | Typ |
|---|---|
| `RENDER_API_KEY` | secret |
| `RENDER_BACKEND_SERVICE_ID` | secret |
| `RENDER_FRONTEND_SERVICE_ID` | secret |
| `BACKEND_URL` | variable |
| `FRONTEND_URL` | variable |

Workflow-filerna vet inte vilken adress som hör till vilken miljö. Det avgörs av
vilken environment ett jobb deklarerar. Inga credentials finns i repot.

## Designbeslut och reflektion

### Ett repo, men oberoende delar

Frontend och backend ligger i samma repo. De är tätt kopplade via API:t: en
ändring i ett endpoint kräver nästan alltid en ändring i frontenden, och i ett
repo hamnar båda i samma Pull Request och testas tillsammans innan de mergas.

Den första versionen tog det ett steg för långt. Frontenden byggdes in i
backendens jar och allt driftsattes som en enda enhet. Det var enkelt, men det
betydde att en CSS-ändring körde alla Java-tester och driftsatte om backenden,
och att en README-ändring körde hela pipelinen.

Lösningen var inte att dela upp repot, utan att dela upp artefakterna. Koden
versioneras tillsammans, men frontend och backend byggs, testas och driftsätts
var för sig. Det ger det mesta av det två repon hade gett, utan att
end-to-end-tester och production-deploy behöver samordnas mellan repon.

Kostnaden är att de två delarna nu måste hitta varandra: CORS i backenden, en
API-adress per miljö i frontenden, och en risk att de hamnar i otakt mellan två
deployer. Den sista hanteras genom att backenden alltid deployas först och att
end-to-end-testerna alltid körs mot kombinationen som faktiskt ligger ute.

Två repon hade varit rätt val om frontend och backend ägdes av olika team med
olika releasetakt, om flera frontends använde samma API, eller om API:t var
versionerat och stabilt nog att ändras oberoende. Inget av det gäller här.

### Bygg en gång, flytta samma artefakt

En tidigare version byggde om imagerna när koden nådde `main`. Källkoden var
densamma som i development, men bygget var nytt, och det som testades var alltså
inte exakt det som driftsattes. Nu byggs varje image en enda gång, och production
får den som testats.

### Omställningen gjordes utan avbrott

Uppdelningen gjordes i två steg. Först fick frontenden en egen image och
backenden fick CORS, medan backenden fortfarande också serverade frontenden.
Först när den nya vägen fungerade togs den gamla bort. Inget slutade fungera
under tiden.

### Validering där data kommer in

All indata normaliseras och kontrolleras i request-objektet innan den når
servicelagret. Resten av koden kan därför lita på att en titel finns, har synlig
text och inte innehåller kontrolltecken. HTML och SQL-liknande text accepteras som
vanlig text: frontenden skriver ut den som text och aldrig som HTML, och
databasanropen använder parametrar.

### Kända begränsningar

- Databasen är in-memory, så data försvinner vid varje omstart och deploy. Ett
  medvetet val: fokus är CI/CD och inte databasinfrastruktur
- Build och tester körs både på Pull Requesten och efter merge. En merge-commit
  är ny kod som ingen testat i exakt den formen, men stegen skulle kunna lyftas ut
  till ett gemensamt reusable workflow i stället för att finnas i två filer
- End-to-end-testerna skapar och tar bort data i development
- Gratisnivån gör att tjänsterna somnar, vilket pipelinen kompenserar för genom
  att vänta tills de svarar