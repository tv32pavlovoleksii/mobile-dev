"""
Навчальний REST-бекенд для ПЗ 9-12 (варіант 15 "Система моніторингу енергоспоживання").

Застосунок-клієнт (Android) звертається сюди через Retrofit з емулятора за адресою
http://10.0.2.2:5156/ (10.0.2.2 в емуляторі Android завжди вказує на localhost хост-машини).
Дані зберігаються лише в пам'яті процесу - це навчальний сервер, не продакшн.

Запуск:  python server/app.py
"""
import secrets
import time
from flask import Flask, jsonify, request

app = Flask(__name__)

# ---------------------------------------------------------------- дані
_next_id = 1
readings: list[dict] = []
tariffs = [
    {"id": 1, "name": "Одна зона", "dayRateUah": 4.32, "nightRateUah": 4.32},
    {"id": 2, "name": "Дві зони", "dayRateUah": 4.32, "nightRateUah": 2.16},
]
USERS = {
    "demo": {"password": "Energy2026!", "role": "USER"},
    "admin": {"password": "Admin2026!", "role": "ADMIN"},
}
TOKEN_TTL_SEC = 300
tokens: dict[str, dict] = {}  # token -> {username, role, expiresAt}
_flaky_attempts = {"count": 0}


def _seed():
    global _next_id
    base = [(1820, 229.0, 7.9), (1960, 230.0, 8.5), (1705, 228.0, 7.5), (2100, 231.0, 9.1)]
    for powerW, voltageV, currentA in base:
        readings.append({
            "id": _next_id, "timestamp": int(time.time()) - (len(base) - _next_id) * 300,
            "powerW": powerW, "voltageV": voltageV, "currentA": currentA,
        })
        _next_id += 1


_seed()


def _auth_user():
    header = request.headers.get("Authorization", "")
    if not header.startswith("Bearer "):
        return None, ("Відсутній токен авторизації", 401)
    token = header.removeprefix("Bearer ").strip()
    entry = tokens.get(token)
    if entry is None:
        return None, ("Недійсний токен", 401)
    if entry["expiresAt"] < time.time():
        del tokens[token]
        return None, ("Термін дії токена сплив, увійдіть повторно", 401)
    return entry, None


# ---------------------------------------------------------------- ПЗ 9/10: прилади/читання (Repository+DataSource, REST)
@app.get("/readings")
def list_readings():
    limit = request.args.get("limit", default=20, type=int)
    return jsonify(readings[-limit:][::-1])


@app.post("/readings")
def add_reading():
    global _next_id
    body = request.get_json(force=True)
    item = {
        "id": _next_id,
        "timestamp": int(time.time()),
        "powerW": body["powerW"],
        "voltageV": body["voltageV"],
        "currentA": body["currentA"],
    }
    readings.append(item)
    _next_id += 1
    return jsonify(item), 201


@app.get("/readings/stats")
def readings_stats():
    if not readings:
        return jsonify({"count": 0, "avgPowerW": 0, "maxPowerW": 0, "minPowerW": 0})
    powers = [r["powerW"] for r in readings]
    return jsonify({
        "count": len(powers),
        "avgPowerW": round(sum(powers) / len(powers), 1),
        "maxPowerW": max(powers),
        "minPowerW": min(powers),
    })


@app.get("/readings/<int:reading_id>")
def get_reading(reading_id):
    for r in readings:
        if r["id"] == reading_id:
            return jsonify(r)
    return jsonify({"message": "Показник не знайдено"}), 404


@app.delete("/readings/<int:reading_id>")
def delete_reading(reading_id):
    global readings
    before = len(readings)
    readings = [r for r in readings if r["id"] != reading_id]
    if len(readings) == before:
        return jsonify({"message": "Показник не знайдено"}), 404
    return "", 204


@app.get("/tariffs")
def list_tariffs():
    return jsonify(tariffs)


# ---------------------------------------------------------------- ПЗ 11: мокований JWT-подібний токен + ролі
@app.post("/auth/login")
def login():
    body = request.get_json(force=True)
    username = str(body.get("username", "")).strip()
    password = str(body.get("password", ""))
    user = USERS.get(username)
    if user is None or user["password"] != password:
        return jsonify({"message": "Невірний логін або пароль"}), 401
    token = secrets.token_urlsafe(24)
    tokens[token] = {"username": username, "role": user["role"], "expiresAt": time.time() + TOKEN_TTL_SEC}
    return jsonify({"token": token, "role": user["role"], "expiresInSec": TOKEN_TTL_SEC})


@app.get("/protected")
def protected():
    user, error = _auth_user()
    if error:
        return jsonify({"message": error[0]}), error[1]
    return jsonify({"message": f"Привіт, {user['username']}! Ваша роль: {user['role']}."})


@app.get("/admin/readings")
def admin_readings():
    user, error = _auth_user()
    if error:
        return jsonify({"message": error[0]}), error[1]
    if user["role"] != "ADMIN":
        return jsonify({"message": "Доступ лише для адміністратора"}), 403
    return jsonify([dict(r, meterId=f"MTR-{r['id']:04d}") for r in readings])


# ---------------------------------------------------------------- ПЗ 12: симуляція помилок і тимчасових збоїв
@app.get("/readings/error/<int:code>")
def simulate_error(code):
    messages = {400: "Некоректний запит", 404: "Ресурс не знайдено", 500: "Внутрішня помилка сервера", 503: "Сервіс тимчасово недоступний"}
    return jsonify({"message": messages.get(code, "Помилка сервера")}), code


@app.get("/readings/flaky")
def flaky():
    """Перші два виклики повертають 503 (тимчасова помилка), третій — успіх. Демонструє retry-логіку клієнта."""
    _flaky_attempts["count"] += 1
    if _flaky_attempts["count"] % 3 != 0:
        return jsonify({"message": "Сервіс тимчасово недоступний, спробуйте ще раз"}), 503
    return jsonify({"message": "Дані успішно отримано після повторних спроб"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5156)
