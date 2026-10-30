// Exporta los diagramas a PNG (escala 2) en docs/diagramas/:
//   - build/*.svg (generados por generar.py) -> PNG con Chrome (puppeteer)
//   - *.mmd (secuencias en Mermaid)          -> PNG con mermaid-cli
// Requiere @mermaid-js/mermaid-cli (trae puppeteer). Ver README.md.
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const puppeteer = require('puppeteer');

const dir = __dirname;
const build = path.join(dir, 'build');
const out = path.resolve(dir, '../../docs/diagramas');
const chrome = process.env.CHROME || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
// mermaid-cli no exporta su cli.js como submódulo: se busca en NODE_PATH
const mmdc = process.env.MMDC_CLI
  || path.join(process.env.NODE_PATH || 'node_modules', '@mermaid-js/mermaid-cli/src/cli.js');

(async () => {
  const cfg = path.join(dir, '.puppeteer.json');
  fs.writeFileSync(cfg, JSON.stringify({ executablePath: chrome, args: ['--no-sandbox'] }));
  try {
    for (const f of fs.readdirSync(dir).filter((f) => f.endsWith('.mmd'))) {
      execFileSync(process.execPath, [mmdc, '-p', cfg, '-t', 'neutral', '-b', 'white', '-s', '2',
        '-i', path.join(dir, f), '-o', path.join(out, f.replace(/\.mmd$/, '.png'))], { stdio: 'inherit' });
    }
  } finally {
    fs.unlinkSync(cfg);
  }

  const browser = await puppeteer.launch({ executablePath: chrome, args: ['--no-sandbox'] });
  const page = await browser.newPage();
  for (const f of fs.readdirSync(build).filter((f) => f.endsWith('.svg'))) {
    const svg = fs.readFileSync(path.join(build, f), 'utf8');
    const w = Number(/width="(\d+)"/.exec(svg)[1]);
    const h = Number(/height="(\d+)"/.exec(svg)[1]);
    await page.setViewport({ width: w, height: h, deviceScaleFactor: 2 });
    await page.setContent(`<html><body style="margin:0">${svg}</body></html>`);
    await page.screenshot({ path: path.join(out, f.replace(/\.svg$/, '.png')), clip: { x: 0, y: 0, width: w, height: h } });
    console.log('png', f);
  }
  await browser.close();
})();
