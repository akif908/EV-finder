# How to Capture the Test Screenshots (Section 8)

The report's Section 8 needs labelled screenshots. This page tells you exactly where each one comes
from. **There are two routes — you can use either, or both.**

- **Route A — Postman (recommended, looks best in the report).** One Send per test case; the
  screenshot shows the method, URL, request body and status code together.
- **Route B — the terminal (fastest).** One screenshot of the test script output covers the whole
  results table, and the evidence log already contains every request/response pair as text.

> Running the **whole collection in one pass** (Collection Runner) is covered in
> [A0](#a0-running-the-whole-collection-at-once-collection-runner). The same instructions are
> embedded in the collection's own description, so you can read them inside Postman.

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
| Figure 5 | `5. Bookings` → **TC-16 POST /api/bookings (201)** |
| Figure 6 | `5. Bookings` → **TC-24 Capacity exceeded (409)** — to see the 409 you must first press Send on the three **Fill capacity** requests above it |
| Figure 7 | `6. Payment` → **Setup: create booking to pay**, then **TC-25 Pay a booking — SUCCESS** |
| Figure 8 | `6. Payment` → **Setup: create booking for forced failure**, then **TC-26 Forced failure — FAILED** |
| Figure 9 | `4. Stations & availability` → **TC-13 GET unknown station (404)** |
| Figure 10 | `7. Security (401 / 403)` → **TC-33 USER calling admin endpoint (403)** |
| Figure 11 | `7. Security (401 / 403)` → **TC-03 GET /api/users/me without token (401)** |

Every request has an assertion, so the **Test Results** tab (next to Body in the response pane) also
shows a green PASS — a second screenshot per case is optional but strengthens the evidence.

### A7. Two screenshots that don't come from Postman

| Figure | Where |
|---|---|
| Figure 2 — application started | The terminal window running `mvnw spring-boot:run`, showing `Tomcat started on port 8080` |
| Figure 3 — database tables | Browser → <http://localhost/phpmyadmin> → database `ev_finder` → open the `bookings` table so rows are visible |

---

## A0. Running the whole collection at once (Collection Runner)

Use this to prove all 47 requests pass in one go. For the report's figures you still need the
individual requests from A5 — the Runner is for the pass/fail evidence, not for the screenshots.

1. **Start the stack first.** MySQL (XAMPP) and the backend — rebuilt from the current source, see
   the note below — then confirm the API answers:
   ```bash
   curl -i http://localhost:8080/api/stations      # expect 401 Unauthorized
   ```
2. **Hover the collection name** in the left sidebar → click **⋯** (More actions) → **Run collection**.
   In older versions, click the collection and press **Run** on the Overview tab.
3. **Keep the defaults:** Iterations `1`, Delay `0`. **Do not reorder the folders** — folder 1 saves
   the JWTs and folder 2 saves the station/service/vehicle ids that every later request depends on.
   Running out of order is the single most common cause of failures.
4. Optionally tick **Save responses** so you can open each response body afterwards.
5. Press **Run EV Finder — CSE 2118 Project Tests**.
6. Read the results table: every request shows **PASS/FAIL**, the status code, time and size.
   Expect **0 failures**. Click any row to reopen that request.

**Worked example — what a clean run looks like:**

```
Folder                                    Requests   Failures
1. Auth                                      5          0
2. Setup (run once, saves ids)               3          0
3. Vehicles (CRUD + 404)                     5          0
4. Stations & availability                   5          0
5. Bookings (create / limits / conflict)    11          0
6. Payment (simulated)                       5          0
7. Security (401 / 403)                      5          0
8. Reviews, notifications, issues, fuel…     7          0
9. Teardown (optional cleanup)               1          0
                                           ──         ──
                                           46          0
```

Two folders contain **setup requests** that must run before the case they support — they are
already in the right position, so a top-to-bottom run needs no thought:

| Setup request | Supports | Why |
|---|---|---|
| `Fill capacity 1/2/3` (folder 5) | TC-24 → 409 | Takes every slot in the 05:00–06:00 window. With a capacity of 2 the window is certainly full, so the next booking must be refused. On later runs they simply report 409, which is still correct. |
| `Setup: create booking to pay` (folder 6) | TC-25 → SUCCESS | A booking can only be paid **once**, so this case needs its own unused booking. |
| `Setup: create booking for forced failure` (folder 6) | TC-26 → FAILED | Same reason, and it must not touch the booking TC-25 paid. |

Folder **9. Teardown** deletes the test vehicles. Untick it if you want to inspect the created
vehicle afterwards, or run it later on its own.

### Running the collection outside Postman (optional)

`docs/validate_collection.js` stubs the Postman `pm` API, runs every request in order and reports
each assertion. It needs no Postman installation and exits non-zero on failure:

```bash
node docs/validate_collection.js http://localhost:8080
# assertions: 59   passed: 59   failed: 0
# COLLECTION OK
```

This exists because a bug in the collection's own scripts cost two rounds of debugging: a line like
`pm.collectionVariables.set('x', ...);_start` is *syntactically* valid, so a syntax check passes it,
but it throws at runtime — Postman then aborts the request ("No response") and the variable stays
empty. Only executing the scripts catches that, which this harness does.

### Exporting the run results (optional extra evidence)

In the Runner, after the run finishes: **Export Results** → save the JSON, or **Ctrl+P** the results
pane to PDF. Both are legitimate evidence, but they do *not* replace the Figure 4–11 screenshots,
because they do not show the request body.

### If a request fails in the Runner

| Symptom | Cause | Fix |
|---|---|---|
| Many failures at once, all `401` or "missing variable" | Folders ran out of order, or you started at folder 3+ | Run again from the top, or run folders 1 and 2 first |
| `TC-06` → `409` | A vehicle with that registration number survived an earlier run (the column is globally unique) | Now impossible: TC-06 generates a unique number and sweeps leftovers. If you still see it, your imported collection is the older copy — re-import |
| `TC-24` → `201` | The service is not full, so the booking is legitimately accepted | Run the three **Fill capacity** requests above TC-24 first (a top-to-bottom run does this for you) |
| `TC-25` or `TC-26` → `404` | The payment URL still held the literal `{{payOkBookingId}}` / `{{failBookingId}}` because nothing had set it | Run the matching `Setup: create booking…` request immediately above the case. A top-to-bottom run does this automatically |
| A request shows **"No response"** | A script in it threw, so Postman aborted the request before sending it | Check the Postman Console for the error. Re-import the collection — this was caused by a bug in an older copy of the setup requests |
| `TC-50` → `409` is expected; `TC-50` → `500` | 500 means the backend predates the delete-guard fix | Rebuild and restart the backend |
| `TC-26` → `400 "already paid"` | It was pointed at a booking TC-25 had paid | Fixed by the dedicated setup request. Re-import if you still see this |
| `TC-50` → `500` | Your backend predates the delete-guard fix | Rebuild and restart the backend |
| `TC-23` → `404` | No booking id and the user has no bookings | Run folder 6 (TC-16) first, or let the pre-request script find one |

Open the **Postman Console** (bottom-left → Console) to see exactly what each pre-request script did.

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
