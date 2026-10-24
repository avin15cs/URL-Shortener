# 🔗 URL Shortener — Monorepo

A full-stack URL shortener application built as a monorepo with a **React + Vite** frontend and a **Spring Boot** backend.

## 📁 Project Structure

```
URL-Shortener/
├── backend/          # Spring Boot REST API (Java 17, Maven)
├── frontend/         # React + Vite SPA (TypeScript)
├── docker-compose.yml
└── README.md
```

## 🚀 Quick Start

### Prerequisites
- **Java 17+** and **Maven 3.8+**
- **Node.js 18+** and **npm 9+**
- **Docker** (optional, for containerized setup)

---

### Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

The API will start at **http://localhost:8080**

### Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

The UI will start at **http://localhost:5173**

---

## 🌐 API Endpoints

| Method | Endpoint              | Description              |
|--------|-----------------------|--------------------------|
| POST   | `/api/urls/shorten`   | Shorten a URL            |
| GET    | `/api/urls`           | List all shortened URLs  |
| GET    | `/api/urls/{id}`      | Get URL details by ID    |
| GET    | `/{shortCode}`        | Redirect to original URL |
| DELETE | `/api/urls/{id}`      | Delete a shortened URL   |
| GET    | `/api/urls/{id}/stats`| Get click statistics     |

## 🐳 Docker Compose

```bash
docker-compose up --build
```

## 🛠️ Tech Stack

| Layer     | Technology                        |
|-----------|-----------------------------------|
| Frontend  | React 18, Vite, TypeScript, Axios |
| Backend   | Spring Boot 3, Spring Data JPA    |
| Database  | H2 (dev) / PostgreSQL (prod)      |
| Container | Docker, Docker Compose            |
