// ─────────────────────────────────────────────────────────────────────────────
// tools/drop-application/lib/plan.mjs — cosa rimuove il de-generatore.
//
// I percorsi qui elencati sono l'INVERSO delle destinazioni che
// tools/new-application/generate.mjs (buildPlan) crea per un'app:
//   • DUE alberi dedicati all'app, rimossi per intero (il servizio Quarkus e il
//     modulo frontend): tutto ciò che vive lì dentro appartiene solo all'app;
//   • TRE file singoli dentro cartelle CONDIVISE (test end-to-end, manifesto
//     dati, listino): qui si rimuove il file dell'app, non la cartella.
//
// Le prime due sono cartelle-per-app → non serve conoscere i ~40 file che il
// generatore vi ha scritto: si toglie la radice. Ciò rende il de-generatore
// robusto anche quando il dominio dell'app è stato riscritto dopo lo scaffolding
// (è il primo lavoro dopo new-application): non si confronta con i modelli, si
// rimuove l'albero.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Alberi dedicati all'app, rimossi per intero. Specchio delle destinazioni
 * `services/<id>` e `frontend/apps/backoffice/src/modules/<id>` di buildPlan.
 */
export function removedTrees(appId) {
  return [
    `services/${appId}`,
    `frontend/apps/backoffice/src/modules/${appId}`,
  ]
}

/**
 * File singoli dell'app dentro cartelle condivise. Specchio delle destinazioni
 * `frontend/apps/backoffice/e2e/<id>.spec.ts`, `docs/compliance/manifests/<id>.yaml`
 * e `services/core/.../pricing/<id>.yaml` di buildPlan.
 */
export function removedFiles(appId) {
  return [
    `frontend/apps/backoffice/e2e/${appId}.spec.ts`,
    `docs/compliance/manifests/${appId}.yaml`,
    `services/core/src/main/resources/pricing/${appId}.yaml`,
  ]
}
