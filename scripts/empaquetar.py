"""Genera Taller4_Entrega.zip en la raíz del repositorio.

    Codigo/                                   git archive de la rama (sin target/ ni archivos ignorados)
    Diseno_Arquitectonico/Diseno_Arquitectonico.pdf
    Diseno_Arquitectonico/diagramas/*.png

Todo sale de la misma rama, no de la copia de trabajo. El PDF se genera a partir de docs/arquitectura.md
(Markdown -> HTML con marked -> paginación con Paged.js -> PDF con Chrome y puppeteer, ver scripts/pdf.js);
requiere Node.js con puppeteer en NODE_PATH y conexión para descargar marked y Paged.js desde el CDN.

Uso: python scripts/empaquetar.py [rama]          (por defecto main; para ensayar: andre o HEAD)
     python scripts/empaquetar.py --pdf <archivo.md>   (solo genera el PDF de ese archivo, para revisarlo)
"""
import io
import json
import os
import shutil
import subprocess
import sys
import tarfile
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "Taller4_Entrega.zip"
PDF_NAME = "Diseno_Arquitectonico.pdf"
# node_modules con puppeteer; se puede indicar otro con la variable NODE_PATH
DEFAULT_NODE_PATH = Path(os.environ.get("TALLER4_NODE_MODULES", ROOT / "tools" / "node_modules"))
CHROMES = [os.environ.get("CHROME", ""),
           r"C:\Program Files\Google\Chrome\Application\chrome.exe",
           r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
           "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
           "google-chrome", "chromium"]

# La primera parte del Markdown (el título y los párrafos antes del primer "## ") se convierte en la portada:
# párrafo 1 = subtítulo, 2 = materia y universidad, 3 = "Integrantes: A, B", 4 = ciudad y fecha.
PAGE = r"""<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>__TITLE__</title>
<script>window.PagedConfig = { auto: false };</script>
<script src="https://cdn.jsdelivr.net/npm/marked@12.0.2/marked.min.js"></script>
<script src="https://unpkg.com/pagedjs@0.4.3/dist/paged.polyfill.js"></script>
<style>
  @page {
    size: A4;
    margin: 25mm 22mm 22mm 25mm;
    @top-right { content: "Diseño arquitectónico"; font: 8pt Arial, sans-serif; color: #777; }
    @bottom-right { content: counter(page); font: 9pt Arial, sans-serif; }
  }
  @page portada { @top-right { content: none; } @bottom-right { content: none; } }
  @page indice { @top-right { content: none; } }

  body { font: 10.5pt/1.5 Arial, Helvetica, sans-serif; color: #000; }
  p { margin: 0 0 7pt; text-align: justify; hyphens: auto; }
  li { text-align: justify; margin-bottom: 3pt; }
  ul { margin: 0 0 8pt; padding-left: 16pt; }
  h2 { font-size: 14pt; margin: 20pt 0 8pt; break-after: avoid; }
  h3 { font-size: 11.5pt; margin: 14pt 0 6pt; break-after: avoid; }
  h2.nueva-pagina, div.nueva-pagina { break-before: page; margin-top: 0; }
  div.mantener { break-inside: avoid; }
  div.mantener > h2:first-child { margin-top: 20pt; }
  div.nueva-pagina > h2:first-child { margin-top: 0; }
  code { font: 9pt Consolas, "Courier New", monospace; }

  .portada { page: portada; height: 247mm; display: flex; flex-direction: column; text-align: center; }
  .portada .institucion { font-size: 13pt; font-weight: bold; letter-spacing: .5pt; }
  .portada .facultad { font-size: 11pt; margin-top: 4pt; }
  .portada .centro { margin-top: 70mm; }
  .portada h1 { font-size: 20pt; line-height: 1.3; margin: 0 0 14pt; }
  .portada .subtitulo { font-size: 12.5pt; text-align: center; }
  .portada .integrantes { margin-top: auto; font-size: 11.5pt; }
  .portada .integrantes b { display: block; margin-bottom: 4pt; }
  .portada .fecha { margin-top: 26mm; font-size: 11pt; }

  .indice { page: indice; break-before: page; break-after: page; }
  .indice h2 { margin-top: 0; }
  .indice ol { list-style: none; padding: 0; margin: 0; }
  .indice li { margin: 0 0 4pt; text-align: left; }
  .indice li.n3 { padding-left: 16pt; font-size: 10pt; }
  .indice a { color: #000; text-decoration: none; display: flex; }
  .indice a .t { flex: 1; overflow: hidden; white-space: nowrap; }
  .indice a .t::after { content: " ........................................................................................................................................"; color: #888; }
  .indice a::after { content: target-counter(attr(href), page); padding-left: 4pt; }

  figure { margin: 10pt 0 12pt; break-inside: avoid; text-align: center; }
  figure img { max-width: 100%; max-height: 150mm; }
  figcaption, p.leyenda { font-size: 9.5pt; text-align: center; margin: 6pt 0; }
  p.leyenda { margin: 12pt 0 4pt; break-after: avoid; }
  figcaption b, p.leyenda b { font-weight: bold; }

  table { border-collapse: collapse; width: 100%; margin: 0 0 12pt; font-size: 8.5pt; line-height: 1.35; }
  th, td { border: 1px solid #8c8c8c; padding: 3.5pt 5pt; vertical-align: top; text-align: left; }
  th { background: #e6e6e6; font-weight: bold; }
  tr { break-inside: avoid; }
  div.corta { break-inside: avoid; }
  thead { break-after: avoid; }

  /* Diagramas anchos: página completa, girados 90 grados */
  figure.girada { break-before: page; break-after: page; height: 238mm; position: relative; margin: 0; }
  figure.girada .giro { position: absolute; width: 232mm; height: 160mm; left: -34mm; top: 39mm;
                        transform: rotate(-90deg); display: flex; flex-direction: column; justify-content: center; }
  figure.girada img { max-width: 100%; max-height: 145mm; margin: 0 auto; }
  td code { font-size: 8pt; }
</style>
</head>
<body>
<main id="doc">No se pudo cargar marked desde el CDN.</main>
<script>
  const md = __MARKDOWN__;
  const GIRADAS = [];   // diagramas que se imprimen girados a página completa (por ejemplo "04-vista-logica")
  const doc = document.getElementById("doc");
  doc.innerHTML = marked.parse(md);

  // Portada: título y párrafos anteriores al primer h2
  const h1 = doc.querySelector("h1");
  const lineas = [];
  for (let n = h1.nextElementSibling; n && n.tagName !== "H2"; ) {
    const sig = n.nextElementSibling; lineas.push(n.textContent.trim()); n.remove(); n = sig;
  }
  const [subtitulo, materia, integrantes, fecha] = lineas;
  const nombres = (integrantes || "").replace(/^Integrantes:\s*/, "").split(/,\s*/);
  const portada = document.createElement("section");
  portada.className = "portada";
  portada.innerHTML =
    '<div class="institucion">PONTIFICIA UNIVERSIDAD JAVERIANA</div>' +
    '<div class="facultad">Facultad de Ingeniería · Departamento de Ingeniería de Sistemas</div>' +
    '<div class="facultad">' + (materia || "").split("·")[0].trim() + '</div>' +
    '<div class="centro"><h1>' + h1.textContent + '</h1><p class="subtitulo">' + subtitulo + '</p></div>' +
    '<div class="integrantes"><b>Integrantes</b>' + nombres.join("<br>") + '</div>' +
    '<div class="fecha">' + fecha + '</div>';
  h1.replaceWith(portada);

  // Figuras: la imagen sola en su párrafo pasa a <figure> con el texto alternativo como leyenda
  doc.querySelectorAll("p > img:only-child").forEach((img) => {
    const fig = document.createElement("figure");
    const cap = document.createElement("figcaption");
    cap.innerHTML = img.alt.replace(/^(Figura \d+\.)/, "<b>$1</b>");
    img.parentElement.replaceWith(fig);
    if (GIRADAS.some((g) => img.getAttribute("src").includes(g))) {
      fig.className = "girada";
      const giro = document.createElement("div");
      giro.className = "giro";
      giro.append(img, cap);
      fig.append(giro);
    } else {
      fig.append(img, cap);
    }
  });
  // Las tablas cortas no se parten entre páginas
  // Las tablas que miden menos de unos 9 cm al ancho de la página no se parten
  doc.style.width = "163mm";
  doc.querySelectorAll("table").forEach((t) => {
    if (t.getBoundingClientRect().height >= 340) return;
    // Tabla corta: se agrupa con su leyenda para que ninguna de las dos quede sola
    const grupo = document.createElement("div");
    grupo.className = "corta";
    const leyenda = t.previousElementSibling;
    t.before(grupo);
    if (leyenda && leyenda.classList.contains("leyenda")) grupo.append(leyenda);
    grupo.append(t);
  });
  doc.style.width = "";
  // Leyendas de tabla: párrafos que empiezan con "Tabla N."
  doc.querySelectorAll("p").forEach((p) => {
    if (/^Tabla \d+\./.test(p.textContent)) {
      p.className = "leyenda";
      p.innerHTML = p.innerHTML.replace(/^(Tabla \d+\.)/, "<b>$1</b>");
    }
  });
  // IDs, códigos y prioridades cortas no se parten en dos líneas
  doc.querySelectorAll("td, th").forEach((c) => { if (c.textContent.trim().length <= 12) c.style.whiteSpace = "nowrap"; });

  // Tabla de contenido con números de página (Paged.js resuelve target-counter)
  const titulos = [...doc.querySelectorAll("h2, h3")];
  titulos.forEach((h, i) => { h.id = "s" + i; });
  const indice = document.createElement("section");
  indice.className = "indice";
  indice.innerHTML = "<h2>Tabla de contenido</h2><ol>" + titulos.map((h) =>
    '<li class="n' + h.tagName[1] + '"><a href="#' + h.id + '"><span class="t">' + h.textContent + "</span></a></li>").join("") + "</ol>";
  portada.after(indice);
  titulos.filter((h) => h.tagName === "H2")[0].classList.add("nueva-pagina");
  // Cada título queda en la misma página que el bloque que lo sigue (párrafo, figura o tabla corta)
  titulos.forEach((h) => {
    if (h.parentElement.classList.contains("mantener")) return;
    const grupo = document.createElement("div");
    grupo.className = "mantener" + (h.classList.contains("nueva-pagina") ? " nueva-pagina" : "");
    h.before(grupo);
    grupo.append(h);
    let sig = grupo.nextElementSibling;
    if (sig && sig.tagName === "H3") { grupo.append(sig); sig = grupo.nextElementSibling; }
    if (sig && (sig.tagName === "P" || sig.tagName === "FIGURE" || sig.classList.contains("corta"))) grupo.append(sig);
  });
  [].forEach((t) => {
    const h = titulos.find((x) => x.textContent.startsWith(t)); if (h) h.classList.add("nueva-pagina");
  });

  window.PagedPolyfill.preview().then(() => { document.title = "listo"; });
</script>
</body>
</html>
"""


def git(*args):
    return subprocess.run(["git", "-C", str(ROOT), *args], check=True, capture_output=True).stdout


def chrome():
    for c in CHROMES:
        if c and (Path(c).exists() or shutil.which(c)):
            return c
    sys.exit("No encontré Chrome; indica la ruta con la variable CHROME.")


def to_pdf(md_file: Path, pdf: Path) -> Path:
    page = md_file.with_suffix(".html")
    markdown = json.dumps(md_file.read_text(encoding="utf-8"), ensure_ascii=False).replace("</", "<\\/")
    page.write_text(PAGE.replace("__TITLE__", "Diseño arquitectónico").replace("__MARKDOWN__", markdown),
                    encoding="utf-8")
    env = dict(os.environ, CHROME=chrome())
    if "NODE_PATH" not in env and DEFAULT_NODE_PATH.exists():
        env["NODE_PATH"] = str(DEFAULT_NODE_PATH)
    r = subprocess.run(["node", str(ROOT / "scripts" / "pdf.js"), str(page), str(pdf)], env=env,
                       capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit("No se pudo generar el PDF: " + (r.stderr.strip() or r.stdout.strip())
                 + "\nInstala puppeteer (npm i @mermaid-js/mermaid-cli) e indica su node_modules en NODE_PATH.")
    page.unlink()
    if not pdf.exists() or pdf.stat().st_size < 50_000:
        sys.exit(f"El PDF de {md_file.name} no se generó bien (¿sin conexión al CDN?).")
    return pdf


def main():
    if len(sys.argv) > 2 and sys.argv[1] == "--pdf":
        md = Path(sys.argv[2]).resolve()
        pdf = to_pdf(md, md.with_name(PDF_NAME))
        print(f"{pdf}: {pdf.stat().st_size // 1024} KB")
        return
    ref = sys.argv[1] if len(sys.argv) > 1 else "main"
    commit = git("rev-parse", "--short", ref).decode().strip()
    print(f"Empaquetando {ref} ({commit})")
    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        archive = tarfile.open(fileobj=io.BytesIO(git("archive", "--format=tar", ref, "docs")))
        archive.extractall(tmp, **({"filter": "data"} if hasattr(tarfile, "data_filter") else {}))
        docs = tmp / "docs"
        pdf = to_pdf(docs / "arquitectura.md", docs / PDF_NAME)
        print(f"  {PDF_NAME}: {pdf.stat().st_size // 1024} KB")

        code = zipfile.ZipFile(io.BytesIO(git("archive", "--format=zip", "--prefix=Codigo/", ref)))
        with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
            for info in code.infolist():
                z.writestr(info, code.read(info))
            design = [pdf] + sorted((docs / "diagramas").glob("*.png"))
            for f in design:
                z.write(f, "Diseno_Arquitectonico/" + f.relative_to(docs).as_posix())
        n_code = sum(1 for i in code.infolist() if not i.is_dir())
    print(f"{OUT.name}: {OUT.stat().st_size // 1024} KB | Codigo/ {n_code} archivos | "
          f"Diseno_Arquitectonico/ {len(design)} archivos")
    print("Revisa el PDF antes de entregar.")


if __name__ == "__main__":
    main()
