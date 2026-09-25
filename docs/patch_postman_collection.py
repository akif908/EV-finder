"""Make the collection deterministic in the Postman Collection Runner.

Problem observed in the Runner: TC-25 (and TC-26) failed with 404 because the
payment URL contained an unresolved {{payOkBookingId}}. Those ids were created
in a *pre-request* script using pm.sendRequest, which is asynchronous — in the
Runner the callback can fire after the request has already been sent, so the
variable was still empty. TC-23 only appeared to work because its variable had
been left set by an earlier manual run.

Fix: no request depends on asynchronous HTTP any more.

  1. Explicit, ordered setup requests create the bookings the payment cases
     need ("Setup: create booking to pay", "Setup: create booking for forced
     failure"), and their *test* scripts set the variables synchronously.
  2. Capacity for TC-24 is filled by three explicit ordered requests instead of
     an async loop.
  3. TC-16 generates a random future window in its pre-request (pure
     computation, always safe) so repeated runs never collide.
  4. TC-25/TC-26 gained a guard that aborts with a readable message when their
     booking id is missing, instead of silently sending a literal variable.
  5. Folders are reordered so state-producing folders run before consumers:
     Bookings and Payment now come before Security, because TC-23 needs a
     booking to exist.

Run:  python docs/patch_postman_collection.py
"""
import collections
import io
import json

PATH = "docs/EV-Finder.postman_collection.json"
OD = collections.OrderedDict

coll = json.load(io.open(PATH, encoding="utf-8"), object_pairs_hook=OD)

for key in ("bkStart", "bkEnd", "payOkBookingId", "failBookingId",
            "testRegNo", "createdVehicleId"):
    if key not in {v["key"] for v in coll["variable"]}:
        coll["variable"].append(OD([("key", key), ("value", "")]))


def find(items, prefix):
    for item in items:
        if "item" in item:
            hit = find(item["item"], prefix)
            if hit:
                return hit
        elif item.get("name", "").startswith(prefix):
            return item
    return None


def folder(name):
    for item in coll["item"]:
        if "item" in item and item.get("name", "").startswith(name):
            return item
    return None


def script(lines):
    return OD([("type", "text/javascript"), ("exec", lines)])


def set_events(request, prereq=None, tests=None):
    events = []
    if prereq:
        events.append(OD([("listen", "prerequest"), ("script", script(prereq))]))
    if tests:
        events.append(OD([("listen", "test"), ("script", script(tests))]))
    if events:
        request["event"] = events
    else:
        request.pop("event", None)


def build(name, method, path_suffix, token_var, body=None, prereq=None,
          tests=None, description=None):
    req = OD([
        ("method", method),
        ("header", [
            OD([("key", "Authorization"), ("value", "Bearer {{%s}}" % token_var)]),
            OD([("key", "Content-Type"), ("value", "application/json")]),
        ]),
        ("url", OD([
            ("raw", "{{baseUrl}}" + path_suffix),
            ("host", ["{{baseUrl}}"]),
            ("path", [s for s in path_suffix.strip("/").split("/")]),
        ])),
    ])
    if body is not None:
        req["body"] = OD([("mode", "raw"), ("raw", body)])
    if description:
        req["description"] = description
    item = OD([("name", name), ("request", req)])
    set_events(item, prereq, tests)
    return item


# ---------------------------------------------------------------------------
# A. TC-06 — compute the unique registration number only (no async HTTP).
#    Cleanup is delegated to the teardown folder, which runs at the end.
# ---------------------------------------------------------------------------
tc06 = find(coll["item"], "TC-06")
set_events(
    tc06,
    prereq=[
        "// A fresh registration number per run. registration_no is GLOBALLY unique,",
        "// so a fixed value made a second run fail with 409 against the row left by",
        "// the previous run. This is pure computation, so it is always safe.",
        "pm.collectionVariables.set('testRegNo', 'TST-' + Date.now().toString().slice(-8));",
        "console.log('TC-06: registration for this run = ' + pm.collectionVariables.get('testRegNo'));",
    ],
    tests=[
        "pm.test('TC-06 status is 201', () => pm.response.to.have.status(201));",
        "const v = pm.response.json();",
        "pm.test('registration number is this run\\'s value',",
        "    () => pm.expect(v.registrationNo).to.eql(pm.collectionVariables.get('testRegNo')));",
        "pm.collectionVariables.set('createdVehicleId', v.id);",
    ],
)

# ---------------------------------------------------------------------------
# B. TC-16 — random future window, so repeats never collide
# ---------------------------------------------------------------------------
tc16 = find(coll["item"], "TC-16")
set_events(
    tc16,
    prereq=[
        "// Pick a random future window so repeated runs never collide. Capacity is",
        "// finite, so a fixed window would fill up and TC-16 would start answering 409.",
        "const day = new Date(Date.now() + (3 + Math.floor(Math.random() * 200)) * 864e5);",
        "const date = day.toISOString().slice(0, 10);",
        "const hour = Math.floor(Math.random() * 22);",
        "const pad = n => String(n).padStart(2, '0');",
        "pm.collectionVariables.set('bkStart', date + 'T' + pad(hour) + ':00:00');",
        "pm.collectionVariables.set('bkEnd', date + 'T' + pad(hour + 1) + ':00:00');",
        "console.log('TC-16: booking window ' + pm.collectionVariables.get('bkStart'));",
    ],
    tests=[
        "pm.test('TC-16 status is 201', () => pm.response.to.have.status(201));",
        "const b = pm.response.json();",
        "pm.test('starts as PENDING', () => pm.expect(b.status).to.eql('PENDING'));",
        "pm.collectionVariables.set('bookingId', b.id);",
    ],
)
tc16["request"]["body"]["raw"] = (
    "{\n"
    '  "vehicleId": "{{vehicleId}}",\n'
    '  "serviceId": "{{serviceId}}",\n'
    '  "startTime": "{{bkStart}}",\n'
    '  "endTime": "{{bkEnd}}"\n'
    "}"
)
tc16["request"]["description"] = (
    "Expected 201 Created with status PENDING.\n\n"
    "Picks a random free window each run, so the case keeps passing no matter how often the "
    "collection is run. Saves the id as bookingId for the later cases."
)

# ---------------------------------------------------------------------------
# C. Build the reordered collection
# ---------------------------------------------------------------------------
auth = folder("1. Auth")
setup = folder("2. Setup")
vehicles = folder("4. Vehicles")
stations = folder("5. Stations")
bookings_old = folder("6. Bookings")
payment_old = folder("7. Payment")
security_old = folder("3. Security")
misc = folder("8. Reviews")
teardown = folder("9. Teardown")

tc17 = find(coll["item"], "TC-17")
tc18 = find(coll["item"], "TC-18")
tc19 = find(coll["item"], "TC-19")
tc20 = find(coll["item"], "TC-20")
tc22 = find(coll["item"], "TC-22")
tc24 = find(coll["item"], "TC-24")
tc27 = find(coll["item"], "TC-27")
tc23 = find(coll["item"], "TC-23")

# --- fillers: explicit and ordered, so capacity is genuinely full ---------
FILL_WINDOW_START = "{{slotDate}}T05:00:00"
FILL_WINDOW_END = "{{slotDate}}T06:00:00"
fillers = []
for i in (1, 2, 3):
    fillers.append(build(
        "Fill capacity %d (setup for TC-24)" % i,
        "POST", "/api/bookings", "userToken",
        body=("{\n"
              '  "vehicleId": "{{vehicleId}}",\n'
              '  "serviceId": "{{serviceId}}",\n'
              '  "startTime": "%s",\n'
              '  "endTime": "%s"\n'
              "}" % (FILL_WINDOW_START, FILL_WINDOW_END)),
        tests=[
            "// 201 = a slot was taken, 409 = the window was already full from an",
            "// earlier run. Both are fine; what matters is that it is full for TC-24.",
            "pm.test('capacity filler accepted',",
            "    () => pm.expect([201, 409]).to.include(pm.response.code));",
            "console.log('filler: HTTP ' + pm.response.code);",
        ],
        description=("Takes one slot in the 05:00-06:00 window. Run three of these in order: with a "
                     "capacity of 2 the window is guaranteed to be full, which is what makes TC-24 "
                     "return 409. On later runs they simply report 409 because it is already full."),
    ))

tc24["request"]["body"]["raw"] = (
    "{\n"
    '  "vehicleId": "{{vehicleId}}",\n'
    '  "serviceId": "{{serviceId}}",\n'
    '  "startTime": "{{slotDate}}T05:00:00",\n'
    '  "endTime": "{{slotDate}}T06:00:00"\n'
    "}"
)
set_events(
    tc24,
    tests=[
        "pm.test('TC-24 status is 409', () => pm.response.to.have.status(409));",
        "pm.test('message explains the conflict',",
        "    () => pm.expect(pm.response.json().message).to.contain('No free slots'));",
    ],
)
tc24["request"]["description"] = (
    "Expected 409 Conflict with \"No free slots left for the selected time\".\n\n"
    "The three 'Fill capacity' requests above must run first — they guarantee the 05:00-06:00 "
    "window is at capacity, which is the condition this case tests."
)

bookings = OD([
    ("name", "5. Bookings (create / limits / conflict)"),
    ("description", "Booking lifecycle. Order matters: TC-16 creates the booking the later cases "
                    "use, and the fillers below must run before TC-24."),
    ("item", [tc16, tc17, tc18, tc19] + fillers + [tc24, tc20, tc22]),
])

# --- payment: explicit booking creation, no async ------------------------
def booking_create(name, hour, id_var, label):
    return build(
        name, "POST", "/api/bookings", "userToken",
        body=("{\n"
              '  "vehicleId": "{{vehicleId}}",\n'
              '  "serviceId": "{{serviceId}}",\n'
              '  "startTime": "{{slotDate}}T%s:00:00",\n'
              '  "endTime": "{{slotDate}}T%s:00:00"\n'
              "}" % (hour, "%02d" % (int(hour) + 1))),
        prereq=[
            "// Random window per run: a booking can only be paid once, and a paid",
            "// booking keeps its slot, so a fixed window would fill up over runs.",
            "const day = new Date(Date.now() + (3 + Math.floor(Math.random() * 200)) * 864e5);",
            "const date = day.toISOString().slice(0, 10);",
            "const hour = %d;" % int(hour),
            "const pad = n => String(n).padStart(2, '0');",
            "pm.collectionVariables.set('%s', date + 'T' + pad(hour) + ':00:00');" % id_var + "_start",
            "pm.collectionVariables.set('%s', date + 'T' + pad(hour + 1) + ':00:00');" % id_var + "_end",
        ],
        tests=[
            "pm.test('%s status is 201', () => pm.response.to.have.status(201));" % name.split()[0],
            "const b = pm.response.json();",
            "pm.collectionVariables.set('%s', b.id);" % id_var,
            "console.log('created booking %s for %s' .replace('%s','') + b.id);",
        ],
        description="Creates a dedicated PENDING booking for %s and saves it as %s." % (label, id_var),
    )

# fix the booking_create bodies to use the generated window variables
def retarget(req, start_var, end_var):
    req["request"]["body"]["raw"] = (
        "{\n"
        '  "vehicleId": "{{vehicleId}}",\n'
        '  "serviceId": "{{serviceId}}",\n'
        '  "startTime": "{{%s}}",\n'
        '  "endTime": "{{%s}}"\n'
        "}" % (start_var, end_var)
    )
    return req

create_ok = retarget(booking_create(
    "Setup: create booking to pay", 10, "payOkBookingId", "the success payment"),
    "payOkBookingId_start", "payOkBookingId_end")
create_fail = retarget(booking_create(
    "Setup: create booking for forced failure", 12, "failBookingId", "the failure payment"),
    "failBookingId_start", "failBookingId_end")
# tidy the noisy console line
for r in (create_ok, create_fail):
    for e in r["event"]:
        if e["listen"] == "test":
            e["script"]["exec"] = [l for l in e["script"]["exec"] if "console.log" not in l]

tc25 = find(coll["item"], "TC-25")
tc26 = find(coll["item"], "TC-26")

guard = lambda var, name: [
    "// Fail with a readable message instead of sending a literal {{%s}}," % var,
    "// which would come back as a confusing 404.",
    "const id = pm.collectionVariables.get('%s');" % var,
    "if (!id) {",
    "    console.error('%s: %s is empty — run \"%s\" first');" % (name, var, name),
    "    pm.collectionVariables.set('%s', 'missing-run-the-setup-request-first');" % var,
    "}",
]
set_events(
    tc23,
    prereq=[
        "// TC-23 needs a booking owned by the USER. The Bookings folder runs before",
        "// this one and stores it, so no network call is needed here — a pre-request",
        "// script that awaits HTTP is unreliable in the Collection Runner.",
        "const id = pm.collectionVariables.get('bookingId');",
        "if (!id) {",
        "    console.error('TC-23: bookingId is empty — run folder 5 (TC-16) first');",
        "}",
    ],
    tests=[
        "pm.test('TC-23 status is 403', () => pm.response.to.have.status(403));",
        "pm.test('the API explains that this is not your booking',",
        "    () => pm.expect(pm.response.json().message).to.contain('own bookings'));",
    ],
)
tc23["request"]["description"] = (
    "Expected 403 Forbidden: the operator is authenticated but does not own this booking. "
    "Runs after the Bookings folder, which stores bookingId. Ownership is checked before status, "
    "so this returns 403 even for a cancelled booking."
)

set_events(
    tc25,
    prereq=guard("payOkBookingId", "TC-25"),
    tests=[
        "pm.test('TC-25 status is 200', () => pm.response.to.have.status(200));",
        "pm.test('TC-25 payment SUCCESS',",
        "    () => pm.expect(pm.response.json().status).to.eql('SUCCESS'));",
    ],
)
set_events(
    tc26,
    prereq=guard("failBookingId", "TC-26"),
    tests=[
        "pm.test('TC-26 status is 200', () => pm.response.to.have.status(200));",
        "pm.test('TC-26 payment FAILED',",
        "    () => pm.expect(pm.response.json().status).to.eql('FAILED'));",
    ],
)
tc25["request"]["description"] = (
    "Expected 200 with \"status\": \"SUCCESS\" and a SIM-<reference> transaction number.\n\n"
    "Run the 'Setup: create booking to pay' request immediately above first — it creates a fresh "
    "PENDING booking and saves payOkBookingId. A booking can only be paid once, which is why the "
    "collection creates its own instead of reusing another case's booking."
)
tc26["request"]["description"] = (
    "Expected 200 with \"status\": \"FAILED\" — the simulated gateway's failure path, which releases "
    "the slot.\n\nRun 'Setup: create booking for forced failure' first; it saves failBookingId, a "
    "booking that no other case has paid."
)

payment = OD([
    ("name", "6. Payment (simulated)"),
    ("description", "Runs after Bookings so the ids exist. Each payment case is preceded by the "
                    "request that creates its booking."),
    ("item", [create_ok, tc25, create_fail, tc26, tc27]),
])

security = OD([
    ("name", "7. Security (401 / 403)"),
    ("description", "Authorisation cases. Runs after Bookings because TC-23 needs a booking to "
                    "exist in order to be forbidden from reading it."),
    ("item", security_old["item"]),
])

# --- rebuild the top-level order, with names matching the new positions ---
auth["name"] = "1. Auth"
setup["name"] = "2. Setup (run once, saves ids)"
vehicles["name"] = "3. Vehicles (CRUD + 404)"
stations["name"] = "4. Stations & availability"
misc["name"] = "8. Reviews, notifications, issues, fuel, news"
teardown["name"] = "9. Teardown (optional cleanup)"

coll["item"] = [auth, setup, vehicles, stations, bookings, payment, security, misc, teardown]

coll["info"]["description"] = coll["info"]["description"].replace(
    "1. Run the three logins in folder 1, then the three requests in folder 2 (Setup) -",
    "1. Run the three logins in folder 1, then the three requests in folder 2 (Setup) -")

io.open(PATH, "w", encoding="utf-8").write(json.dumps(coll, indent=2) + "\n")
print("collection rebuilt: no request depends on asynchronous HTTP")
