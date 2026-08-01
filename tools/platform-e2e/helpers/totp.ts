/**
 * totp — codici a tempo per il 2FA (UC 0091, J-PWD): implementazione RFC 6238 coerente col
 * servizio auth (TotpService: HMAC-SHA1, 6 cifre, periodo 30s, secret Base32; tolleranza ±1
 * periodo della libreria dev.samstevens.totp). Zero dipendenze: solo node:crypto.
 */
import { createHmac } from 'node:crypto'

const BASE32_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'

/** Decodifica Base32 (RFC 4648, senza padding) → Buffer. */
function base32Decode(input: string): Buffer {
  const clean = input.toUpperCase().replace(/=+$/, '').replace(/\s+/g, '')
  let bits = 0
  let value = 0
  const out: number[] = []
  for (const ch of clean) {
    const idx = BASE32_ALPHABET.indexOf(ch)
    if (idx < 0) throw new Error(`carattere Base32 non valido nel secret TOTP: "${ch}"`)
    value = (value << 5) | idx
    bits += 5
    if (bits >= 8) {
      out.push((value >>> (bits - 8)) & 0xff)
      bits -= 8
    }
  }
  return Buffer.from(out)
}

/**
 * Codice TOTP a 6 cifre per il `secret` Base32 all'istante `at` (default: adesso).
 * Parametri fissi = quelli del servizio auth: SHA1, periodo 30s, 6 cifre.
 */
export function totp(secretBase32: string, at: number = Date.now()): string {
  const counter = Math.floor(at / 1000 / 30)
  const msg = Buffer.alloc(8)
  msg.writeBigUInt64BE(BigInt(counter))
  const digest = createHmac('sha1', base32Decode(secretBase32)).update(msg).digest()
  const offset = digest[digest.length - 1] & 0x0f
  const code =
    (((digest[offset] & 0x7f) << 24) |
      ((digest[offset + 1] & 0xff) << 16) |
      ((digest[offset + 2] & 0xff) << 8) |
      (digest[offset + 3] & 0xff)) %
    1_000_000
  return code.toString().padStart(6, '0')
}
