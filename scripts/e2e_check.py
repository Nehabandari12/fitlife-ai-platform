"""End-to-end check of a running FitLife stack (README, "Quickstart").

    pip install httpx
    python scripts/e2e_check.py

Registers two throwaway users through the gateway, signs each in with the same authorization-code +
PKCE flow the React app uses, logs an activity as one of them, waits for the Kafka -> AI service
recommendation, then checks that the other user can't read, change or delete any of it: through the
gateway, with a forged X-User-ID header, and directly against each service's port. Exits non-zero if
any check fails. The users it creates stay in Keycloak and the databases.
"""
import base64, hashlib, html, json, re, secrets, sys, time, uuid
from urllib.parse import parse_qs, urlparse

import httpx

# 127.0.0.1 rather than localhost: Python's cookie jar mishandles cookies for "localhost".
KC = "http://127.0.0.1:8181/realms/fitness-app/protocol/openid-connect"
GW = "http://localhost:8080/api"
REDIRECT = "http://localhost:5173"
results = []


def check(name, ok, detail=""):
    results.append((name, ok))
    print(f"{'PASS' if ok else 'FAIL'}  {name}" + (f"  ({detail})" if detail else ""), flush=True)


def sign_up(email, password):
    r = httpx.post(f"{GW}/auth/register", json={"email": email, "password": password, "firstName": "Test", "lastName": email.split("@")[0]}, timeout=30)
    return r.status_code


def sign_in(email, password):
    """The browser flow: authorization code with PKCE, as the React app does it."""
    verifier = secrets.token_urlsafe(48)
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).rstrip(b"=").decode()
    with httpx.Client(timeout=30, follow_redirects=False) as c:
        page = c.get(f"{KC}/auth", params={"client_id": "oauth2-pkce-client", "redirect_uri": REDIRECT, "response_type": "code",
                                         "scope": "openid profile email", "code_challenge": challenge,
                                         "code_challenge_method": "S256", "state": "e2e"})
        action = html.unescape(re.search(r'action="([^"]+)"', page.text).group(1))
        # Keycloak marks its session cookies Secure; browsers still send them to localhost over HTTP,
        # Python's cookie jar doesn't, so carry them by hand.
        cookies = "; ".join(h.split(";", 1)[0] for h in page.headers.get_list("set-cookie"))
        resp = c.post(action, data={"username": email, "password": password, "credentialId": ""}, headers={"Cookie": cookies})
        location = resp.headers.get("location", "")
        code = parse_qs(urlparse(location).query).get("code", [None])[0]
        if not code:
            raise SystemExit(f"sign-in failed: HTTP {resp.status_code} {location[:120]}")
        tok = c.post(f"{KC}/token", data={"grant_type": "authorization_code", "code": code, "redirect_uri": REDIRECT,
                                          "client_id": "oauth2-pkce-client", "code_verifier": verifier}).json()
        return tok["access_token"]


def sub_of(token):
    payload = token.split(".")[1]
    return json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))["sub"]


def api(method, path, token=None, base=GW, **kw):
    headers = kw.pop("headers", {})
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return httpx.request(method, base + path, headers=headers, timeout=60, **kw)


run = uuid.uuid4().hex[:6]
alice, bob, pw = f"alice-{run}@example.com", f"bob-{run}@example.com", "fitlife-e2e-" + run
check("sign-up through the gateway (alice)", sign_up(alice, pw) == 201)
check("sign-up through the gateway (bob)", sign_up(bob, pw) == 201)
ta, tb = sign_in(alice, pw), sign_in(bob, pw)
check("PKCE sign-in returns access tokens", bool(ta and tb))
a_id, b_id = sub_of(ta), sub_of(tb)

r = api("POST", "/activities", ta, json={"userId": b_id, "type": "RUNNING", "duration": 30, "caloriesBurned": 320,
                                          "startTime": "2026-10-06T07:30:00", "additionalMetrics": {"distanceKm": 5.2}})
check("alice logs an activity (body userId ignored)", r.status_code == 200 and r.json()["userId"] == a_id, f"HTTP {r.status_code}")
act = r.json()["id"]

r = api("GET", "/activities", ta)
check("alice's history lists it", r.status_code == 200 and [x["id"] for x in r.json()] == [act])

rec = None
for _ in range(40):
    r = api("GET", f"/recommendations/activity/{act}", ta)
    if r.status_code == 200:
        rec = r.json()
        break
    time.sleep(1.5)
check("Kafka -> AI service produced a recommendation", rec is not None)
if rec:
    print("      status:", rec.get("status"), "reason:", rec.get("fallbackReason"), flush=True)

check("bob gets 404 for alice's activity", api("GET", f"/activities/{act}", tb).status_code == 404)
check("bob with a forged X-User-ID still gets 404", api("GET", f"/activities/{act}", tb, headers={"X-User-ID": a_id}).status_code == 404)
check("bob gets 404 for alice's recommendation", api("GET", f"/recommendations/activity/{act}", tb, headers={"X-User-ID": a_id}).status_code == 404)
check("bob cannot request alice's summary", api("GET", f"/recommendations/user/{a_id}", tb).status_code == 403)
check("bob cannot read alice's profile", api("GET", f"/users/{a_id}", tb).status_code == 403)
check("bob cannot update alice's activity", api("PUT", f"/activities/{act}", tb, json={"type": "YOGA", "duration": 1, "caloriesBurned": 1}).status_code == 404)
check("bob cannot delete alice's activity", api("DELETE", f"/activities/{act}", tb).status_code == 404)
check("bob's history is empty", api("GET", "/activities", tb).json() == [])
r = api("GET", f"/users/{a_id}", ta)
check("alice's own profile has no password field", r.status_code == 200 and "password" not in r.json(), f"HTTP {r.status_code}")

for port, path in ((8082, f"/api/activities/{act}"), (8083, f"/api/recommendations/activity/{act}"), (8081, f"/api/users/{a_id}")):
    no_token = api("GET", path, base=f"http://localhost:{port}", headers={"X-User-ID": a_id}).status_code
    as_bob = api("GET", path, tb, base=f"http://localhost:{port}", headers={"X-User-ID": a_id}).status_code
    check(f"direct call to :{port} without a token is refused, with bob's token is denied", no_token == 401 and as_bob in (403, 404), f"{no_token}/{as_bob}")

check("no token at the gateway is 401", api("GET", "/activities", headers={"X-User-ID": a_id}).status_code == 401)

r = api("DELETE", f"/activities/{act}", ta)
check("alice deletes her activity", r.status_code == 204)
gone = False
for _ in range(20):
    if api("GET", f"/recommendations/activity/{act}", ta).status_code == 404:
        gone = True
        break
    time.sleep(1)
check("the delete event removed the recommendation", gone)

passed = sum(ok for _, ok in results)
print(f"\n{passed}/{len(results)} checks passed", flush=True)
sys.exit(0 if passed == len(results) else 1)
