// Imprime una página HTML a PDF esperando a que Paged.js termine la paginación (la página pone title = "listo").
// Uso: node scripts/pdf.js <entrada.html> <salida.pdf>
// Requiere puppeteer (viene con @mermaid-js/mermaid-cli; ver tools/diagramas/README.md): NODE_PATH=<node_modules>
const path = require('path');
const { pathToFileURL } = require('url');
const puppeteer = require('puppeteer');

const [entrada, salida] = process.argv.slice(2);
const chrome = process.env.CHROME || 'C:/Program Files/Google/Chrome/Application/chrome.exe';

(async () => {
  const browser = await puppeteer.launch({ executablePath: chrome, args: ['--no-sandbox'] });
  try {
    const page = await browser.newPage();
    await page.goto(pathToFileURL(path.resolve(entrada)).href, { waitUntil: 'networkidle0', timeout: 120000 });
    await page.waitForFunction(() => document.title === 'listo', { timeout: 120000 });
    await page.pdf({ path: salida, preferCSSPageSize: true, printBackground: true });
  } finally {
    await browser.close();
  }
})().catch((e) => { console.error(e.message); process.exit(1); });
