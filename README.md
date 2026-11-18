# CollegeBot (Spring Boot + Gemini AI + Docker + Cloud)

Local run (Java 17 + Maven needed):
    set GEMINI_API_KEY=your_key      (Windows CMD)
    mvn spring-boot:run
Open http://localhost:8080

Get a free API key: aistudio.google.com -> Get API key

Cloud deploy (Render, free):
1. Upload everything inside this folder to a GitHub repo (pom.xml, Dockerfile, src at the root)
2. render.com -> New -> Web Service -> select repo -> Language: Docker -> Free plan
3. Environment -> add GEMINI_API_KEY = your key
4. Deploy, then open the URL. Header should show "AI ON".

API: POST /api/chat, GET/DELETE /api/history, GET /api/stats, GET /api/health
