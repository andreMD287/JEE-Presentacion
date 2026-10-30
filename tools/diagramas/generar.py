"""Genera los diagramas estructurales de docs/arquitectura.md como SVG con posiciones fijas.

Se dibujan a mano (coordenadas explícitas) para controlar la estructura y evitar cruces de flechas, cosa que el
acomodo automático de Mermaid no garantiza. Las secuencias siguen en Mermaid (archivos *.mmd).

Uso:  python generar.py            -> escribe los .svg en esta carpeta
      node render.js               -> exporta .svg y .mmd a .png (ver README.md)
"""
from pathlib import Path
from xml.sax.saxutils import escape

# Los SVG intermedios van a build/; render.js los exporta a PNG en docs/diagramas/
OUT = Path(__file__).resolve().parent / "build"
OUT.mkdir(exist_ok=True)
FONT = "Segoe UI, Helvetica, Arial, sans-serif"
INK = "#1f1f1f"
MUTED = "#555555"
NODE_FILL = "#f3f3f3"
NODE_TOP = "#e2e2e2"
NODE_SIDE = "#cfcfcf"
DEPTH = 10


def text_width(s, size):
    return len(s) * size * 0.56


class Svg:
    def __init__(self, w, h):
        self.w, self.h, self.parts = w, h, []

    def add(self, s):
        self.parts.append(s)

    # ------------------------------------------------------------------ texto
    def text(self, x, y, s, size=13, weight="normal", anchor="start", fill=INK, italic=False):
        style = ' font-style="italic"' if italic else ""
        self.add(f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{weight}" text-anchor="{anchor}" '
                 f'fill="{fill}"{style}>{escape(s)}</text>')

    def label(self, x, y, lines, size=11.5):
        """Etiqueta de conector centrada en (x, y) con fondo blanco."""
        if isinstance(lines, str):
            lines = [lines]
        w = max(text_width(s, size) for s in lines) + 10
        h = len(lines) * (size + 3) + 6
        top = y - h / 2
        self.add(f'<rect x="{x - w / 2:.1f}" y="{top:.1f}" width="{w:.1f}" height="{h:.1f}" fill="white" rx="3"/>')
        for i, s in enumerate(lines):
            self.text(x, top + 4 + (i + 1) * (size + 3) - 3, s, size=size, anchor="middle", fill=MUTED)

    # ------------------------------------------------------------------ formas
    def node(self, x, y, w, h, title, subtitle=None, badge=None):
        """Nodo UML 3D (servidor o contenedor)."""
        d = DEPTH
        self.add(f'<polygon points="{x},{y} {x + d},{y - d} {x + w + d},{y - d} {x + w},{y}" '
                 f'fill="{NODE_TOP}" stroke="{INK}" stroke-width="1.2"/>')
        self.add(f'<polygon points="{x + w},{y} {x + w + d},{y - d} {x + w + d},{y + h - d} {x + w},{y + h}" '
                 f'fill="{NODE_SIDE}" stroke="{INK}" stroke-width="1.2"/>')
        self.add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{NODE_FILL}" stroke="{INK}" stroke-width="1.2"/>')
        self.text(x + 12, y + 22, title, size=14, weight="bold")
        if subtitle:
            self.text(x + 12, y + 39, subtitle, size=11.5, fill=MUTED)
        if badge:
            self.badge(x + w - 18, y + 18, badge)

    def badge(self, cx, cy, s):
        self.add(f'<circle cx="{cx}" cy="{cy}" r="11" fill="{INK}"/>')
        self.text(cx, cy + 4.5, s, size=12, weight="bold", anchor="middle", fill="white")

    def component(self, x, y, w, name, stereo=None, lines=(), fill="white"):
        """Componente UML: caja con el ícono de componente. Devuelve la altura."""
        h = 12 + (15 if stereo else 0) + 17 + 15 * len(lines) + 8
        self.add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{fill}" stroke="{INK}" stroke-width="1.1" rx="2"/>')
        ix, iy = x + w - 22, y + 7
        self.add(f'<rect x="{ix}" y="{iy}" width="14" height="16" fill="white" stroke="{INK}" stroke-width="0.9"/>')
        self.add(f'<rect x="{ix - 4}" y="{iy + 3}" width="8" height="3.5" fill="white" stroke="{INK}" stroke-width="0.9"/>')
        self.add(f'<rect x="{ix - 4}" y="{iy + 9.5}" width="8" height="3.5" fill="white" stroke="{INK}" stroke-width="0.9"/>')
        cy = y + 12
        cx = x + (w - 18) / 2
        if stereo:
            cy += 12
            self.text(cx, cy, f"«{stereo}»", size=11, anchor="middle", fill=MUTED)
            cy += 3
        cy += 15
        self.text(cx, cy, name, size=13, weight="bold", anchor="middle")
        for s in lines:
            cy += 15
            self.text(cx, cy, s, size=11.5, anchor="middle", fill=MUTED)
        return h

    def box(self, x, y, w, h, title, lines=(), stereo=None, dashed=False):
        """Caja simple (sistema externo)."""
        dash = ' stroke-dasharray="5 3"' if dashed else ""
        self.add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="white" stroke="{INK}" stroke-width="1.2" rx="4"{dash}/>')
        cy = y + 20
        if stereo:
            self.text(x + w / 2, cy, f"«{stereo}»", size=11, anchor="middle", fill=MUTED)
            cy += 17
        self.text(x + w / 2, cy, title, size=13.5, weight="bold", anchor="middle")
        for s in lines:
            cy += 16
            self.text(x + w / 2, cy, s, size=11.5, anchor="middle", fill=MUTED)

    def classbox(self, x, y, w, name, stereo=None, members=(), italic=False):
        """Clase UML (nombre + miembros). Devuelve la altura."""
        head = 10 + (14 if stereo else 0) + 18
        h = head + 8 + 15 * len(members) + (4 if members else 0)
        self.add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="white" stroke="{INK}" stroke-width="1.1"/>')
        cy = y + 6
        if stereo:
            cy += 12
            self.text(x + w / 2, cy, f"«{stereo}»", size=10.5, anchor="middle", fill=MUTED)
        cy += 17
        self.text(x + w / 2, cy, name, size=12.5, weight="bold", anchor="middle", italic=italic)
        sep = y + head
        self.add(f'<line x1="{x}" y1="{sep}" x2="{x + w}" y2="{sep}" stroke="{INK}" stroke-width="0.9"/>')
        cy = sep + 4
        for m in members:
            cy += 15
            self.text(x + 8, cy, m, size=11.2)
        return h

    def package(self, x, y, w, h, name):
        tab_w = text_width(name, 12.5) + 22
        self.add(f'<rect x="{x}" y="{y - 20}" width="{tab_w}" height="20" fill="{NODE_TOP}" stroke="{INK}" stroke-width="1.1"/>')
        self.add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="#fafafa" stroke="{INK}" stroke-width="1.1"/>')
        self.text(x + 10, y - 5, name, size=12.5, weight="bold")

    def actor(self, cx, top, label_lines):
        self.add(f'<circle cx="{cx}" cy="{top + 11}" r="11" fill="white" stroke="{INK}" stroke-width="1.4"/>')
        self.add(f'<line x1="{cx}" y1="{top + 22}" x2="{cx}" y2="{top + 52}" stroke="{INK}" stroke-width="1.4"/>')
        self.add(f'<line x1="{cx - 17}" y1="{top + 32}" x2="{cx + 17}" y2="{top + 32}" stroke="{INK}" stroke-width="1.4"/>')
        self.add(f'<line x1="{cx}" y1="{top + 52}" x2="{cx - 14}" y2="{top + 74}" stroke="{INK}" stroke-width="1.4"/>')
        self.add(f'<line x1="{cx}" y1="{top + 52}" x2="{cx + 14}" y2="{top + 74}" stroke="{INK}" stroke-width="1.4"/>')
        for i, s in enumerate(label_lines):
            self.text(cx, top + 92 + 15 * i, s, size=12, anchor="middle")

    def cylinder(self, x, y, w, h, lines):
        ry = 9
        self.add(f'<path d="M{x},{y + ry} L{x},{y + h - ry} A{w / 2},{ry} 0 0 0 {x + w},{y + h - ry} '
                 f'L{x + w},{y + ry}" fill="{NODE_FILL}" stroke="{INK}" stroke-width="1.2"/>')
        self.add(f'<ellipse cx="{x + w / 2}" cy="{y + ry}" rx="{w / 2}" ry="{ry}" fill="{NODE_TOP}" stroke="{INK}" stroke-width="1.2"/>')
        for i, s in enumerate(lines):
            self.text(x + w / 2, y + h / 2 + 8 + 15 * i - 7 * (len(lines) - 1), s, size=12.5 if i == 0 else 11.5,
                      weight="bold" if i == 0 else "normal", anchor="middle", fill=INK if i == 0 else MUTED)

    def note(self, x, y, w, lines, size=11.5):
        h = 14 + len(lines) * (size + 4)
        f = 10
        self.add(f'<path d="M{x},{y} L{x + w - f},{y} L{x + w},{y + f} L{x + w},{y + h} L{x},{y + h} Z" '
                 f'fill="#fbfbf3" stroke="{INK}" stroke-width="1"/>')
        self.add(f'<path d="M{x + w - f},{y} L{x + w - f},{y + f} L{x + w},{y + f}" fill="none" stroke="{INK}" stroke-width="1"/>')
        for i, s in enumerate(lines):
            self.text(x + 9, y + 8 + (i + 1) * (size + 4) - 3, s, size=size)
        return h

    def port(self, x, y, label, label_side="left"):
        self.add(f'<rect x="{x - 6}" y="{y - 6}" width="12" height="12" fill="white" stroke="{INK}" stroke-width="1.3"/>')
        if label_side == "left":
            self.text(x - 10, y - 10, label, size=11.5, weight="bold", anchor="end")
        elif label_side == "right":
            self.text(x + 16, y + 4, label, size=11.5, weight="bold")
        else:
            self.text(x, y + 22, label, size=11.5, weight="bold", anchor="middle")

    # ------------------------------------------------------------------ conectores
    def arrow(self, pts, kind="call", dashed=False):
        """kind: call (punta rellena), dep (punta abierta), real (triángulo hueco), none."""
        marker = {"call": "url(#call)", "dep": "url(#dep)", "real": "url(#real)", "none": ""}[kind]
        dash = ' stroke-dasharray="6 4"' if dashed or kind in ("real", "dep") else ""
        p = " ".join(f"{a},{b}" for a, b in pts)
        end = f' marker-end="{marker}"' if marker else ""
        self.add(f'<polyline points="{p}" fill="none" stroke="{INK}" stroke-width="1.4"{dash}{end}/>')

    def legend(self, x, y, items):
        """items: lista de (tipo, texto) con tipo call/real/dep/dashed-call/badge:<n>."""
        cx = x
        for kind, s in items:
            if kind.startswith("badge:"):
                self.badge(cx + 11, y - 4, kind.split(":")[1])
                self.text(cx + 28, y, s, size=11.5, fill=MUTED)
            else:
                k = "call" if kind == "dashed-call" else kind
                self.arrow([(cx, y - 4), (cx + 38, y - 4)], k, dashed=(kind == "dashed-call"))
                self.text(cx + 46, y, s, size=11.5, fill=MUTED)
            cx += 60 + text_width(s, 11.5)

    def save(self, name):
        defs = f"""<defs>
  <marker id="call" viewBox="0 0 10 10" refX="9.5" refY="5" markerWidth="9" markerHeight="9" orient="auto-start-reverse">
    <path d="M0,0 L10,5 L0,10 Z" fill="{INK}"/></marker>
  <marker id="dep" viewBox="0 0 10 10" refX="9.5" refY="5" markerWidth="9" markerHeight="9" orient="auto-start-reverse">
    <path d="M0,0 L10,5 L0,10" fill="none" stroke="{INK}" stroke-width="1.4"/></marker>
  <marker id="real" viewBox="0 0 12 12" refX="11.5" refY="6" markerWidth="13" markerHeight="13" orient="auto-start-reverse">
    <path d="M0,0 L12,6 L0,12 Z" fill="white" stroke="{INK}" stroke-width="1.2"/></marker>
</defs>"""
        body = "\n".join(self.parts)
        svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.w}" height="{self.h}" '
               f'viewBox="0 0 {self.w} {self.h}" font-family="{FONT}">\n{defs}\n'
               f'<rect width="100%" height="100%" fill="white"/>\n{body}\n</svg>\n')
        (OUT / name).write_text(svg, encoding="utf-8", newline="\n")
        print("escrito", name)


# ====================================================================== 1. Contexto
def contexto():
    s = Svg(1000, 400)
    s.add(f'<rect x="340" y="130" width="300" height="140" fill="{NODE_FILL}" stroke="{INK}" stroke-width="2.2" rx="6"/>')
    s.text(490, 172, "«sistema»", size=11.5, anchor="middle", fill=MUTED)
    s.text(490, 196, "Gestión de recursos", size=16, weight="bold", anchor="middle")
    s.text(490, 216, "en emergencias", size=16, weight="bold", anchor="middle")
    s.text(490, 245, "registrar · consultar · atender solicitudes", size=11.5, anchor="middle", fill=MUTED)

    s.actor(90, 140, ["Operador del", "centro de atención"])
    s.arrow([(125, 200), (340, 200)])
    s.label(232, 200, ["navegador", "HTTP · JSF"])

    s.box(790, 50, 190, 74, "Sistema externo REST", ["p. ej. app móvil, tablero"], stereo="sistema externo")
    s.box(790, 276, 190, 74, "Sistema externo SOAP", ["sistema heredado de un organismo"], stereo="sistema externo")
    s.arrow([(790, 87), (715, 87), (715, 170), (640, 170)])
    s.label(715, 128, ["REST / JSON", "+ API key"])
    s.arrow([(790, 313), (715, 313), (715, 230), (640, 230)])
    s.label(715, 272, ["SOAP 1.1", "contrato WSDL"])
    s.legend(340, 380, [("call", "solicita operaciones (RF-1..RF-5) y recibe la respuesta")])
    s.save("01-contexto.svg")


# Alturas que devuelve component(): 37 + 15 (si tiene estereotipo) + 15 por línea.
# Alturas que devuelve classbox(): 54 (con estereotipo) o 40 (sin él) + 15 por miembro.

# ====================================================================== 2. Etapa 1
def etapa1():
    s = Svg(1500, 480)
    s.actor(55, 110, ["Operador"])
    # presentation
    s.node(120, 60, 290, 370, "presentation", "Tomcat 10.1 · JSF 4 + Weld 5")
    s.component(140, 110, 250, "index.xhtml", "Facelets", ["registro · tabla · búsqueda · pendientes"])            # 110-177
    s.component(140, 205, 250, "ResourceRequestBean", "CDI @Named · @ViewScoped", ["muestra mensajes de negocio y de falla"])  # 205-272
    s.component(140, 315, 250, "EjbRequestClient", "estrategia · BACKEND_MODE=ejb",
                ["usa RemoteEjbLocator"])                                                          # 315-382
    s.arrow([(265, 177), (265, 205)])
    s.arrow([(265, 272), (265, 315)])
    s.arrow([(80, 142), (140, 142)])
    s.label(108, 128, "HTTP")
    # logic
    s.node(540, 60, 330, 370, "logic", "WildFly 38 · EJB")
    s.component(560, 110, 290, "ResourceRequestServiceBean", "@Stateless · NOT_SUPPORTED",
                ["implementa ResourceRequestService", "reglas de negocio RN-1..RN-6"])        # 110-192
    s.component(560, 225, 290, "ResourceRequestRepository", "puerto del dominio",
                ["save · findById · findAll"])                                                     # 225-292
    s.component(560, 325, 290, "RemoteResourceRequestRepository", "adaptador",
                ["traduce dominio ⇄ ResourceRequestData"])                                         # 325-392
    s.arrow([(705, 192), (705, 225)])
    s.label(735, 208, "usa")
    s.arrow([(705, 325), (705, 292)], "real")
    s.label(755, 308, "implementa")
    s.arrow([(390, 350), (475, 350), (475, 150), (560, 150)])
    s.label(475, 250, ["EJB remoto", "remote+http", "+ usuario de", "aplicación"])
    # data
    s.node(1000, 60, 300, 370, "data", "WildFly 38 · JPA + JTA")
    s.component(1020, 110, 260, "ResourceRequestDataServiceBean", "@Stateless · JTA",
                ["implementa ResourceRequestDataService", "una transacción por llamada"])          # 110-192
    s.component(1020, 225, 260, "ResourceRequestEntity", "@Entity", ["tabla resource_request"])  # 225-292
    s.component(1020, 325, 260, "RecursosDS", "datasource JTA", ["pool de conexiones JDBC"])     # 325-392
    s.arrow([(1150, 192), (1150, 225)])
    s.label(1188, 208, "persiste")
    s.arrow([(1150, 292), (1150, 325)])
    s.label(1172, 308, "usa")
    s.arrow([(850, 360), (935, 360), (935, 150), (1020, 150)])
    s.label(935, 255, ["EJB remoto", "remote+http"])
    # base de datos
    s.cylinder(1380, 305, 100, 110, ["PostgreSQL", "16"])
    s.arrow([(1280, 360), (1380, 360)])
    s.label(1330, 345, "JDBC")
    s.legend(120, 466, [("call", "llamada / uso"), ("real", "implementa (el adaptador implementa el puerto)")])
    s.save("02-etapa1.svg")


# ====================================================================== 3. Etapas 2 y 3
def etapas23():
    s = Svg(1460, 670)
    # clientes
    s.box(30, 70, 200, 72, "Cliente REST", ["Postman · Newman · apps"], stereo="sistema externo")
    s.box(30, 200, 200, 72, "presentation (JSF)", ["RestRequestClient"], stereo="BACKEND_MODE=rest")
    s.box(30, 470, 200, 72, "Cliente SOAP", ["SoapUI · Newman · heredados"], stereo="sistema externo")
    s.badge(218, 84, "2")
    s.badge(218, 214, "3")
    s.badge(218, 484, "3")
    # gateway
    s.node(320, 50, 270, 300, "gateway", "Kong 3.9 · DB-less (kong.yml)", badge="3")
    s.component(340, 100, 230, "key-auth", lines=["sin API key o inválida → 401", "QA-SEG-1"])     # 100-167
    s.component(340, 185, 230, "rate-limiting", lines=["> 20 peticiones/s → 429", "QA-DIS-2"])    # 185-252
    s.component(340, 270, 230, "correlation-id", lines=["agrega X-Correlation-ID", "trazabilidad"])  # 270-337
    s.arrow([(455, 167), (455, 185)])
    s.arrow([(455, 252), (455, 270)])
    s.arrow([(230, 106), (290, 106), (290, 133)], "none")
    s.arrow([(230, 236), (290, 236), (290, 133)], "none")
    s.arrow([(290, 133), (340, 133)])
    s.label(268, 172, ["REST", "+ apikey"])
    # services
    s.node(690, 50, 320, 520, "services", "WildFly 38 · fachada sin reglas", badge="2")
    s.component(710, 105, 280, "ResourceRequestResource", "JAX-RS", ["/services/api/v1/requests"])  # 105-172
    s.component(710, 205, 280, "ApiExceptionMapper", "JAX-RS @Provider",
                ["BusinessException.code → 400 / 404 / 409"])                                       # 205-272
    s.component(710, 365, 280, "BusinessFault", "@WebFault", ["SOAP Fault con code y message"])     # 365-432
    s.component(710, 465, 280, "ResourceRequestSoapService", "JAX-WS", ["WSDL · 5 operaciones"])     # 465-532
    s.arrow([(850, 172), (850, 205)])
    s.label(884, 189, "errores")
    s.arrow([(850, 465), (850, 432)])
    s.label(884, 449, "errores")
    s.arrow([(570, 303), (630, 303), (630, 140), (710, 140)])
    s.label(630, 222, ["REST", "HTTP"])
    s.arrow([(230, 505), (710, 505)])
    s.label(460, 505, "SOAP 1.1 por HTTP (sin gateway)")
    # logic
    s.node(1110, 60, 320, 210, "logic", "WildFly 38 · sin cambios desde la etapa 1")
    s.component(1130, 110, 280, "ResourceRequestServiceBean", "@Stateless", ["reglas RN-1..RN-6"])  # 110-177
    s.component(1130, 195, 280, "RemoteResourceRequestRepository", "adaptador")                   # 195-247
    s.arrow([(1270, 177), (1270, 195)])
    s.arrow([(990, 135), (1130, 135)])
    s.arrow([(990, 498), (1060, 498), (1060, 158), (1130, 158)])
    s.label(1060, 330, ["EJB remoto", "remote+http"])
    # data + bd
    s.node(1110, 330, 320, 140, "data", "WildFly 38 · JPA + JTA")
    s.component(1130, 380, 280, "ResourceRequestDataServiceBean", "@Stateless · JTA")              # 380-432
    s.arrow([(1270, 247), (1270, 380)])
    s.label(1270, 300, "EJB remoto")
    s.cylinder(1225, 525, 90, 90, ["PostgreSQL"])
    s.arrow([(1270, 432), (1270, 525)])
    s.label(1270, 492, "JDBC")
    s.legend(30, 652, [("badge:2", "agregado en la etapa 2"), ("badge:3", "agregado en la etapa 3"),
                       ("call", "llamada (protocolo en la etiqueta)")])
    s.save("03-etapas2-3.svg")


# ====================================================================== 4. Vista lógica
def logica():
    s = Svg(1360, 800)
    # presentation-jsf
    s.package(30, 50, 410, 255, "presentation-jsf")
    s.classbox(45, 80, 180, "ResourceRequestBean", "CDI @ViewScoped", ["+ register()", "+ attend(id)", "+ search()"])
    s.classbox(245, 80, 180, "RequestClient", "interface", ["register · findAll · findById", "attend · findPending"],
               italic=True)                                                                         # 80-164
    s.classbox(45, 210, 180, "RestRequestClient", "modo rest", ["HTTP + apikey vía Kong"])         # 210-279
    s.classbox(245, 210, 180, "EjbRequestClient", "modo ejb", ["EJB remoto a logic"])             # 210-279
    s.arrow([(225, 112), (245, 112)], "dep")
    s.arrow([(135, 210), (135, 190), (335, 190), (335, 164)], "real")
    s.arrow([(335, 210), (335, 190)], "none", dashed=True)
    # services
    s.package(30, 355, 410, 235, "services")
    s.classbox(45, 385, 180, "ApiExceptionMapper", "JAX-RS @Provider", ["code → 400 / 404 / 409"])
    s.classbox(245, 385, 180, "ResourceRequestResource", "JAX-RS", ["/api/v1/requests"])           # 385-454
    s.classbox(45, 490, 180, "BusinessFault", "@WebFault", ["code · message"])
    s.classbox(245, 490, 180, "ResourceRequestSoapService", "JAX-WS", ["5 operaciones"])           # 490-559
    s.arrow([(245, 422), (225, 422)], "dep")
    s.arrow([(245, 527), (225, 527)], "dep")
    # contracts
    s.package(500, 50, 360, 720, "contracts")
    s.classbox(515, 80, 330, "ResourceRequestService", "interface @Remote",
               ["+ register(CreateResourceRequestCommand) : View", "+ findAll() : List<View>",
                "+ findById(Long) : View", "+ attend(Long) : View", "+ findPending() : List<View>"],
               italic=True)                                                                         # 80-209
    s.classbox(515, 230, 330, "DTO serializables", "JavaBeans",
               ["CreateResourceRequestCommand", "ResourceRequestView · ResourceRequestData",
                "Priority {BAJA, MEDIA, ALTA}", "RequestStatus {PENDIENTE, ATENDIDA}"])            # 230-344
    s.classbox(515, 365, 330, "BusinessException", "@ApplicationException · abstracta",
               ["+ getCode() : String", "BusinessValidationException → VALIDATION_ERROR",
                "RequestNotFoundException → REQUEST_NOT_FOUND", "AlreadyAttendedException → ALREADY_ATTENDED",
                "ConcurrentUpdateException → CONCURRENT_UPDATE (interna)"],
               italic=True)                                                                         # 365-494
    s.classbox(515, 525, 330, "ResourceRequestDataService", "interface @Remote",
               ["+ save(ResourceRequestData) : ResourceRequestData", "+ findById(Long) : Data (null si no existe)",
                "+ findAll() : List<Data>"], italic=True)                                           # 525-624
    s.classbox(515, 655, 330, "RemoteEjbLocator", "utilidad",
               ["+ lookup(urlVar, jndi, tipo) · timeout 5 s", "la usan los tres clientes EJB"])     # 655-739
    # clientes de ResourceRequestService (un solo bus, sin cruces)
    for y in (245, 420, 525):
        s.arrow([(425, y), (465, y)], "none", dashed=True)
    s.arrow([(465, 525), (465, 135), (515, 135)], "dep")
    s.add(f'<rect x="469" y="262" width="18" height="136" fill="#fafafa"/>')
    s.add(f'<text x="482" y="330" font-size="11.5" fill="{MUTED}" transform="rotate(-90 482 330)" '
          f'text-anchor="middle">usan por EJB remoto</text>')
    # logic
    s.package(920, 50, 430, 420, "logic")
    s.classbox(935, 80, 380, "ResourceRequestServiceBean", "@Stateless · NOT_SUPPORTED",
               ["reglas de negocio RN-1..RN-6"])                                               # 80-149
    s.classbox(935, 200, 230, "ResourceRequestRepository", "interface · puerto",
               ["save · findById · findAll"], italic=True)                                          # 200-269
    s.classbox(1185, 200, 150, "ResourceRequest", "dominio", ["id · estado …"])
    s.classbox(935, 345, 380, "RemoteResourceRequestRepository", "adaptador",
               ["traduce dominio ⇄ ResourceRequestData", "usa RemoteEjbLocator"])                  # 345-429
    s.arrow([(935, 115), (845, 115)], "real")
    s.arrow([(1050, 149), (1050, 200)], "dep")
    s.arrow([(1050, 345), (1050, 269)], "real")
    s.arrow([(1165, 234), (1185, 234)], "dep")
    s.arrow([(935, 387), (885, 387), (885, 560), (845, 560)], "dep")
    s.label(885, 475, ["EJB", "remoto"])
    # data
    s.package(920, 545, 430, 135, "data")
    s.classbox(935, 575, 230, "ResourceRequestDataServiceBean", "@Stateless · JTA", ["- EntityManager em"])  # 575-644
    s.classbox(1185, 575, 150, "ResourceRequestEntity", "@Entity", ["resource_request"])
    s.arrow([(935, 605), (845, 605)], "real")
    s.arrow([(1165, 610), (1185, 610)], "dep")
    s.legend(30, 790, [("real", "implementa"), ("dep", "usa / depende de")])
    s.save("04-vista-logica.svg")


# ====================================================================== 5. Vista de desarrollo
def desarrollo():
    s = Svg(1220, 485)
    mods = [
        ("presentation-jsf", "recursos.war", "Tomcat · presentation",
         ["beans CDI y páginas Facelets", "RequestClient + 2 estrategias", "Faces 4.0.28 · Weld 5.1.7",
          "cliente EJB de WildFly 38.0.1"]),
        ("services", "services.war", "WildFly · services",
         ["recurso JAX-RS + ApiExceptionMapper", "endpoint JAX-WS + BusinessFault"]),
        ("logic", "logic.war", "WildFly · logic",
         ["ResourceRequestServiceBean", "puerto ResourceRequestRepository", "adaptador remoto",
          "dominio ResourceRequest"]),
        ("data", "data.war", "WildFly · data",
         ["ResourceRequestDataServiceBean", "ResourceRequestEntity", "persistence.xml"]),
    ]
    for i, (name, art, where, lines) in enumerate(mods):
        x = 30 + i * 297
        cx = x + 160
        s.add(f'<rect x="{x + 60}" y="40" width="200" height="58" fill="white" stroke="{INK}" stroke-width="1.1"/>')
        s.text(cx, 58, "«artifact»", size=11, anchor="middle", fill=MUTED)
        s.text(cx, 75, art, size=13, weight="bold", anchor="middle")
        s.text(cx, 91, where, size=11, anchor="middle", fill=MUTED)
        s.package(x, 165, 270, 92, name)
        for j, t in enumerate(lines):
            s.text(x + 12, 187 + 18 * j, "· " + t, size=11.8)
        s.arrow([(cx, 165), (cx, 98)], "dep")
        s.label(cx, 128, "se empaqueta en")
        s.arrow([(cx, 257), (cx, 310)], "none", dashed=True)
    s.arrow([(190, 310), (1081, 310)], "none", dashed=True)
    s.arrow([(610, 310), (610, 355)], "dep")
    s.label(800, 310, "todos dependen solo de contracts")
    s.package(400, 375, 420, 62, "contracts (jar, va dentro de cada WAR)")
    for j, t in enumerate(["interfaces @Remote · DTO JavaBeans · excepciones con code",
                           "RemoteEjbLocator (conexión EJB y timeout) · JndiNames"]):
        s.text(412, 398 + 18 * j, "· " + t, size=11.8)
    s.note(860, 362, 330, ["logic no depende de data: solo conoce la", "interfaz ResourceRequestDataService y su",
                           "nombre JNDI. Se puede cambiar Datos sin", "recompilar la Lógica."])
    s.legend(30, 470, [("dep", "depende de / se empaqueta en")])
    s.save("05-vista-desarrollo.svg")


# ====================================================================== 9. Despliegue
def despliegue():
    s = Svg(1510, 730)
    # clientes externos
    s.box(25, 130, 170, 60, "Navegador", ["operador"])
    s.box(25, 310, 170, 60, "Cliente REST", ["Postman · Newman"])
    s.box(25, 475, 170, 60, "Cliente SOAP", ["SoapUI · Newman"])
    # equipo anfitrión y red de Docker
    s.node(240, 40, 1210, 650, "Equipo anfitrión", "Docker Desktop · Docker Compose (proyecto jee-presentacion)")
    s.add(f'<rect x="275" y="94" width="1145" height="576" fill="none" stroke="{MUTED}" stroke-width="1.1" '
          f'stroke-dasharray="7 4" rx="8"/>')
    s.text(290, 660, "red jee-presentacion_default", size=11.5, fill=MUTED, italic=True)

    def ctr(x, y, name, image, artifact, order):
        s.node(x, y, 230, 112, name, image, badge=order)
        s.add(f'<rect x="{x + 14}" y="{y + 54}" width="202" height="44" fill="white" stroke="{INK}" stroke-width="1"/>')
        s.text(x + 115, y + 70, "«artifact»", size=10.5, anchor="middle", fill=MUTED)
        s.text(x + 115, y + 87, artifact, size=12, weight="bold", anchor="middle")

    ctr(330, 120, "presentation", "tomcat:10.1-jdk17-temurin", "recursos.war", "6")   # 120-232
    ctr(330, 300, "gateway", "kong:3.9.4 · DB-less", "kong.yml", "5")                 # 300-412
    ctr(640, 420, "services", "wildfly:38.0.1.Final-jdk17", "services.war", "4")      # 420-532
    ctr(950, 120, "logic", "wildfly:38.0.1.Final-jdk17", "logic.war", "3")            # 120-232
    ctr(950, 420, "data", "wildfly:38.0.1.Final-jdk17", "data.war", "2")              # 420-532
    s.text(1075, 252, "sin puerto publicado", size=11, fill=MUTED, italic=True)
    s.text(955, 552, "sin puerto publicado", size=11, fill=MUTED, italic=True)
    s.node(1200, 280, 180, 112, "postgres", "postgres:16-alpine", badge="1")           # 280-392
    s.cylinder(1222, 336, 136, 46, ["resource_request"])
    s.cylinder(1275, 565, 110, 70, ["volumen", "postgres_data"])
    s.arrow([(1330, 392), (1330, 565)], "none", dashed=True)
    # puertos publicados en el borde del anfitrión
    s.port(240, 160, ":8080")
    s.port(240, 340, ":8000")
    s.port(240, 505, ":8082")
    s.port(1450, 336, ":5434", label_side="right")
    # conexiones
    s.arrow([(195, 160), (330, 160)])
    s.arrow([(195, 340), (330, 340)])
    s.arrow([(195, 505), (640, 505)])
    s.label(455, 505, "SOAP · HTTP :8080")
    s.arrow([(445, 232), (445, 300)], dashed=True)
    s.label(445, 266, "modo rest")
    s.arrow([(560, 160), (950, 160)])
    s.label(755, 160, "remote+http :8080 (modo ejb)")
    s.arrow([(560, 360), (600, 360), (600, 460), (640, 460)])
    s.label(600, 410, ["HTTP", ":8080"])
    s.arrow([(870, 460), (905, 460), (905, 195), (950, 195)])
    s.label(905, 330, ["remote", "+http"])
    s.arrow([(1065, 232), (1065, 420)])
    s.label(1065, 330, "remote+http :8080")
    s.arrow([(1180, 450), (1215, 450), (1215, 392)])
    s.label(1262, 428, "JDBC :5432")
    s.arrow([(1380, 336), (1444, 336)], "none")
    s.legend(330, 714, [("badge:1", "orden de arranque (depends_on + healthcheck)"),
                        ("call", "conexión (protocolo y puerto interno)"),
                        ("dashed-call", "solo con BACKEND_MODE=rest")])
    s.save("09-despliegue.svg")


if __name__ == "__main__":
    contexto()
    etapa1()
    etapas23()
    logica()
    desarrollo()
    despliegue()
