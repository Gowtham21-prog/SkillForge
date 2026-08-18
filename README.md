# LearnHub — E-Learning Marketplace

A full-stack platform where instructors can list and sell courses, and students can browse,
purchase, and learn from them.

- **Frontend:** React 18 (React Router, Axios)
- **Backend:** Spring Boot 3 (Spring Security + JWT, Spring Data JPA)
- **Database:** MySQL 8

---

## Features

- Email/password auth with JWT, two roles: `STUDENT` and `INSTRUCTOR`
- Instructors can create, edit, and delete courses with multiple lectures, thumbnails, price, category, and level
- Students can browse/search courses, purchase them (simulated payment — see note below), track progress, and leave ratings/reviews
- File upload endpoint for course thumbnails
- Clean separation: REST API backend, single-page React frontend

---

## Project structure

```
elearning-platform/
├── backend/     Spring Boot API (Java 17, Maven)
└── frontend/    React app (Vite)
```

---

## 1. Backend setup

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8 running locally (or reachable)

### Configure the database

Create a database user/password or use root. Edit
`backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/elearning_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
```

The app will auto-create the database (`createDatabaseIfNotExist=true`) and all tables
(`spring.jpa.hibernate.ddl-auto=update`) on first run — no manual SQL needed. A `schema.sql`
file is included in `backend/` if you'd rather create tables manually.

### Run it

```bash
cd backend
mvn spring-boot:run
```

The API will start on **http://localhost:8080/api**.

### Key environment values to change before deploying

- `app.jwt.secret` — replace with a long random string
- `app.cors.allowed-origins` / the CORS config in `SecurityConfig.java` — set to your real frontend URL
- Database credentials

---

## 2. Frontend setup

### Prerequisites
- Node.js 18+
- npm

### Install & run

```bash
cd frontend
npm install
npm run dev
```

(`npm start` also works — it's aliased to the same Vite dev server.)

The app will start on **http://localhost:3000** and talks to the backend at
`http://localhost:8080/api` by default. To override, copy `.env.example` to `.env` and set
`VITE_API_URL` (Vite only exposes env vars prefixed with `VITE_` to client code).

### Build for production

```bash
npm run build
```

Outputs static files to `frontend/build/`, which you can serve from any static host or
behind Nginx. Preview the production build locally with `npm run preview`.

---

## 3. Using the app

1. Register an account — choose "Sell courses (Instructor)" or "Learn courses (Student)".
2. As an **instructor**: go to *Instructor studio* → *New course* → add title, price, lectures
   (with optional video URLs) → publish.
3. As a **student**: browse *Courses*, open a course, click **Buy this course** (or **Enroll
   for free** if price is 0). This creates an `Enrollment` record and a `Payment` record.
4. Purchased courses show up under *My learning*, where progress can be tracked and reviews
   left.

---

## Notes on payments

Real Stripe Checkout is wired in. Behavior depends on configuration:

- **Free courses** always enroll instantly — no payment needed.
- **Paid courses**, if `STRIPE_ENABLED=true` and `STRIPE_SECRET_KEY`/`STRIPE_WEBHOOK_SECRET`
  are set: the purchase button redirects to a real Stripe Checkout session. Stripe calls
  back to `POST /api/webhooks/stripe` on `checkout.session.completed`, which is what
  actually creates the enrollment (not the initial purchase request) — so a user only gets
  access once Stripe confirms the charge succeeded.
- **Paid courses**, if Stripe is *not* configured (the default for local dev): purchases
  fall back to the old simulated instant-success flow so you can still test the full
  student experience without a Stripe account.

To go live: create a Stripe account, set the three `STRIPE_*` env vars, and register a
webhook endpoint in the Stripe dashboard pointing at
`https://yourdomain.com/api/webhooks/stripe`, subscribed to `checkout.session.completed`.

## Notes on video hosting

Lecture `videoUrl` is just a string field — point it at any hosted video (YouTube unlisted
link, Vimeo, S3/CloudFront URL, Mux, etc.). There's no built-in video transcoding/hosting in
this scaffold.

## API overview

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | — | Create account |
| POST | `/auth/login` | — | Log in, get JWT |
| GET | `/courses` | — | List published courses |
| GET | `/courses/{id}` | — | Course detail |
| GET | `/courses/search?keyword=` | — | Search courses |
| POST | `/courses` | Instructor | Create course |
| PUT | `/courses/{id}` | Instructor (owner) | Update course |
| DELETE | `/courses/{id}` | Instructor (owner) | Delete course |
| GET | `/courses/instructor/mine` | Instructor | My listed courses |
| POST | `/enrollments/{courseId}/purchase` | Student | Buy/enroll — returns `{type: "ENROLLED"}` or `{type: "REDIRECT", url}` |
| POST | `/webhooks/stripe` | Stripe only (signature-verified) | Confirms payment, creates enrollment |
| GET | `/enrollments/mine` | Student | My enrolled courses |
| PATCH | `/enrollments/{courseId}/progress` | Student | Update progress % |
| GET | `/courses/{courseId}/reviews` | — | List reviews |
| POST | `/courses/{courseId}/reviews` | Student (enrolled) | Add/update review |
| POST | `/files/upload` | Any authenticated user | Upload thumbnail/asset |
| POST | `/auth/refresh` | — (refresh token) | Rotate access + refresh tokens |
| POST | `/auth/logout` | — (refresh token) | Revoke refresh token |
| POST | `/auth/verify-email` | — | Confirm email via token |
| POST | `/auth/resend-verification` | — | Resend verification email |
| POST | `/auth/forgot-password` | — | Request password reset email |
| POST | `/auth/reset-password` | — | Reset password via token |

All authenticated requests need `Authorization: Bearer <token>`. Access tokens expire in
15 minutes by default; the frontend automatically uses the refresh token to get a new one
transparently. Login/register endpoints are rate-limited per IP (5 login attempts / 15 min,
3 registrations / hour by default — configurable via env vars).
