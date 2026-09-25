#!/usr/bin/env node
/**
 * Validate and run the Postman collection outside Postman.
 *
 * Why this exists: a bug in the collection's own scripts cost two rounds of
 * debugging. `pm.collectionVariables.set('x', ...);_start` is *syntactically*
 * valid JavaScript, so `node --check` passes it — but it throws a
 * ReferenceError at runtime, so Postman aborts the request ("No response") and
 * the variable it was meant to set stays empty. Only *executing* the scripts
 * catches that class of mistake.
 *
 * This harness therefore:
 *   1. stubs the `pm` API (collectionVariables, response, test, expect,
 *      sendRequest) closely enough to run the real scripts,
 *   2. runs every request in collection order against a live backend,
 *   3. reports each assertion, and
 *   4. exits non-zero if anything fails, so it can gate a commit.
 *
 * Usage:
 *   node docs/validate_collection.js [baseUrl]
 *   node docs/validate_collection.js http://localhost:8081
 */
const fs = require("fs");
const path = require("path");

const BASE = process.argv[2] || "http://localhost:8080";
const COLLECTION = path.join(__dirname, "EV-Finder.postman_collection.json");

const coll = JSON.parse(fs.readFileSync(COLLECTION, "utf8"));
const variables = new Map();
(coll.variable || []).forEach((v) => variables.set(v.key, v.value));
variables.set("baseUrl", BASE);

// --------------------------------------------------------------------------
// Minimal `pm` test/expect implementation (the subset the collection uses)
// --------------------------------------------------------------------------
function expect(actual) {
  const fail = (msg) => {
    throw new Error(msg);
  };
  const api = {
    eql: (e) => {
      if (JSON.stringify(actual) !== JSON.stringify(e)) {
        fail(`expected ${JSON.stringify(actual)} to deep equal ${JSON.stringify(e)}`);
      }
    },
    equal: (e) => {
      if (actual !== e) fail(`expected ${JSON.stringify(actual)} to equal ${JSON.stringify(e)}`);
    },
    above: (n) => {
      if (!(actual > n)) fail(`expected ${actual} to be above ${n}`);
    },
    below: (n) => {
      if (!(actual < n)) fail(`expected ${actual} to be below ${n}`);
    },
    include: (v) => {
      const ok = Array.isArray(actual) ? actual.includes(v) : String(actual).includes(v);
      if (!ok) fail(`expected ${JSON.stringify(actual)} to include ${JSON.stringify(v)}`);
    },
    contain: (v) => {
      if (!String(actual).includes(v)) fail(`expected ${JSON.stringify(actual)} to contain "${v}"`);
    },
  };
  Object.defineProperty(api, "true", {
    get: () => {
      if (actual !== true) fail(`expected ${JSON.stringify(actual)} to be true`);
    },
  });
  Object.defineProperty(api, "false", {
    get: () => {
      if (actual !== false) fail(`expected ${JSON.stringify(actual)} to be false`);
    },
  });
  // `.to`, `.be`, `.have` are pass-through noise words
  api.to = api;
  api.be = api;
  api.have = api;
  // `.a` / `.an` are *called*: expect(x).to.be.a('string'). Keep the chain
  // methods available on the function too, so .a.above(...) would still work.
  const aFn = (type) => {
    const t = typeof actual;
    const ok =
      (type === "string" && t === "string") ||
      (type === "number" && t === "number") ||
      (type === "boolean" && t === "boolean") ||
      (type === "object" && actual !== null && t === "object") ||
      (type === "array" && Array.isArray(actual));
    if (!ok) fail(`expected ${JSON.stringify(actual)} to be a ${type}`);
  };
  Object.assign(aFn, api);
  api.a = aFn;
  api.an = aFn;
  return api;
}

function makeResponse(status, statusText, bodyText) {
  let parsed;
  const json = () => {
    if (parsed === undefined) {
      try {
        parsed = JSON.parse(bodyText);
      } catch (e) {
        throw new Error(`response body is not JSON: ${bodyText.slice(0, 120)}`);
      }
    }
    return parsed;
  };
  return {
    code: status,
    status: statusText,
    text: () => bodyText,
    json,
    to: {
      have: {
        status: (expected) => {
          if (status !== expected) {
            throw new Error(`expected response to have status code ${expected} but got ${status}`);
          }
        },
      },
    },
  };
}

async function httpSend({ url, method = "GET", header = {}, body }) {
  const headers = {};
  if (Array.isArray(header)) header.forEach((h) => (headers[h.key] = resolve(h.value)));
  else Object.entries(header || {}).forEach(([k, v]) => (headers[k] = resolve(v)));

  let payload;
  if (body && typeof body === "object" && body.mode === "raw") payload = resolve(body.raw);
  else if (typeof body === "string") payload = resolve(body);

  const res = await fetch(resolve(url), { method, headers, body: payload });
  const text = await res.text();
  return { status: res.status, statusText: res.statusText, text };
}

/** Substitute {{variables}} the way Postman does. */
function resolve(input) {
  if (typeof input !== "string") return input;
  return input.replace(/\{\{([^}]+)\}\}/g, (m, key) => {
    const v = variables.get(key);
    return v === undefined || v === "" ? m : v;
  });
}

// --------------------------------------------------------------------------
// Runner
// --------------------------------------------------------------------------
const results = [];
let currentName = "";

function makePm() {
  const pending = [];
  const logs = [];
  const assertFailures = [];

  const pm = {
    collectionVariables: {
      get: (k) => variables.get(k),
      set: (k, v) => variables.set(k, v),
      unset: (k) => variables.delete(k),
    },
    environment: { get: (k) => variables.get(k), set: (k, v) => variables.set(k, v) },
    variables: { get: (k) => variables.get(k), set: (k, v) => variables.set(k, v) },
    response: null,
    expect,
    test: (name, fn) => {
      try {
        fn();
        results.push({ request: currentName, test: name, ok: true });
      } catch (e) {
        assertFailures.push(`${name} — ${e.message}`);
        results.push({ request: currentName, test: name, ok: false, error: e.message });
        process.exitCode = 1;
      }
    },
    sendRequest: (opts, cb) => {
      const p = httpSend(opts)
        .then((r) => cb && cb(null, makeResponse(r.status, r.statusText, r.text)))
        .catch((e) => cb && cb(e, null));
      pending.push(p);
      return p;
    },
  };
  Object.defineProperty(pm, "_pending", { value: pending });
  pm.console = {
    log: (...a) => logs.push("      " + a.join(" ")),
    error: (...a) => logs.push("      ! " + a.join(" ")),
    warn: (...a) => logs.push("      ! " + a.join(" ")),
  };
  return { pm, logs, assertFailures };
}

async function runScript(lines, pm) {
  const src = lines.join("\n");
  const fn = new Function("pm", "console", `return (async () => {\n${src}\n})();`);
  await fn(pm, pm.console);
}

async function runRequest(item) {
  currentName = item.name;
  const req = item.request;
  const { pm, logs, assertFailures } = makePm();

  const prereq = (item.event || []).find((e) => e.listen === "prerequest");
  if (prereq) {
    await runScript(prereq.script.exec, pm);
    await Promise.all(pm._pending);
  }

  const url = resolve(req.url.raw);
  const method = req.method;
  let outcome;
  try {
    const r = await httpSend({
      url,
      method,
      header: req.header,
      body: req.body,
    });
    outcome = { status: r.status, statusText: r.statusText };
    pm.response = makeResponse(r.status, r.statusText, r.text);
  } catch (e) {
    outcome = { status: "NO RESPONSE", statusText: e.message };
    console.log(`  ${item.name.padEnd(46)} -> NO RESPONSE  (${e.message})`);
    console.log(`      url: ${url}`);
    results.push({ request: item.name, test: "request completed", ok: false, error: e.message });
    process.exitCode = 1;
    return;
  }

  const test = (item.event || []).find((e) => e.listen === "test");
  if (test) {
    await runScript(test.script.exec, pm);
    await Promise.all(pm._pending);
  }

  const failed = assertFailures.length;
  const marks = results.filter((r) => r.request === currentName);
  const passed = marks.filter((m) => m.ok).length;
  console.log(
    `  ${item.name.padEnd(46)} HTTP ${String(outcome.status).padEnd(5)} ` +
      `${failed === 0 ? "PASS" : "FAIL"}`
  );
  if (failed) assertFailures.forEach((f) => console.log(`      x ${f}`));
  if (logs.length && process.env.SHOW_LOGS) logs.forEach((l) => console.log(l));
}

(async () => {
  console.log(`Validating ${path.basename(COLLECTION)} against ${BASE}\n`);
  for (const folder of coll.item) {
    console.log(folder.name);
    for (const item of folder.item || []) {
      await runRequest(item);
    }
    console.log("");
  }

  const total = results.length;
  const failed = results.filter((r) => !r.ok).length;
  console.log("=".repeat(64));
  console.log(`assertions: ${total}   passed: ${total - failed}   failed: ${failed}`);
  if (failed) {
    console.log("\nFailed assertions:");
    results.filter((r) => !r.ok).forEach((r) => console.log(`  ${r.request}: ${r.test} — ${r.error}`));
  }
  console.log(failed === 0 ? "\nCOLLECTION OK" : "\nCOLLECTION HAS FAILURES");
})();
