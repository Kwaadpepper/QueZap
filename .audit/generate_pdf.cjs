const fs = require('fs');
const path = require('path');
const { chromium } = require('/home/jeremy/projets/svelte-test/node_modules/playwright-core');

const mdPath = path.join(__dirname, 'RAPPORT_AUDIT_GLOBAL_QUEZAP.md');
const pdfPath = path.join(__dirname, 'RAPPORT_AUDIT_GLOBAL_QUEZAP.pdf');
const mdContent = fs.readFileSync(mdPath, 'utf-8');

const htmlTemplate = `<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Rapport d'Audit Global — QueZap</title>
  <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
  <style>
    @page {
      size: A4;
      margin: 18mm 16mm 18mm 16mm;
      @bottom-right {
        content: counter(page) " / " counter(pages);
        font-size: 9pt;
        color: #64748b;
      }
    }
    
    * {
      box-sizing: border-box;
    }
    
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      color: #1e293b;
      line-height: 1.55;
      font-size: 10.5pt;
      background: #ffffff;
      margin: 0;
      padding: 0;
    }
    
    h1 {
      color: #065f46;
      font-size: 20pt;
      border-bottom: 2.5px solid #10b981;
      padding-bottom: 6px;
      margin-top: 0;
      margin-bottom: 14px;
      page-break-before: always;
    }
    
    h1:first-of-type {
      page-break-before: avoid;
    }
    
    h2 {
      color: #0f766e;
      font-size: 14pt;
      border-bottom: 1px solid #cbd5e1;
      padding-bottom: 4px;
      margin-top: 22px;
      margin-bottom: 12px;
      page-break-after: avoid;
    }
    
    h3 {
      color: #1e293b;
      font-size: 12pt;
      margin-top: 16px;
      margin-bottom: 8px;
      page-break-after: avoid;
    }

    h4 {
      color: #334155;
      font-size: 11pt;
      margin-top: 14px;
      margin-bottom: 6px;
      page-break-after: avoid;
    }
    
    p {
      margin-top: 0;
      margin-bottom: 10px;
      text-align: justify;
    }
    
    ul, ol {
      margin-top: 0;
      margin-bottom: 12px;
      padding-left: 20px;
    }
    
    li {
      margin-bottom: 4px;
    }
    
    hr {
      border: 0;
      height: 1px;
      background: #e2e8f0;
      margin: 20px 0;
    }
    
    table {
      width: 100%;
      border-collapse: collapse;
      margin: 14px 0 18px 0;
      font-size: 9.5pt;
      page-break-inside: avoid;
    }
    
    th {
      background-color: #f1f5f9;
      color: #0f172a;
      font-weight: 600;
      text-align: left;
      padding: 7px 10px;
      border: 1px solid #cbd5e1;
    }
    
    td {
      padding: 6px 10px;
      border: 1px solid #e2e8f0;
      vertical-align: top;
    }
    
    tr:nth-child(even) {
      background-color: #f8fafc;
    }
    
    code {
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-size: 9pt;
      background: #f1f5f9;
      color: #0f766e;
      padding: 1.5px 4px;
      border-radius: 3px;
      border: 1px solid #e2e8f0;
    }
    
    pre {
      background: #0f172a;
      color: #f8fafc;
      padding: 12px;
      border-radius: 6px;
      font-size: 8.5pt;
      overflow-x: auto;
      page-break-inside: avoid;
      margin: 12px 0;
    }
    
    pre code {
      background: transparent;
      color: inherit;
      padding: 0;
      border: none;
    }
    
    blockquote {
      border-left: 4px solid #10b981;
      margin: 12px 0;
      padding: 6px 14px;
      background: #f0fdf4;
      color: #166534;
      font-style: italic;
    }

    .badge-p0 {
      background: #fee2e2;
      color: #991b1b;
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: bold;
    }

    .badge-p1 {
      background: #fef3c7;
      color: #92400e;
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: bold;
    }

    .badge-p2 {
      background: #dcfce7;
      color: #166534;
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: bold;
    }

    .mermaid {
      text-align: center;
      margin: 16px 0;
      page-break-inside: avoid;
    }
  </style>
</head>
<body>
  <div id="content"></div>
  <script>
    mermaid.initialize({ startOnLoad: false, theme: 'neutral' });
    const rawMarkdown = ${JSON.stringify(mdContent)};
    document.getElementById('content').innerHTML = marked.parse(rawMarkdown);
    
    // Convert mermaid code blocks
    document.querySelectorAll('pre code.language-mermaid').forEach((el, index) => {
      const parent = el.parentElement;
      const graphDef = el.textContent;
      const mermaidDiv = document.createElement('div');
      mermaidDiv.className = 'mermaid';
      mermaidDiv.textContent = graphDef;
      parent.replaceWith(mermaidDiv);
    });
    mermaid.run();
  </script>
</body>
</html>`;

(async () => {
  console.log('Lancement du navigateur headless Chromium...');
  const browser = await chromium.launch({
    executablePath: '/home/jeremy/.cache/ms-playwright/chromium-1248/chrome-linux64/chrome',
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });

  const page = await browser.newPage();
  await page.setContent(htmlTemplate, { waitUntil: 'networkidle' });
  
  // Attendre le rendu Mermaid et les polices
  await page.waitForTimeout(2500);

  console.log('Génération du document PDF...');
  await page.pdf({
    path: pdfPath,
    format: 'A4',
    printBackground: true,
    displayHeaderFooter: true,
    headerTemplate: `
      <div style="font-size: 8pt; color: #94a3b8; width: 100%; padding: 0 16mm; display: flex; justify-content: space-between; font-family: sans-serif;">
        <span>QueZap — Rapport d'Audit & Plan d'Action</span>
        <span>Version 1.0.0</span>
      </div>
    `,
    footerTemplate: `
      <div style="font-size: 8pt; color: #94a3b8; width: 100%; padding: 0 16mm; display: flex; justify-content: space-between; font-family: sans-serif;">
        <span>Confidentiel & Stratégique</span>
        <span>Page <span class="pageNumber"></span> sur <span class="totalPages"></span></span>
      </div>
    `,
    margin: {
      top: '18mm',
      bottom: '18mm',
      left: '16mm',
      right: '16mm'
    }
  });

  await browser.close();
  console.log('Succès ! PDF généré avec succès dans :', pdfPath);
})();
