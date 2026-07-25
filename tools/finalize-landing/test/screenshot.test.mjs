// Test della cattura screenshot (UC 0057).
//
// La cattura richiede un browser (Playwright). Il browser vive in una cache GLOBALE
// condivisa col frontend: se è presente, il test dimostra il plumbing (HTML → PNG); se
// non lo è (ambiente headless senza chromium), il test SALTA con grazia — la cattura è
// esplicitamente una prova "più pesante/ambiente-dipendente" (UC 0057, requisiti di test).
import { test } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import sharp from 'sharp'
import { captureHtmlToPng, loadChromium } from '../lib/screenshot.mjs'

/** True se un browser Playwright è lanciabile in questo ambiente. */
async function browserAvailable() {
  try {
    const chromium = await loadChromium()
    const browser = await chromium.launch()
    await browser.close()
    return true
  } catch {
    return false
  }
}

test('captureHtmlToPng produce un PNG delle dimensioni richieste', async (t) => {
  if (!(await browserAvailable())) {
    t.skip('browser Playwright non disponibile in questo ambiente — cattura non verificabile qui')
    return
  }
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'finalize-shot-'))
  const outPath = path.join(dir, 'hero.en.png')
  const html = '<html><body style="margin:0"><div style="width:1280px;height:800px;background:#0b1020;color:#fff;font:48px sans-serif;display:flex;align-items:center;justify-content:center">appgrove</div></body></html>'
  const result = await captureHtmlToPng({ html, outPath, width: 1280, height: 800 })
  assert.equal(result, outPath)
  assert.ok(fs.existsSync(outPath), 'il file PNG esiste')
  const meta = await sharp(outPath).metadata()
  assert.equal(meta.format, 'png')
  assert.equal(meta.width, 1280)
  assert.equal(meta.height, 800)
})
