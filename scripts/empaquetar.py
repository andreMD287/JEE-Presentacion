"""Genera Taller4_Entrega.zip en la raíz del repositorio.

    Codigo/                  git archive de la rama (sin target/ ni archivos ignorados)
    Diseno_Arquitectonico/   arquitectura y api en .md y .pdf, diagramas y evidencias

Todo sale de la misma rama, no de la copia de trabajo. Los PDF se generan con Chrome headless
(Markdown -> HTML con marked -> PDF); requiere conexión para descargar marked desde el CDN.

Uso: python scripts/empaquetar.py [rama]        (por defecto main; para ensayar: andre o HEAD)
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
REF = sys.argv[1] if len(sys.argv) > 1 else "main"
OUT = ROOT / "Taller4_Entrega.zip"
DOCS = {"arquitectura": "Diseño arquitectónico", "api": "Contratos e interfaces"}
CHROMES = [os.environ.get("CHROME", ""),
           r"C:\Program Files\Google\Chrome\Application\chrome.exe",
           r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
           "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
           "google-chrome", "chromium"]

PAGE = """<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>{title}</title>
<script src="https://cdn.jsdelivr.net/npm/marked@12.0.2/marked.min.js"></script>
<style>
  @page {{ size: A4; margin: 14mm 13mm; }}
  body {{ font: 10pt/1.45 "Segoe UI", Arial, sans-serif; color: #1a1a1a; margin: 0; }}
  h1 {{ font-size: 19pt; margin: 0 0 6pt; }}
  h2 {{ font-size: 14pt; margin: 18pt 0 6pt; border-bottom: 1px solid #ccc; padding-bottom: 2pt; break-after: avoid; }}
  h3 {{ font-size: 11.5pt; margin: 12pt 0 4pt; break-after: avoid; }}
  table {{ border-collapse: collapse; width: 100%; margin: 6pt 0 10pt; font-size: 8.3pt; }}
  th, td {{ border: 1px solid #bbb; padding: 3pt 4pt; vertical-align: top; text-align: left; }}
  th {{ background: #f0f0f0; }}
  tr {{ break-inside: avoid; }}
  code {{ font: 8.6pt Consolas, monospace; background: #f4f4f4; padding: 0 2px; }}
  pre {{ background: #f6f6f6; border: 1px solid #ddd; padding: 6pt; white-space: pre-wrap; font-size: 8.3pt; }}
  pre code {{ background: none; padding: 0; }}
  img {{ display: block; max-width: 100%; max-height: 235mm; margin: 8pt auto 2pt; break-inside: avoid; }}
  blockquote {{ border-left: 3px solid #bbb; margin: 6pt 0; padding: 0 8pt; color: #444; }}
</style>
</head>
<body>
<main id="doc">No se pudo cargar marked desde el CDN.</main>
<script>
  const md = {markdown};
  document.getElementById("doc").innerHTML = marked.parse(md);
  // IDs, fechas y prioridades cortas no se parten en dos líneas
  document.querySelectorAll("td, th").forEach(c => {{
    if (c.textContent.trim().length <= 12) c.style.whiteSpace = "nowrap";
  }});
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


def to_pdf(md_file: Path, title: str) -> Path:
    page = md_file.with_suffix(".html")
    markdown = json.dumps(md_file.read_text(encoding="utf-8"), ensure_ascii=False).replace("</", "<\\/")
    page.write_text(PAGE.format(title=title, markdown=markdown), encoding="utf-8")
    pdf = md_file.with_suffix(".pdf")
    subprocess.run([chrome(), "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                    "--virtual-time-budget=25000", f"--print-to-pdf={pdf}", page.as_uri()],
                   check=True, capture_output=True)
    page.unlink()
    if not pdf.exists() or pdf.stat().st_size < 10_000:
        sys.exit(f"El PDF de {md_file.name} no se generó bien (¿sin conexión al CDN?).")
    return pdf


def main():
    commit = git("rev-parse", "--short", REF).decode().strip()
    print(f"Empaquetando {REF} ({commit})")
    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        archive = tarfile.open(fileobj=io.BytesIO(git("archive", "--format=tar", REF, "docs")))
        archive.extractall(tmp, **({"filter": "data"} if hasattr(tarfile, "data_filter") else {}))
        docs = tmp / "docs"
        for name, title in DOCS.items():
            pdf = to_pdf(docs / f"{name}.md", title)
            print(f"  {pdf.name}: {pdf.stat().st_size // 1024} KB")

        code = zipfile.ZipFile(io.BytesIO(git("archive", "--format=zip", "--prefix=Codigo/", REF)))
        with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
            for info in code.infolist():
                z.writestr(info, code.read(info))
            design = [docs / f"{n}{ext}" for n in DOCS for ext in (".md", ".pdf")]
            design += sorted(p for d in ("diagramas", "evidencias") for p in (docs / d).rglob("*") if p.is_file())
            for f in design:
                z.write(f, "Diseno_Arquitectonico/" + f.relative_to(docs).as_posix())
        n_code = sum(1 for i in code.infolist() if not i.is_dir())
    print(f"{OUT.name}: {OUT.stat().st_size // 1024} KB | Codigo/ {n_code} archivos | "
          f"Diseno_Arquitectonico/ {len(design)} archivos")
    print("Revisa los PDF antes de entregar: abre el zip o vuelve a generarlos con este script.")


if __name__ == "__main__":
    main()
