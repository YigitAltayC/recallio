# recallio

Recallio repository is organized as an app workspace so backend, web, and desktop clients can live under the same GitHub repository.

## Structure

```text
recallio/
├─ apps/
│  ├─ backend/   # Spring Boot backend
│  ├─ web/       # Frontend app placeholder
│  └─ desktop/   # Desktop app placeholder
├─ LICENSE
└─ README.md
```

## Backend

The Spring Boot project still keeps its application identity as `recallio`. Only its folder location changed.

Run it from [apps/backend](C:\Users\bewar\IdeaProjects\Portfolio Projects\recallio\apps\backend):

```powershell
cd apps/backend
.\mvnw.cmd spring-boot:run
```
