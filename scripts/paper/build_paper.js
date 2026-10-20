// Builds paper/paper.docx (IEEE-style, two columns, A4) from paper/paper.md. Usage: node scripts/paper/build_paper.js
// Markdown subset: %meta lines, "## HEADING" (numbered roman; "!" prefix = unnumbered), "### Sub", paragraphs, "- " bullets,
// "Table: caption" / "Table*: caption" (wide) + pipe table, "![caption](path){wide|full|fullsmall}", **bold**, *italic*, `code`.
const fs = require('fs');
const path = require('path');
const sizeOf = (f) => { const b = fs.readFileSync(f); return { w: b.readUInt32BE(16), h: b.readUInt32BE(20) }; };
const {
  Document, Packer, Paragraph, TextRun, ImageRun, Table, TableRow, TableCell, WidthType, AlignmentType, BorderStyle,
  ShadingType, SectionType, Footer, PageNumber, LevelFormat, VerticalAlign, TableLayoutType,
} = require('docx');

const ROOT = path.resolve(__dirname, '..', '..');
const SRC = path.join(ROOT, 'paper', 'paper.md');
const OUT = path.join(ROOT, 'paper', 'paper.docx');
const FONT = 'Times New Roman';
const PAGE_W = 11906, PAGE_H = 16838, MARGIN = 1000, GAP = 360;
const CONTENT_W = PAGE_W - 2 * MARGIN;             // 9906
const COL_W = Math.floor((CONTENT_W - GAP) / 2);    // 4773
const EMU_PER_DXA = 635;
const roman = ['', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X', 'XI', 'XII'];
const romanTable = (n) => roman[n] || String(n);

const lines = fs.readFileSync(SRC, 'utf8').split('\n');
const meta = {};
const body = [];
for (const l of lines) {
  const m = l.match(/^%(\w+)\s+(.*)$/);
  if (m) meta[m[1]] = m[2]; else body.push(l);
}

// ---- inline formatting ----
function runs(text, base = {}) {
  const out = [];
  const re = /(\*\*[^*]+\*\*|\*[^*]+\*|`[^`]+`)/g;
  let last = 0, m;
  while ((m = re.exec(text)) !== null) {
    if (m.index > last) out.push(new TextRun({ text: text.slice(last, m.index), font: FONT, ...base }));
    const t = m[0];
    if (t.startsWith('**')) out.push(new TextRun({ text: t.slice(2, -2), bold: true, font: FONT, ...base }));
    else if (t.startsWith('`')) out.push(new TextRun({ text: t.slice(1, -1), font: 'Courier New', ...base, size: Math.max(14, (base.size || 20) - 2) }));
    else out.push(new TextRun({ text: t.slice(1, -1), italics: true, font: FONT, ...base }));
    last = m.index + t.length;
  }
  if (last < text.length) out.push(new TextRun({ text: text.slice(last), font: FONT, ...base }));
  return out;
}
const para = (text, opts = {}) => new Paragraph({
  alignment: opts.align || AlignmentType.JUSTIFIED, spacing: { after: opts.after ?? 80, before: opts.before ?? 0, line: opts.line ?? 240 },
  indent: opts.indent, keepNext: opts.keepNext, keepLines: opts.keepLines,
  children: runs(text, { size: opts.size || 20, ...(opts.run || {}) }),
});

// ---- block parsing ----
const blocks = []; // {type, ...}
let secNo = 0, subNo = 0, tabNo = 0, figNo = 0, inAppendix = false;
let i = 0;
while (i < body.length) {
  const l = body[i];
  if (!l.trim()) { i++; continue; }
  let m;
  if ((m = l.match(/^## (.*)$/))) {
    let t = m[1]; let numbered = true;
    if (t.startsWith('!')) { numbered = false; t = t.slice(1); }
    if (numbered) { secNo++; subNo = 0; }
    if (/^APPENDIX/.test(t)) inAppendix = true;
    blocks.push({ type: 'h1', text: numbered ? `${romanTable(secNo)}. ${t}` : t, appendix: /^APPENDIX/.test(t) });
    i++; continue;
  }
  if ((m = l.match(/^### (.*)$/))) { subNo++; blocks.push({ type: 'h2', text: `${String.fromCharCode(64 + subNo)}. ${m[1]}` }); i++; continue; }
  if ((m = l.match(/^Table(\*?): (.*)$/))) {
    tabNo++;
    const rows = [];
    i++;
    while (i < body.length && body[i].startsWith('|')) {
      const cells = body[i].trim().replace(/^\||\|$/g, '').split('|').map((c) => c.trim());
      if (!cells.every((c) => /^:?-+:?$/.test(c))) rows.push(cells);
      i++;
    }
    blocks.push({ type: 'table', wide: m[1] === '*', caption: m[2], no: tabNo, rows });
    continue;
  }
  if ((m = l.match(/^!\[(.*)\]\((.*)\)\{(\w+)\}$/))) {
    figNo++;
    blocks.push({ type: 'fig', caption: m[1], file: path.resolve(path.dirname(SRC), m[2]), mode: m[3], no: figNo });
    i++; continue;
  }
  if (l.startsWith('- ')) {
    const items = [];
    while (i < body.length && body[i].startsWith('- ')) { items.push(body[i].slice(2)); i++; }
    blocks.push({ type: 'bullets', items });
    continue;
  }
  if (/^\[\d+\] /.test(l)) { blocks.push({ type: 'ref', text: l }); i++; continue; }
  // paragraph: gather following non-empty, non-special lines
  let text = l; i++;
  while (i < body.length && body[i].trim() && !/^(#|Table|!\[|- |\[\d+\] )/.test(body[i])) { text += ' ' + body[i]; i++; }
  blocks.push({ type: 'p', text });
}

// ---- docx element builders ----
const thin = { style: BorderStyle.SINGLE, size: 4, color: '000000' };
const none = { style: BorderStyle.NONE, size: 0, color: 'FFFFFF' };

function tableEl(b) {
  const width = b.wide ? CONTENT_W : COL_W;
  const ncol = b.rows[0].length;
  const need = [];
  for (let c = 0; c < ncol; c++) {
    let mx = 0;
    b.rows.forEach((r, ri) => {
      const t = r[c] || '';
      const longestWord = Math.max(...t.split(/\s+/).map((w) => w.length));
      mx = Math.max(mx, ri === 0 ? longestWord + 2 : Math.min(t.length, 44));
    });
    need.push(c === 0 ? Math.max(mx + 3, 8) : Math.max(mx, 5));
  }
  const tot = need.reduce((x, y) => x + y, 0);
  let widths = need.map((n) => Math.floor(width * n / tot));
  widths[widths.length - 1] += width - widths.reduce((x, y) => x + y, 0);
  const sz = b.wide ? 17 : 16;
  const rows = b.rows.map((r, ri) => new TableRow({
    tableHeader: ri === 0, cantSplit: true,
    children: r.map((c, ci) => new TableCell({
      width: { size: widths[ci], type: WidthType.DXA },
      verticalAlign: VerticalAlign.CENTER,
      margins: { top: 20, bottom: 20, left: 60, right: 60 },
      shading: ri === 0 ? { type: ShadingType.CLEAR, fill: 'E6E6E6', color: 'auto' } : undefined,
      borders: { top: ri === 0 ? thin : none, bottom: ri === 0 || ri === b.rows.length - 1 ? thin : { style: BorderStyle.SINGLE, size: 2, color: 'BBBBBB' }, left: none, right: none },
      children: [new Paragraph({ alignment: ci === 0 || (ncol <= 4 && b.wide) ? AlignmentType.LEFT : AlignmentType.CENTER, spacing: { after: 0, line: 220 },
        children: runs(c, { size: sz, bold: ri === 0 }) })],
    })),
  }));
  const cap = new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 120, after: 60 }, keepNext: true,
    children: [new TextRun({ text: `TABLE ${romanTable(b.no)}`, font: FONT, size: 16 }), new TextRun({ text: '', break: 1 }),
      ...runs(b.caption.toUpperCase().replace(/`/g, ''), { size: 16 })] });
  return [cap, new Table({ width: { size: width, type: WidthType.DXA }, columnWidths: widths, layout: TableLayoutType.FIXED, rows }),
    new Paragraph({ spacing: { after: 100 }, children: [] })];
}

function figEl(b) {
  const dim = sizeOf(b.file);
  let wDxa;
  if (b.mode === 'wide') wDxa = CONTENT_W * 0.86;
  else if (b.mode === 'col') wDxa = COL_W;
  else if (b.mode === 'cont') wDxa = 3900;
  else wDxa = 8600; // appendix figure
  let hDxa = wDxa * dim.h / dim.w;
  const maxH = b.mode === 'full' ? 12500 : 12000;
  if (hDxa > maxH) { hDxa = maxH; wDxa = hDxa * dim.w / dim.h; }
  const px = (d) => Math.round(d / 15); // DXA -> px at 96 dpi (1px = 15 dxa)
  return [
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 80, after: 40 }, keepNext: true, children: [
      new ImageRun({ type: 'png', data: fs.readFileSync(b.file), transformation: { width: px(wDxa), height: px(hDxa) },
        altText: { title: `Figure ${b.no}`, description: b.caption, name: `fig${b.no}` } })] }),
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 }, children: runs(`Fig. ${b.no}. ${b.caption}`, { size: 16 }) }),
  ];
}

function h1El(b) {
  if (b.appendix && /^APPENDIX C/.test(b.text)) return new Paragraph({ pageBreakBefore: true, alignment: AlignmentType.CENTER, spacing: { before: 0, after: 80 }, keepNext: true, children: runs(b.text.toUpperCase(), { size: 18, bold: true }) });
  if (b.appendix) return new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 160, after: 80 }, keepNext: true, children: runs(b.text.toUpperCase(), { size: 18, bold: true }) });
  return new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 160, after: 80 }, keepNext: true, children: [new TextRun({ text: b.text.toUpperCase(), font: FONT, size: 18, smallCaps: false })] });
}
const h2El = (b) => new Paragraph({ spacing: { before: 100, after: 50 }, keepNext: true, children: [new TextRun({ text: b.text, italics: true, font: FONT, size: 20 })] });
const bulletEl = (items) => items.map((t) => new Paragraph({ numbering: { reference: 'bul', level: 0 }, alignment: AlignmentType.JUSTIFIED, spacing: { after: 40, line: 240 }, children: runs(t, { size: 20 }) }));
const refEl = (b) => new Paragraph({ alignment: AlignmentType.JUSTIFIED, spacing: { after: 40, line: 220 }, indent: { left: 360, hanging: 360 }, children: runs(b.text, { size: 16 }) });

// ---- sections ----
const footer = new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: 16 })] })] });
const pageProps = (cols, type) => ({
  type, page: { size: { width: PAGE_W, height: PAGE_H }, margin: { top: 1000, bottom: 1000, left: MARGIN, right: MARGIN } },
  column: cols === 2 ? { count: 2, space: GAP, equalWidth: true } : { count: 1 },
});

const sections = [];
// title block (single column)
const title = [
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 0, after: 120 }, children: [new TextRun({ text: meta.title, font: FONT, size: 40, bold: false })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 20 }, children: [new TextRun({ text: meta.authors, font: FONT, size: 22 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 20 }, children: [new TextRun({ text: meta.affil, font: FONT, size: 20, italics: true })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 160 }, children: [new TextRun({ text: `Submitted for: ${meta.course}`, font: FONT, size: 20, italics: true })] }),
];
const abstractBlock = [
  new Paragraph({ alignment: AlignmentType.JUSTIFIED, spacing: { after: 80, line: 240 }, indent: { left: 400, right: 400 },
    children: [new TextRun({ text: 'Abstract—', bold: true, italics: true, font: FONT, size: 18 }), ...runs(meta.abstract, { size: 18, bold: false })] }),
  new Paragraph({ alignment: AlignmentType.JUSTIFIED, spacing: { after: 140, line: 240 }, indent: { left: 400, right: 400 },
    children: [new TextRun({ text: 'Keywords—', bold: true, italics: true, font: FONT, size: 18 }), new TextRun({ text: meta.keywords, italics: true, font: FONT, size: 18 })] }),
];
sections.push({ properties: pageProps(1, SectionType.NEXT_PAGE), footers: { default: footer }, children: [...title, ...abstractBlock] });

let cur = { cols: 2, children: [], type: SectionType.CONTINUOUS };
const flush = () => { if (cur.children.length) sections.push({ properties: pageProps(cur.cols, cur.type), footers: { default: footer }, children: cur.children }); };
// appendix C: pages = [domain] [services + exceptions] [sequence]
const startSection = (cols, type) => { flush(); cur = { cols, children: [], type }; };
let appendixStarted = false;
let figIdx = 0;
let flagNext = false;
for (const b of blocks) {
  if (b.type === 'h1' && /^APPENDIX C/.test(b.text)) flagNext = true;
  if (b.type === 'fig' && b.mode === 'full' && flagNext) { b.firstOnPage = true; flagNext = false; }
  if (b.type === 'h1' && b.appendix && !appendixStarted) { appendixStarted = true; startSection(1, SectionType.NEXT_PAGE); }
  if (b.type === 'fig' && (b.mode === 'wide')) { startSection(1, SectionType.CONTINUOUS); cur.children.push(...figEl(b)); startSection(2, SectionType.CONTINUOUS); continue; }
  if (b.type === 'table' && b.wide && !appendixStarted) { startSection(1, SectionType.CONTINUOUS); cur.children.push(...tableEl(b)); startSection(2, SectionType.CONTINUOUS); continue; }
  if (b.type === 'fig' && b.mode === 'full') { startSection(1, b.firstOnPage ? SectionType.CONTINUOUS : SectionType.NEXT_PAGE); cur.children.push(...figEl(b)); continue; }
  if (b.type === 'fig' && b.mode === 'cont') { cur.children.push(...figEl(b)); continue; }
  if (b.type === 'h1') cur.children.push(h1El(b));
  else if (b.type === 'h2') cur.children.push(h2El(b));
  else if (b.type === 'p') cur.children.push(para(b.text));
  else if (b.type === 'bullets') cur.children.push(...bulletEl(b.items));
  else if (b.type === 'table') cur.children.push(...tableEl(b));
  else if (b.type === 'fig') cur.children.push(...figEl(b));
  else if (b.type === 'ref') cur.children.push(refEl(b));
}
flush();
// fix: first body section must be continuous with the title section
if (sections[1]) sections[1].properties = pageProps(sections[1].properties.column.count === 2 ? 2 : 1, SectionType.CONTINUOUS);

const doc = new Document({
  creator: 'V. Pathak, D. Shah, K. Thakran', title: meta.title,
  styles: { default: { document: { run: { font: FONT, size: 20 } } } },
  numbering: { config: [{ reference: 'bul', levels: [{ level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 360, hanging: 240 } } } }] }] },
  sections,
});
Packer.toBuffer(doc).then((buf) => { fs.writeFileSync(OUT, buf); console.log('wrote', OUT, buf.length, 'bytes;', blocks.filter(b => b.type === 'fig').length, 'figures,', blocks.filter(b => b.type === 'table').length, 'tables'); });
