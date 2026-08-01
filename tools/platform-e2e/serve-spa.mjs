#!/usr/bin/env node
// serve-spa.mjs — server statico + inoltro API per la suite e2e di piattaforma (UC 0090).
//
// L'equivalente HEADLESS del blocco `api-routes` del dev/Caddyfile: serve il dist/ di una
// SPA costruita davvero e inoltra le rotte /api/* ai servizi della suite. Le rotte NON sono
// cablate qui: arrivano da un file JSON generato da run.sh a partire dalla stessa scoperta
// servizi di dev/lib/services.sh — una nuova app entra nella suite senza toccare questo file.
//
// Solo libreria standard Node (http/fs/path): zero dipendenze, zero TLS, zero /etc/hosts —
// la fragilità che lo smoke headless (change 0037) ha volutamente evitato resta fuori.
//
// Uso:
//   node serve-spa.mjs --port 24173 --dist <dir dist SPA> --config <config.json di suite> \
//                      --routes <routes.json: [{"prefix":"/api/auth/","target":"http://localhost:21100"}]>

import http from 'node:http'
import { readFileSync, existsSync, statSync } from 'node:fs'
import { join, resolve, normalize, extname } from 'node:path'

function arg(name) {
  const i = process.argv.indexOf(`--${name}`)
  if (i < 0 || i + 1 >= process.argv.length) {
    console.error(`serve-spa: argomento --${name} mancante`)
    process.exit(2)
  }
  return process.argv[i + 1]
}

const port = Number(arg('port'))
const dist = resolve(arg('dist'))
const configPath = resolve(arg('config'))
const routes = JSON.parse(readFileSync(resolve(arg('routes')), 'utf8'))

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.map': 'application/json',
  '.txt': 'text/plain; charset=utf-8',
  '.webmanifest': 'application/manifest+json',
}

/** Inoltra la richiesta al servizio della suite indicato dalla rotta (streaming, cookie inclusi). */
function proxy(req, res, target) {
  const url = new URL(target)
  const upstream = http.request(
    {
      hostname: url.hostname,
      port: url.port,
      path: req.url,
      method: req.method,
      headers: { ...req.headers, host: `${url.hostname}:${url.port}` },
    },
    (up) => {
      res.writeHead(up.statusCode ?? 502, up.headers)
      up.pipe(res)
    },
  )
  upstream.on('error', (err) => {
    res.writeHead(502, { 'content-type': 'application/json' })
    res.end(JSON.stringify({ title: 'Bad Gateway (suite platform-e2e)', detail: String(err) }))
  })
  req.pipe(upstream)
}

const server = http.createServer((req, res) => {
  const path = new URL(req.url ?? '/', 'http://localhost').pathname

  // 1. rotte API → servizi della suite (derivate dalla scoperta servizi, vedi run.sh)
  const route = routes.find((r) => path.startsWith(r.prefix))
  if (route) return proxy(req, res, route.target)

  // 2. config runtime della suite (mai in cache: stesso contratto di public/config.json)
  if (path === '/config.json') {
    res.writeHead(200, { 'content-type': MIME['.json'], 'cache-control': 'no-store' })
    return res.end(readFileSync(configPath))
  }

  // 3. asset statico dal dist della SPA (path traversal escluso), altrimenti fallback SPA
  const safe = normalize(join(dist, path)).startsWith(dist) ? normalize(join(dist, path)) : dist
  const file = existsSync(safe) && statSync(safe).isFile() ? safe : join(dist, 'index.html')
  try {
    const body = readFileSync(file)
    res.writeHead(200, { 'content-type': MIME[extname(file)] ?? 'application/octet-stream' })
    res.end(body)
  } catch {
    res.writeHead(404, { 'content-type': MIME['.txt'] })
    res.end('not found')
  }
})

server.listen(port, () => {
  console.log(`serve-spa: ${dist} su http://localhost:${port} (${routes.length} rotte API inoltrate)`)
})
