# How to Capture the Test Screenshots (Section 8)

The report's Section 8 needs labelled screenshots. This page tells you exactly where each one comes
from. **There are two routes — you can use either, or both.**

- **Route A — Postman (recommended, looks best in the report).** One Send per test case; the
  screenshot shows the method, URL, request body and status code together.
- **Route B — the terminal (fastest).** One screenshot of the test script output covers the whole
  results table, and the evidence log already contains every request/response pair as text.

---

## Before you start (both routes)

```bash
# 1. Start MySQL from the XAMPP Control Panel.

# 2. Start the backend
cd EV-finder-api
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Wait for this line before taking any screenshots:

```
Started EvFinderApiApplication in ... seconds
```

> **Rebuild first if you have an older backend running.** A defect was found and fixed during
> testing: `DELETE /api/vehicles/{id}` used to answer **500** (instead of 409) for a vehicle with
> booking history. If your backend was started before that fix, TC-50 in the results table reports
> 500. Rebuild and restart (`mvnw spring-boot:run` picks up the compiled classes) so the fix is live.

---

## Route A — Postman

### A1. Get Postman
Download the **desktop** app from <https://www.postman.com/downloads/> and install it.
(Use the desktop app, not the browser version — the web version cannot reliably reach
`localhost:8080`.)

### A2. Import the collection
In Postman: **File → Import** (or the Import button, top-left) → **Files** →
select:

```
D:\EV finder\docs\EV-Finder.postman_collection.json
```

The collection appears in the left sidebar as **"EV Finder — CSE 2118 Project Tests"** with
8 folders.

### A3. Check the base URL
Click the collection name → **Variables** tab → confirm:

| Variable | Value |
|---|---|
| `baseUrl` | `http://localhost:8080` |

Change it only if your backend runs somewhere else.

### A4. Run these first, in this order (important)
The later requests need a token and some ids, which these save automatically:

1. **1. Auth → TC-01 Login as USER (200)** — press **Send**
2. **1. Auth → Login as OPERATOR (200)** — press **Send**
3. **1. Auth → Login as ADMIN (200)** — press **Send**
4. **2. Setup (run once, saves ids) → send all three requests** — press **Send** on each

> If you skip step 4, requests that use `{{stationId}}` or `{{serviceId}}` will fail with a
> missing-variable error.

### A5. Take the screenshot so it counts
For each test case you want in the report:

1. Open the request in the left sidebar.
2. In the **request** pane (top half), click the **Body** tab so the JSON payload is visible.
3. Click **Send**.
4. In the **response** pane (bottom half), stay on the **Body** tab and choose **Pretty**.
5. Screenshot **the whole window** (Win + Shift + S for a region, or Alt + PrtScn for the window).

The status code appears at the top-right of the response pane. A good screenshot shows **all four**:
the method + URL at the top, the request body, the status code, and the response body. Cropping out
the status code is the most common reason marks are lost.

### A6. Which requests map to which figure

| Figure | Folder → request |
|---|---|
| Figure 4 | `1. Auth` → **TC-01 Login as USER (200)** |
| Figure 5 | `6. Bookings` → **TC-16 POST /api/bookings (201)** |
| Figure 6 | `6. Bookings` → **TC-24 Capacity exceeded (409)** — just press Send (it fills capacity itself, see below) |
| Figure 7 | `7. Payment` → **TC-25 Pay a booking — SUCCESS** |
| Figure 8 | `7. Payment` → **TC-26 Forced failure — FAILED** |
| Figure 9 | `5. Stations & availability` → **TC-13 GET unknown station (404)** |
| Figure 10 | `3. Security (401 / 403)` → **TC-33 USER calling admin endpoint (403)** |
| Figure 11 | `3. Security (401 / 403)` → **TC-03 GET /api/users/me without token (401)** |

Every request has an assertion, so the **Test Results** tab (next to Body in the response pane) also
shows a green PASS — a second screenshot per case is optional but strengthens the evidence.

### A7. Two screenshots that don't come from Postman

| Figure | Where |
|---|---|
| Figure 2 — application started | The terminal window running `mvnw spring-boot:run`, showing `Tomcat started on port 8080` |
| Figure 3 — database tables | Browser → <http://localhost/phpmyadmin> → database `ev_finder` → open the `bookings` table so rows are visible |

---

## A8. Troubleshooting: why a case shows the wrong status

Three cases depend on server **state**, not just on a valid request. Each now sets up its own state
with a pre-request script, so they run correctly on their own — but the reasons are worth knowing,
because they are real API behaviours:

| Case | If it returned the wrong status before | Why | Now |
|---|---|---|---|
| **TC-23** (other user's booking → 403) | 404 instead of 403 | `{{bookingId}}` was still empty, so the URL was `/api/bookings/` — an empty id matches no booking. The 403 you expected needs a *real* booking owned by someone else. | The pre-request script picks one of the user's existing bookings if none is stored |
| **TC-24** (capacity exceeded → 409) | 201 instead of 409 | Capacity was not full. The service holds **2** slots, so a single booking leaves a free slot and the next request is legitimately accepted. | The pre-request script books the 05:00-06:00 window until the service reports 409, then the case asks for one more |
| **TC-26** (forced failure → FAILED) | 400 instead of FAILED | It was reusing the same booking that **TC-25 had already paid**. A booking can only be paid once, so the API correctly answered `400 "Booking is already paid and confirmed"`. | The pre-request script creates a dedicated unpaid booking (12:00-13:00) |

None of these were API bugs — in each case the API was enforcing a rule correctly. They were
deficiencies in how the collection set up its data, which is a useful thing to note in the report's
testing section: *a test that depends on hidden state is a fragile test.*

**Postman's Console** (bottom-left → Console) prints what each pre-request script did, for example
`TC-24: window full after 2 booking(s); 409 expected next`. If a case still fails, open the Console
and read that line first.

### Re-running the suite

The pre-request scripts create bookings, and those bookings hold capacity permanently (that is the
point of the rule). Variables are cached, so a second run reuses the same ids and still passes.
If you want a completely fresh run:

1. Collection → **Variables** tab → **Reset All** (or delete the values for `stationId`,
   `serviceId`, `vehicleId`, `slotDate`, `bookingId`, `payOkBookingId`, `failBookingId`).
2. Re-run folder **1. Auth**, then **2. Setup**, then the cases you need.

A *third* run of TC-24 may report `409` even in its pre-request script, which is fine — the script
stops as soon as it sees that status and the case still expects 409.

---

## Route B — the terminal (fastest, one screenshot for the whole table)

```bash
cd "D:/EV finder"
bash docs/run-api-tests.sh http://localhost:8080
```

This authenticates the three demo accounts, runs all 49 cases, and prints the results table:

```
| TC | Method | Endpoint | Exp | Act | Result |
|----|--------|----------|-----|-----|--------|
| TC-01  | POST   | /api/auth/login                                | 200 | 200 | PASS |
...
TOTAL: 49   PASSED: 49   FAILED: 0
```

Screenshot that output — **one image documents the entire Section 8.2 table**. The same run writes
every request and response body to:

```
D:\EV finder\docs\test-evidence.log
```

That file is text, so you can paste individual cases into an appendix instead of screenshotting
them, e.g.:

```
### TC-24  POST /api/bookings
expected: 409   actual: 409   -> PASS
response: {"timestamp":"...","status":409,"error":"Conflict",
           "message":"No free slots left for the selected time","path":"/api/bookings"}
```

---

## What already exists vs what you must capture

| Evidence | Status |
|---|---|
| `docs/test-evidence.log` — all 49 request/response pairs as text | ✅ already generated |
| Results table (expected vs actual vs PASS) | ✅ already in report Section 8.2 (reproducible with the script) |
| Postman collection with assertions for every case | ✅ `docs/EV-Finder.postman_collection.json` |
| **Postman screenshots (Figures 4–11)** | ❌ you must capture these — they need your screen |
| **Console screenshot (Figure 2)** | ❌ you must capture this |
| **phpMyAdmin screenshot (Figure 3)** | ❌ you must capture this |

Figures 1 (architecture diagram) and 12 (Android app) are also yours to produce: redraw Figure 1 in
draw.io from the diagram in Section 4, and screenshot the app's Home screen with the backend running.

---

## After capturing

1. Insert each image under its `[SCREENSHOT — Figure N: …]` marker in
   `docs/Spring_Boot_Project_Report.md` with the caption above it.
2. Delete every remaining `[SCREENSHOT …]` and `[FILL IN …]` marker.
3. Fill the cover page: Student ID, Section, Submitted To, Date of Submission.
4. Export to PDF named `StudentID_Name_SpringBootProject.pdf`.
