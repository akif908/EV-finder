"""Fix the stale-vehicle (409) failure in the Postman collection.

Root cause: TC-06 created a vehicle with a FIXED registration number
("TST-AC-001"), and the column is globally unique, so a second run hit
409 Conflict. The collection also never deleted the vehicle, so the row
survived every run.

Three changes, addressing the cause and both suggested mitigations:

  1. TC-06 generates a UNIQUE registration number per run, so a collision is
     impossible by construction (`testRegNo` = TST-<run stamp>).
  2. TC-06 gains a pre-request script that deletes leftover TST-* vehicles,
     so the list cannot grow and any historical duplicate is cleared.
  3. TC-07 (update 200) and TC-09 (delete 204) are added so the collection
     exercises the full CRUD cycle and removes what it created, plus a
     "9. Teardown" folder that sweeps any remaining test vehicles — which
     also cleans up after a run that was aborted part-way.

Run:  python docs/patch_postman_collection.py
"""
import collections
import io
import json

PATH = "docs/EV-Finder.postman_collection.json"
OD = collections.OrderedDict

coll = json.load(io.open(PATH, encoding="utf-8"), object_pairs_hook=OD)

# ---- variables ------------------------------------------------------------
have = {v["key"] for v in coll["variable"]}
for key in ("testRegNo", "createdVehicleId"):
    if key not in have:
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


def find_folder(items, prefix):
    for item in items:
        if "item" in item and item.get("name", "").startswith(prefix):
            return item
    return None


def set_prereq(request, lines):
    request.setdefault("event", [])
    request["event"] = [e for e in request["event"] if e["listen"] != "prerequest"]
    request["event"].insert(0, OD([
        ("listen", "prerequest"),
        ("script", OD([("type", "text/javascript"), ("exec", lines)])),
    ]))


def auth_header(token_var):
    return [
        OD([("key", "Authorization"), ("value", "Bearer {{%s}}" % token_var)]),
        OD([("key", "Content-Type"), ("value", "application/json")]),
    ]


def json_request(name, method, path_suffix, token_var, body=None, tests=None,
                 description=None, prereq=None):
    """Build a Postman request item."""
    item = OD([
        ("name", name),
        ("request", OD([
            ("method", method),
            ("header", auth_header(token_var)),
            ("url", OD([
                ("raw", "{{baseUrl}}" + path_suffix),
                ("host", ["{{baseUrl}}"]),
                ("path", [s for s in path_suffix.strip("/").split("/")]),
            ])),
        ])),
    ])
    if body is not None:
        item["request"]["body"] = OD([("mode", "raw"), ("raw", body)])
    if description:
        item["request"]["description"] = description
    events = []
    if prereq:
        events.append(OD([("listen", "prerequest"),
                          ("script", OD([("type", "text/javascript"), ("exec", prereq)]))]))
    if tests:
        events.append(OD([("listen", "test"),
                          ("script", OD([("type", "text/javascript"), ("exec", tests)]))]))
    if events:
        item["event"] = events
    return item


# ---------------------------------------------------------------------------
# 1 + 2. TC-06 — unique registration number per run, plus hygiene sweep
# ---------------------------------------------------------------------------
tc06 = find(coll["item"], "TC-06")
set_prereq(tc06, [
    "// TC-06 used to fail with 409 on a second run: the registration number was",
    "// fixed ('TST-AC-001') and that column is GLOBALLY unique, so the row created",
    "// by the previous run still occupied it.",
    "//",
    "// Two safeguards:",
    "//   1. a fresh registration number for every run, so a collision is",
    "//      impossible even if cleanup never happens;",
    "//   2. best-effort deletion of leftover TST-* vehicles so the garage does",
    "//      not accumulate test data.",
    "const base = pm.collectionVariables.get('baseUrl');",
    "const auth = { 'Authorization': 'Bearer ' + pm.collectionVariables.get('userToken') };",
    "",
    "pm.collectionVariables.set('testRegNo', 'TST-' + Date.now().toString().slice(-8));",
    "",
    "pm.sendRequest({ url: base + '/api/vehicles/my', method: 'GET', header: auth }, (err, res) => {",
    "    const list = res && res.json ? res.json() : [];",
    "    const stale = (Array.isArray(list) ? list : [])",
    "        .filter(v => (v.registrationNo || '').toUpperCase().startsWith('TST-'));",
    "    console.log('TC-06: registration for this run = ' + pm.collectionVariables.get('testRegNo'));",
    "    console.log('TC-06: deleting ' + stale.length + ' leftover test vehicle(s)');",
    "    stale.forEach(v => {",
    "        pm.sendRequest({ url: base + '/api/vehicles/' + v.id, method: 'DELETE', header: auth },",
    "            () => {});",
    "    });",
    "});",
])
tc06["request"]["body"]["raw"] = (
    "{\n"
    '  "manufacturer": "Test",\n'
    '  "model": "Acceptance Car",\n'
    '  "vehicleType": "ELECTRIC_CAR",\n'
    '  "connectorType": "CCS2",\n'
    '  "batteryCapacityKwh": 60,\n'
    '  "registrationNo": "{{testRegNo}}"\n'
    "}"
)
# keep the existing 201 assertion, add id capture
test_ev = [e for e in tc06.get("event", []) if e["listen"] == "test"][0]
test_ev["script"]["exec"] = [
    "pm.test('TC-06 status is 201', () => pm.response.to.have.status(201));",
    "const v = pm.response.json();",
    "pm.test('registration number is the unique test value',",
    "    () => pm.expect(v.registrationNo).to.eql(pm.collectionVariables.get('testRegNo')));",
    "pm.collectionVariables.set('createdVehicleId', v.id);",
]
tc06["request"]["description"] = (
    "Expected 201 Created.\n\n"
    "The registration number is generated fresh for each run and any leftover TST-* vehicle is "
    "removed first, so repeated runs no longer collide (that column is globally unique)."
)

# ---------------------------------------------------------------------------
# vehicles folder: insert TC-07 after TC-06, and TC-09 after TC-08
# ---------------------------------------------------------------------------
vehicles = find_folder(coll["item"], "4. Vehicles")

tc07 = json_request(
    "TC-07 PUT /api/vehicles/{id} (200)",
    "PUT", "/api/vehicles/{{createdVehicleId}}", "userToken",
    body=("{\n"
          '  "manufacturer": "Test",\n'
          '  "model": "Acceptance Car v2",\n'
          '  "vehicleType": "ELECTRIC_CAR",\n'
          '  "connectorType": "CCS2",\n'
          '  "batteryCapacityKwh": 64,\n'
          '  "registrationNo": "{{testRegNo}}"\n'
          "}"),
    tests=[
        "pm.test('TC-07 status is 200', () => pm.response.to.have.status(200));",
        "pm.test('model was updated', () => pm.expect(pm.response.json().model).to.eql('Acceptance Car v2'));",
    ],
    description=("Expected 200 OK with the updated model.\n\n"
                 "Keeps the same registration number, which the service explicitly allows for an "
                 "update of the same vehicle. Requires TC-06 to have run (it saves createdVehicleId)."),
)

tc09 = json_request(
    "TC-09 DELETE /api/vehicles/{id} (204)",
    "DELETE", "/api/vehicles/{{createdVehicleId}}", "userToken",
    tests=[
        "pm.test('TC-09 status is 204', () => pm.response.to.have.status(204));",
        "pm.collectionVariables.set('createdVehicleId', '');",
    ],
    description=("Expected 204 No Content with an empty body.\n\n"
                 "Deletes the vehicle TC-06 created, so a normal run leaves the garage as it found it."),
)

# rebuild the folder in the intended CRUD order
order = {"TC-06": 0, "TC-07": 1, "TC-08": 2, "TC-09": 3, "TC-10": 4}
items = [i for i in vehicles["item"] if not i.get("name", "").startswith(("TC-07", "TC-09"))]
items.append(tc07)
items.append(tc09)
items.sort(key=lambda i: order.get(i.get("name", "")[:5], 99))
vehicles["item"] = items

# ---------------------------------------------------------------------------
# Setup: pick a REAL vehicle, so bookings never attach to a test vehicle
# ---------------------------------------------------------------------------
setup_vehicle = find(coll["item"], "Save vehicle id")
setup_vehicle["event"] = [e for e in setup_vehicle.get("event", []) if e["listen"] != "test"]
setup_vehicle["event"].append(OD([
    ("listen", "test"),
    ("script", OD([("type", "text/javascript"), ("exec", [
        "// Pick a vehicle that is NOT test data. A booking attached to a TST-*",
        "// vehicle makes that vehicle undeletable (the API answers 409 for a",
        "// vehicle with booking history), which is what left stale rows behind.",
        "const list = pm.response.json();",
        "const all = Array.isArray(list) ? list : [];",
        "pm.test('at least one vehicle exists', () => pm.expect(all.length).to.be.above(0));",
        "const real = all.filter(v => !(v.registrationNo || '').toUpperCase().startsWith('TST-'));",
        "const pick = real[0] || all[0];",
        "pm.collectionVariables.set('vehicleId', pick ? pick.id : '');",
        "console.log('setup: bookings will use vehicle ' + (pick ? pick.registrationNo : 'none'));",
    ])])),
]))
setup_vehicle["request"]["description"] = (
    "Saves the vehicle id used by the booking and payment cases.\n\n"
    "Deliberately prefers a non-test vehicle: bookings attached to a TST-* vehicle would make it "
    "undeletable, since the API refuses to delete a vehicle that has booking history (409)."
)

# ---------------------------------------------------------------------------
# 3. teardown folder
# ---------------------------------------------------------------------------
teardown = OD([
    ("name", "9. Teardown (optional cleanup)"),
    ("description", "Removes test data this collection creates. Run it last. Safe to run any "
                    "number of times."),
    ("item", [json_request(
        "Cleanup: delete all test vehicles (TST-*)",
        "GET", "/api/vehicles/my", "userToken",
        tests=[
            "// Sweeps every leftover TST-* vehicle, including those from a run that was",
            "// aborted before TC-09 could delete the vehicle it created.",
            "const base = pm.collectionVariables.get('baseUrl');",
            "const auth = { 'Authorization': 'Bearer ' + pm.collectionVariables.get('userToken') };",
            "const list = pm.response.json();",
            "const stale = (Array.isArray(list) ? list : [])",
            "    .filter(v => (v.registrationNo || '').toUpperCase().startsWith('TST-'));",
            "pm.test('TC-teardown status is 200', () => pm.response.to.have.status(200));",
            "pm.test('teardown sweep ran', () => {",
            "    console.log('teardown: ' + stale.length + ' test vehicle(s) to remove');",
            "    stale.forEach(v => pm.sendRequest({",
            "        url: base + '/api/vehicles/' + v.id, method: 'DELETE', header: auth",
            "    }, (err, res) => {",
            "        if (res && res.code === 204) {",
            "            console.log('  removed ' + v.registrationNo);",
            "        } else {",
            "            console.log('  kept ' + v.registrationNo + ' (HTTP ' + (res ? res.code : '?') +",
            "                        ') — a vehicle with booking history cannot be deleted');",
            "        }",
            "    }));",
            "    pm.expect(true).to.be.true;",
            "});",
        ],
        description=("Lists the user's vehicles and deletes every one whose registration number "
                     "starts with TST-. Run this last to leave the demo database clean; use it "
                     "after an interrupted run to clear a stale vehicle that would otherwise cause "
                     "TC-06 to answer 409.\n\n"
                     "A vehicle that already has bookings is reported as kept rather than removed: "
                     "the API refuses to delete a vehicle with booking history (409) so that "
                     "booking records are not orphaned. That is intended behaviour, not a failure."),
    )]),
])

coll["item"] = [i for i in coll["item"] if not i.get("name", "").startswith("9. Teardown")]
coll["item"].append(teardown)

io.open(PATH, "w", encoding="utf-8").write(json.dumps(coll, indent=2) + "\n")
print("collection patched: unique TC-06 data, TC-07/TC-09 added, teardown folder appended")
