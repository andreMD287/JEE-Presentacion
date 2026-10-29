// Exporta los diagramas de esta carpeta a PNG (escala 2):
//   - *.svg (generados por generar.py)  -> *.png con Chrome (puppeteer)
//   - *.mmd (secuencias en Mermaid)     -> *.svg y *.png con mermaid-cli
// Requiere @mermaid-js/mermaid-cli (trae puppeteer). Ver README.md.
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const puppeteer = require('puppeteer');

const dir = __dirname;
const chrome = process.env.CHROME || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
// mermaid-cli no exporta su cli.js como submódulo: se busca en NODE_PATH
const mmdc = process.env.MMDC_CLI
  || path.join(process.env.NODE_PATH || 'node_modules', '@mermaid-js/mermaid-cli/src/cli.js');

(async () => {
  const cfg = path.join(dir, '.puppeteer.json');
  fs.writeFileSync(cfg, JSON.stringify({ executablePath: chrome, args: ['--no-sandbox'] }));
  try {
    for (const f of fs.readdirSync(dir).filter((f) => f.endsWith('.mmd'))) {
      for (const ext of ['svg', 'png']) {
        const out = path.join(dir, f.replace(/\.mmd$/, '.' + ext));
        execFileSync(process.execPath, [mmdc, '-p', cfg, '-t', 'neutral', '-b', 'white', '-s', '2',
          '-i', path.join(dir, f), '-o', out], { stdio: 'inherit' });
      }
    }
  } finally {
    fs.unlinkSync(cfg);
  }

  const browser = await puppeteer.launch({ executablePath: chrome, args: ['--no-sandbox'] });
  const page = await browser.newPage();
  const generated = fs.readdirSync(dir)
    .filter((f) => f.endsWith('.svg') && !fs.existsSync(path.join(dir, f.replace(/\.svg$/, '.mmd'))));
  for (const f of generated) {
    const svg = fs.readFileSync(path.join(dir, f), 'utf8');
    const w = Number(/width="(\d+)"/.exec(svg)[1]);
    const h = Number(/height="(\d+)"/.exec(svg)[1]);
    await page.setViewport({ width: w, height: h, deviceScaleFactor: 2 });
    await page.setContent(`<html><body style="margin:0">${svg}</body></html>`);
    await page.screenshot({ path: path.join(dir, f.replace(/\.svg$/, '.png')), clip: { x: 0, y: 0, width: w, height: h } });
    console.log('png', f);
  }
  await browser.close();
})();
