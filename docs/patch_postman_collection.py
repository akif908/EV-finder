"""Make the Postman collection's state-dependent cases self-contained.

TC-23, TC-24 and TC-26 previously assumed the caller had already created a
booking (TC-16) or was running on a service whose capacity was already full.
Each now carries a pre-request script that establishes the state it needs:

  TC-23  picks one of the user's existing bookings if bookingId is unset
  TC-24  fills its own window (05:00-06:00) until the service reports 409
  TC-25  creates a dedicated unpaid booking (11:00-12:00) to pay
  TC-26  creates a dedicated unpaid booking (12:00-13:00) for the failure path

Run:  python docs/patch_postman_collection.py
"""
import collections
import io
import json

PATH = "docs/EV-Finder.postman_collection.json"
OD = collections.OrderedDict

coll = json.load(io.open(PATH, encoding="utf-8"), object_pairs_hook=OD)

# ---- extra collection variables ------------------------------------------
existing = {v["key"] for v in coll["variable"]}
for key in ("payOkBookingId", "failBookingId"):
    if key not in existing:
        coll["variable"].append(OD([("key", key), ("value", "")]))


def find(items, prefix):
    """Depth-first lookup of a request whose name starts with `prefix`."""
    for item in items:
        if "item" in item:
            hit = find(item["item"], prefix)
            if hit:
                return hit
        elif item.get("name", "").startswith(prefix):
            return item
    return None


def set_prereq(request, lines):
    """Attach (or replace) a pre-request script built from a list of lines."""
    request.setdefault("event", [])
    request["event"] = [e for e in request["event"] if e["listen"] != "prerequest"]
    request["event"].insert(0, OD([
        ("listen", "prerequest"),
        ("script", OD([("type", "text/javascript"), ("exec", lines)])),
    ]))


def set_url(request, path_suffix):
    """Rewrite a request URL to {{baseUrl}} + path_suffix."""
    request["url"] = OD([
        ("raw", "{{baseUrl}}" + path_suffix),
        ("host", ["{{baseUrl}}"]),
        ("path", [seg for seg in path_suffix.strip("/").split("/")]),
    ])


# ---------------------------------------------------------------------------
# TC-23 — 403 when another user fetches a booking they do not own
# ---------------------------------------------------------------------------
tc23 = find(coll["item"], "TC-23")
set_prereq(tc23, [
    "// TC-23 needs a booking owned by the USER, not the operator.",
    "// If none is stored yet, take the user's most recent booking so the request",
    "// also works when run straight after the logins.",
    "const base = pm.collectionVariables.get('baseUrl');",
    "if (!pm.collectionVariables.get('bookingId')) {",
    "    pm.sendRequest({",
    "        url: base + '/api/bookings/my',",
    "        method: 'GET',",
    "        header: { 'Authorization': 'Bearer ' + pm.collectionVariables.get('userToken') }",
    "    }, (err, res) => {",
    "        const list = res && res.json ? res.json() : [];",
    "        if (Array.isArray(list) && list.length) {",
    "            pm.collectionVariables.set('bookingId', list[0].id);",
    "            console.log('TC-23: using booking ' + list[0].id + ' owned by the user');",
    "        } else {",
    "            console.log('TC-23: no bookings found — run folder 6, TC-16 first');",
    "        }",
    "    });",
    "}",
])
tc23["request"]["description"] = (
    "Expected 403 Forbidden: the operator is authenticated but does not own this booking.\n\n"
    "Self-contained — if no bookingId is stored, the pre-request script picks one of the user's "
    "existing bookings. Ownership is checked before status, so this returns 403 even for a "
    "cancelled booking. Needs the user and operator tokens from folder 1."
)

# ---------------------------------------------------------------------------
# TC-24 — 409 when the requested window is already at capacity
# ---------------------------------------------------------------------------
tc24 = find(coll["item"], "TC-24")
set_prereq(tc24, [
    "// TC-24 only returns 409 when the service is FULL for the requested window.",
    "// This pre-request fills capacity in a dedicated window (05:00-06:00) so the",
    "// case stands alone and never eats into the window TC-16 uses.",
    "const base = pm.collectionVariables.get('baseUrl');",
    "const payload = JSON.stringify({",
    "    vehicleId: pm.collectionVariables.get('vehicleId'),",
    "    serviceId: pm.collectionVariables.get('serviceId'),",
    "    startTime: pm.collectionVariables.get('slotDate') + 'T05:00:00',",
    "    endTime:   pm.collectionVariables.get('slotDate') + 'T06:00:00'",
    "});",
    "let attempt = 0;",
    "(function fill() {",
    "    if (attempt++ >= 8) {",
    "        console.log('TC-24: could not fill capacity in 8 attempts');",
    "        return;",
    "    }",
    "    pm.sendRequest({",
    "        url: base + '/api/bookings',",
    "        method: 'POST',",
    "        header: { 'Content-Type': 'application/json',",
    "                  'Authorization': 'Bearer ' + pm.collectionVariables.get('userToken') },",
    "        body: { mode: 'raw', raw: payload }",
    "    }, (err, res) => {",
    "        if (res && res.code === 409) {",
    "            console.log('TC-24: window full after ' + (attempt - 1) + ' booking(s); 409 expected next');",
    "        } else {",
    "            fill();",
    "        }",
    "    });",
    "})();",
])
tc24["request"]["body"]["raw"] = (
    "{\n"
    '  "vehicleId": "{{vehicleId}}",\n'
    '  "serviceId": "{{serviceId}}",\n'
    '  "startTime": "{{slotDate}}T05:00:00",\n'
    '  "endTime": "{{slotDate}}T06:00:00"\n'
    "}"
)
tc24["request"]["description"] = (
    "Expected 409 Conflict with the message \"No free slots left for the selected time\".\n\n"
    "Self-contained — the pre-request script reserves the 05:00-06:00 window until the service is "
    "full, then this request asks for one more slot. This is the double-booking rule under test."
)

# ---------------------------------------------------------------------------
# TC-25 — SUCCESS on a freshly created booking
# ---------------------------------------------------------------------------
tc25 = find(coll["item"], "TC-25")
set_prereq(tc25, [
    "// A booking can only be paid once: paying it again returns",
    "// 400 'Booking is already paid and confirmed'. So this case creates its own",
    "// dedicated booking the first time it runs.",
    "const base = pm.collectionVariables.get('baseUrl');",
    "if (pm.collectionVariables.get('payOkBookingId')) return;",
    "pm.sendRequest({",
    "    url: base + '/api/bookings',",
    "    method: 'POST',",
    "    header: { 'Content-Type': 'application/json',",
    "              'Authorization': 'Bearer ' + pm.collectionVariables.get('userToken') },",
    "    body: { mode: 'raw', raw: JSON.stringify({",
    "        vehicleId: pm.collectionVariables.get('vehicleId'),",
    "        serviceId: pm.collectionVariables.get('serviceId'),",
    "        startTime: pm.collectionVariables.get('slotDate') + 'T11:00:00',",
    "        endTime:   pm.collectionVariables.get('slotDate') + 'T12:00:00' }) }",
    "}, (err, res) => {",
    "    const b = res && res.json ? res.json() : null;",
    "    if (b && b.id) {",
    "        pm.collectionVariables.set('payOkBookingId', b.id);",
    "        console.log('TC-25: created booking ' + b.id + ' to pay');",
    "    } else {",
    "        console.log('TC-25: could not create a booking — reset collection variables and retry');",
    "    }",
    "});",
])
set_url(tc25["request"], "/api/payments/{{payOkBookingId}}")
tc25["request"]["description"] = (
    "Expected 200 with \"status\": \"SUCCESS\" and a SIM-<reference> transaction number.\n\n"
    "Self-contained — the pre-request script creates a fresh PENDING booking (11:00-12:00) on the "
    "first run, because a booking can only be paid once."
)

# ---------------------------------------------------------------------------
# TC-26 — FAILED on its own unpaid booking
# ---------------------------------------------------------------------------
tc26 = find(coll["item"], "TC-26")
set_prereq(tc26, [
    "// TC-26 needs its OWN unpaid booking. Reusing the booking TC-25 already paid",
    "// returns 400 'Booking is already paid and confirmed' instead of FAILED.",
    "const base = pm.collectionVariables.get('baseUrl');",
    "if (pm.collectionVariables.get('failBookingId')) return;",
    "pm.sendRequest({",
    "    url: base + '/api/bookings',",
    "    method: 'POST',",
    "    header: { 'Content-Type': 'application/json',",
    "              'Authorization': 'Bearer ' + pm.collectionVariables.get('userToken') },",
    "    body: { mode: 'raw', raw: JSON.stringify({",
    "        vehicleId: pm.collectionVariables.get('vehicleId'),",
    "        serviceId: pm.collectionVariables.get('serviceId'),",
    "        startTime: pm.collectionVariables.get('slotDate') + 'T12:00:00',",
    "        endTime:   pm.collectionVariables.get('slotDate') + 'T13:00:00' }) }",
    "}, (err, res) => {",
    "    const b = res && res.json ? res.json() : null;",
    "    if (b && b.id) {",
    "        pm.collectionVariables.set('failBookingId', b.id);",
    "        console.log('TC-26: created booking ' + b.id + ' for the forced failure');",
    "    } else {",
    "        console.log('TC-26: could not create a booking — reset collection variables and retry');",
    "    }",
    "});",
])
set_url(tc26["request"], "/api/payments/{{failBookingId}}")
tc26["request"]["description"] = (
    "Expected 200 with \"status\": \"FAILED\" — the simulated gateway's failure path, which "
    "releases the slot.\n\nSelf-contained — the pre-request script creates a dedicated unpaid "
    "booking (12:00-13:00) so it never reuses the booking TC-25 already paid."
)

io.open(PATH, "w", encoding="utf-8").write(json.dumps(coll, indent=2) + "\n")
print("collection patched")
