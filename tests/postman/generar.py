"""Genera las colecciones y entornos de Newman del Taller 4.

Uso: python tests/postman/generar.py   (escribe junto a este archivo; se puede pasar otra carpeta como argumento)
"""
import json
import sys
from pathlib import Path

OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent
SCHEMA = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
NS = "http://recursos.javeriana.edu.co/soap"


def item(name, method, url, tests, body=None, headers=None, prerequest=None):
    req = {"method": method, "header": headers or [], "url": url}
    if body is not None:
        req["body"] = {"mode": "raw", "raw": body}
    events = [{"listen": "test", "script": {"type": "text/javascript", "exec": tests.strip().splitlines()}}]
    if prerequest:
        events.insert(0, {"listen": "prerequest",
                          "script": {"type": "text/javascript", "exec": prerequest.strip().splitlines()}})
    return {"name": name, "event": events, "request": req}


def collection(name, description, items):
    return {"info": {"name": name, "description": description, "schema": SCHEMA}, "item": items}


def env(name, values):
    return {"name": name, "values": [{"key": k, "value": v, "enabled": True} for k, v in values.items()]}


# ---------------------------------------------------------------- REST
JSON_H = [{"key": "Content-Type", "value": "application/json"}, {"key": "apikey", "value": "{{apiKey}}"}]
KEY_H = [{"key": "apikey", "value": "{{apiKey}}"}]


def rest_error(status, code, message=None):
    msg = f"""
pm.test("mensaje de negocio", () => pm.expect(pm.response.json().message).to.eql("{message}"));""" if message else ""
    return f"""
pm.test("HTTP {status}", () => pm.response.to.have.status({status}));
pm.test("código {code}", () => pm.expect(pm.response.json().code).to.eql("{code}"));
pm.test("cuerpo de error del T2", () => pm.expect(pm.response.json()).to.have.keys("code", "message", "timestamp"));{msg}
"""


def body(desc, rtype, qty, prio):
    return json.dumps({"disasterDescription": desc, "resourceType": rtype, "quantity": qty, "priority": prio},
                      ensure_ascii=False, indent=2)


rest_items = [
    item("RF-1 Registrar solicitud válida", "POST", "{{baseUrl}}/requests", """
pm.test("HTTP 201", () => pm.response.to.have.status(201));
const r = pm.response.json();
pm.test("nace PENDIENTE", () => pm.expect(r.status).to.eql("PENDIENTE"));
pm.test("tiene id", () => pm.expect(r.id).to.be.a("number"));
pm.test("devuelve los datos enviados", () => {
  pm.expect(r.resourceType).to.eql("Agua potable");
  pm.expect(r.quantity).to.eql(120);
  pm.expect(r.priority).to.eql("ALTA");
});
pm.test("cabecera Location", () => pm.expect(pm.response.headers.get("Location")).to.include("/requests/" + r.id));
pm.collectionVariables.set("id", r.id);
""", body("Inundación en Mocoa {{$timestamp}}", "Agua potable", 120, "ALTA"), JSON_H),

    item("RF-1 Regla: cantidad 0", "POST", "{{baseUrl}}/requests",
         rest_error(400, "VALIDATION_ERROR", "La cantidad debe ser mayor que cero"),
         body("Incendio forestal", "Carpas", 0, "MEDIA"), JSON_H),
    item("RF-1 Regla: descripción vacía", "POST", "{{baseUrl}}/requests",
         rest_error(400, "VALIDATION_ERROR", "La descripción del desastre es obligatoria"),
         body("   ", "Carpas", 5, "MEDIA"), JSON_H),
    item("RF-1 Regla: tipo de recurso vacío", "POST", "{{baseUrl}}/requests",
         rest_error(400, "VALIDATION_ERROR", "El tipo de recurso es obligatorio"),
         body("Deslizamiento", "", 5, "MEDIA"), JSON_H),
    item("RF-1 Regla: prioridad inválida", "POST", "{{baseUrl}}/requests",
         rest_error(400, "VALIDATION_ERROR", "La prioridad debe ser BAJA, MEDIA o ALTA"),
         body("Deslizamiento", "Mantas", 5, "URGENTE"), JSON_H),
    item("RF-1 Regla: prioridad vacía", "POST", "{{baseUrl}}/requests",
         rest_error(400, "VALIDATION_ERROR", "La prioridad es obligatoria"),
         body("Deslizamiento", "Mantas", 5, None), JSON_H),

    item("RF-2 Consultar todas", "GET", "{{baseUrl}}/requests", """
pm.test("HTTP 200", () => pm.response.to.have.status(200));
const list = pm.response.json();
pm.test("es una lista", () => pm.expect(list).to.be.an("array"));
pm.test("incluye la registrada", () =>
  pm.expect(list.map(r => r.id)).to.include(pm.collectionVariables.get("id")));
""", headers=KEY_H),

    item("RF-3 Consultar por id", "GET", "{{baseUrl}}/requests/{{id}}", """
pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("es la solicitud pedida", () => pm.expect(pm.response.json().id).to.eql(pm.collectionVariables.get("id")));
""", headers=KEY_H),
    item("RF-3 Solicitud inexistente", "GET", "{{baseUrl}}/requests/999999999",
         rest_error(404, "REQUEST_NOT_FOUND", "No existe la solicitud con id 999999999"), headers=KEY_H),

    item("RF-5 Pendientes incluye la nueva", "GET", "{{baseUrl}}/requests/pending", """
pm.test("HTTP 200", () => pm.response.to.have.status(200));
const list = pm.response.json();
pm.test("solo PENDIENTE", () => list.forEach(r => pm.expect(r.status).to.eql("PENDIENTE")));
pm.test("incluye la registrada", () =>
  pm.expect(list.map(r => r.id)).to.include(pm.collectionVariables.get("id")));
""", headers=KEY_H),

    item("RF-4 Atender", "PUT", "{{baseUrl}}/requests/{{id}}/attend", """
pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("queda ATENDIDA", () => pm.expect(pm.response.json().status).to.eql("ATENDIDA"));
""", headers=KEY_H),
    item("RF-4 Regla: atender dos veces", "PUT", "{{baseUrl}}/requests/{{id}}/attend",
         """
pm.test("HTTP 409", () => pm.response.to.have.status(409));
pm.test("código ALREADY_ATTENDED", () => pm.expect(pm.response.json().code).to.eql("ALREADY_ATTENDED"));
pm.test("mensaje de negocio", () =>
  pm.expect(pm.response.json().message).to.eql("La solicitud con id " + pm.collectionVariables.get("id") + " ya fue atendida"));
""", headers=KEY_H),
    item("RF-4 Regla: atender inexistente", "PUT", "{{baseUrl}}/requests/999999999/attend",
         rest_error(404, "REQUEST_NOT_FOUND"), headers=KEY_H),

    item("RF-5 Pendientes ya no la incluye", "GET", "{{baseUrl}}/requests/pending", """
pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("la atendida salió de pendientes", () =>
  pm.expect(pm.response.json().map(r => r.id)).to.not.include(pm.collectionVariables.get("id")));
""", headers=KEY_H),
]

rest = collection(
    "Taller 4 - REST (RF-1..RF-5)",
    "Operaciones REST de services. Se corre contra el gateway (entorno gateway) o directo contra services "
    "(entorno directo). Contrato en docs/api.md.",
    rest_items)
rest["variable"] = [{"key": "id", "value": ""}]

# ---------------------------------------------------------------- Gateway (QA-SEG-1)
gateway = collection(
    "Taller 4 - Gateway (QA-SEG-1)",
    "Controles del API Gateway: sin API key o con una clave inválida la petición se rechaza en Kong (401).",
    [
        item("Sin API key", "GET", "{{baseUrl}}/requests", """
pm.test("HTTP 401 en el gateway", () => pm.response.to.have.status(401));
pm.test("responde Kong, no services", () => pm.expect(pm.response.json().message).to.eql("No API key found in request"));
"""),
        item("API key inválida", "GET", "{{baseUrl}}/requests", """
pm.test("HTTP 401 en el gateway", () => pm.response.to.have.status(401));
pm.test("responde Kong, no services", () => pm.expect(pm.response.json().message).to.eql("Unauthorized"));
""", headers=[{"key": "apikey", "value": "clave-equivocada"}]),
        item("Con API key: pasa y lleva X-Correlation-ID", "GET", "{{baseUrl}}/requests", """
pm.test("no la rechaza el gateway", () => pm.expect(pm.response.code).to.not.be.oneOf([401, 403, 429]));
pm.test("X-Correlation-ID en la respuesta", () => pm.expect(pm.response.headers.get("X-Correlation-ID")).to.be.a("string"));
pm.test("informa el límite de peticiones", () => pm.expect(pm.response.headers.get("X-RateLimit-Limit-Second")).to.eql("20"));
""", headers=KEY_H),
    ])

# ---------------------------------------------------------------- SOAP
SOAP_H = [{"key": "Content-Type", "value": "text/xml; charset=utf-8"}, {"key": "SOAPAction", "value": "\"\""}]


def envelope(inner):
    return (f'<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:r="{NS}">\n'
            f'  <soapenv:Header/>\n  <soapenv:Body>\n{inner}\n  </soapenv:Body>\n</soapenv:Envelope>')


def register(desc, rtype, qty, prio):
    return envelope(f"""    <r:registerRequest>
      <request>
        <disasterDescription>{desc}</disasterDescription>
        <resourceType>{rtype}</resourceType>
        <quantity>{qty}</quantity>
        <priority>{prio}</priority>
      </request>
    </r:registerRequest>""")


def by_id(op, value="{{soapId}}"):
    return envelope(f"    <r:{op}>\n      <id>{value}</id>\n    </r:{op}>")


def no_args(op):
    return envelope(f"    <r:{op}/>")


# Helpers JS: leen elementos sin depender del prefijo de namespace que elija JAX-WS
SOAP_HELPERS = r"""
const xml = pm.response.text();
const all = tag => [...xml.matchAll(new RegExp("<(?:\\w+:)?" + tag + ">([^<]*)</(?:\\w+:)?" + tag + ">", "g"))].map(m => m[1]);
const one = tag => all(tag)[0];
"""


def soap_fault(code, message=None):
    msg = f"""
pm.test("mensaje de negocio", () => pm.expect(one("message")).to.eql("{message}"));""" if message else ""
    return SOAP_HELPERS + f"""
pm.test("SOAP Fault (HTTP 500)", () => {{
  pm.response.to.have.status(500);
  pm.expect(xml).to.match(/<(\\w+:)?Fault>/);
}});
pm.test("código {code} en el detail", () => pm.expect(one("code")).to.eql("{code}"));{msg}
"""


soap_items = [
    item("WSDL publicado", "GET", "{{soapUrl}}?wsdl", """
pm.test("HTTP 200", () => pm.response.to.have.status(200));
const wsdl = pm.response.text();
pm.test("es un WSDL del namespace acordado", () => pm.expect(wsdl).to.include('targetNamespace="%s"'));
["registerRequest", "listRequests", "getRequest", "attendRequest", "listPendingRequests"].forEach(op =>
  pm.test("expone " + op, () => pm.expect(wsdl).to.include('name="' + op + '"')));
""" % NS),
    item("RF-1 registerRequest válida", "POST", "{{soapUrl}}", SOAP_HELPERS + """
pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("nace PENDIENTE", () => pm.expect(one("status")).to.eql("PENDIENTE"));
pm.test("tiene id", () => pm.expect(Number(one("id"))).to.be.above(0));
pm.collectionVariables.set("soapId", one("id"));
""", register("Sismo en Pasto {{$timestamp}}", "Medicamentos", 40, "ALTA"), SOAP_H),
    item("RF-1 Regla: cantidad 0", "POST", "{{soapUrl}}",
         soap_fault("VALIDATION_ERROR", "La cantidad debe ser mayor que cero"),
         register("Sismo en Pasto", "Medicamentos", 0, "ALTA"), SOAP_H),
    item("RF-1 Regla: prioridad inválida", "POST", "{{soapUrl}}",
         soap_fault("VALIDATION_ERROR", "La prioridad debe ser BAJA, MEDIA o ALTA"),
         register("Sismo en Pasto", "Medicamentos", 10, "URGENTE"), SOAP_H),
    item("RF-1 Regla: prioridad vacía", "POST", "{{soapUrl}}",
         soap_fault("VALIDATION_ERROR", "La prioridad es obligatoria"),
         register("Sismo en Pasto", "Medicamentos", 10, ""), SOAP_H),
    item("RF-2 listRequests", "POST", "{{soapUrl}}", SOAP_HELPERS + """
pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("incluye la registrada", () => pm.expect(all("id")).to.include(pm.collectionVariables.get("soapId")));
""", no_args("listRequests"), SOAP_H),
    item("RF-3 getRequest", "POST", "{{soapUrl}}", SOAP_HELPERS + """
pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("es la solicitud pedida", () => pm.expect(one("id")).to.eql(pm.collectionVariables.get("soapId")));
""", by_id("getRequest"), SOAP_H),
    item("RF-3 Solicitud inexistente", "POST", "{{soapUrl}}",
         soap_fault("REQUEST_NOT_FOUND", "No existe la solicitud con id 999999999"),
         by_id("getRequest", "999999999"), SOAP_H),
    item("RF-5 listPendingRequests incluye la nueva", "POST", "{{soapUrl}}", SOAP_HELPERS + """
pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("solo PENDIENTE", () => all("status").forEach(s => pm.expect(s).to.eql("PENDIENTE")));
pm.test("incluye la registrada", () => pm.expect(all("id")).to.include(pm.collectionVariables.get("soapId")));
""", no_args("listPendingRequests"), SOAP_H),
    item("RF-4 attendRequest", "POST", "{{soapUrl}}", SOAP_HELPERS + """
pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("queda ATENDIDA", () => pm.expect(one("status")).to.eql("ATENDIDA"));
""", by_id("attendRequest"), SOAP_H),
    item("RF-4 Regla: atender dos veces", "POST", "{{soapUrl}}", soap_fault("ALREADY_ATTENDED"),
         by_id("attendRequest"), SOAP_H),
    item("RF-5 listPendingRequests ya no la incluye", "POST", "{{soapUrl}}", SOAP_HELPERS + """
pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("la atendida salió de pendientes", () => pm.expect(all("id")).to.not.include(pm.collectionVariables.get("soapId")));
""", no_args("listPendingRequests"), SOAP_H),
]

soap = collection(
    "Taller 4 - SOAP (RF-1..RF-5)",
    f"Servicio ResourceRequestSoapService de services (directo, sin gateway). Namespace {NS}. Contrato en docs/api.md.",
    soap_items)
soap["variable"] = [{"key": "soapId", "value": ""}]

OUT.mkdir(parents=True, exist_ok=True)
files = {
    "rest.postman_collection.json": rest,
    "gateway.postman_collection.json": gateway,
    "soap.postman_collection.json": soap,
    # Newman corre en un contenedor dentro de la red de Compose: usa los nombres de servicio
    "gateway.postman_environment.json": env("gateway", {
        "baseUrl": "http://gateway:8000/api/v1", "apiKey": "clave-taller4",
        "soapUrl": "http://services:8080/services/ResourceRequestSoapService"}),
    "directo.postman_environment.json": env("directo", {
        "baseUrl": "http://services:8080/services/api/v1", "apiKey": "no-se-usa",
        "soapUrl": "http://services:8080/services/ResourceRequestSoapService"}),
    "local.postman_environment.json": env("local (Postman en el equipo)", {
        "baseUrl": "http://localhost:8000/api/v1", "apiKey": "clave-taller4",
        "soapUrl": "http://localhost:8082/services/ResourceRequestSoapService"}),
}
for name, data in files.items():
    (OUT / name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print("escrito", OUT / name)
