-- UC 0118 (change 0090) — inviti e registrazione quando l'identità esiste già.
--
-- Dopo UC 0116 il modello AMMETTE che una persona appartenga a più account, ma nessun percorso di
-- prodotto ne crea una seconda appartenenza: l'invito a chi esiste già arriva fino all'accettazione e
-- lì si ferma. Questa migrazione dà alla riga di invito le due cose che le mancano per farlo entrare.

-- ── il collegamento all'identità che esiste già ──────────────────────────────
-- Valorizzato LATO SERVER al momento dell'invio, quando un'identità con quell'indirizzo esiste. Serve
-- all'accettazione (sapere che non c'è nessuna identità da coniare) e alle prove di adempimento.
--
-- ANNULLABILE, e resta nullo nel caso normale: la maggior parte degli invitati non esiste ancora sulla
-- piattaforma. Un valore nullo NON significa «non controllato»: significa «al momento dell'invio non
-- c'era». Se la persona si registra da sola mentre l'invito è in attesa, l'invito resta valido e
-- l'accettazione trova l'identità comunque — il collegamento è un'ottimizzazione della lettura, non
-- l'autorità.
--
-- IL VALORE NON ESCE MAI VERSO CHI HA INVITATO (UC 0118 §5): sapere che quella persona ha già un
-- rapporto con la piattaforma è un'informazione che non appartiene all'account che la invita. Per
-- questo non compare in InvitationView né in alcuna interfaccia di account.
--
-- Nessun ON DELETE: la cancellazione di un'identità passa dalla purga (UC 0033), che tratta gli inviti
-- dell'account insieme a tutto il resto. Un vincolo con azione automatica qui darebbe l'impressione che
-- l'invito sopravviva alla persona.
ALTER TABLE platform.invitations
    ADD COLUMN identity_id uuid REFERENCES platform.identity (id);

COMMENT ON COLUMN platform.invitations.identity_id IS
    'UC 0118 — identità già esistente a cui l''invito si riferisce, valorizzata lato server all''invio '
    'quando quell''indirizzo ha già un''identità; nulla altrimenti. NON viene mai restituita a chi ha '
    'invitato: l''esistenza di un rapporto fra quella persona e la piattaforma non è informazione '
    'dell''account che invita.';

-- ── la lettura «quali inviti sono indirizzati a me?» ─────────────────────────
-- È una lettura di PIATTAFORMA: attraversa gli account per costruzione, perché la domanda ha per
-- soggetto la persona e non l'account — come `ix_membership_identity` di V17. L'indice è su
-- lower(email) perché il confronto degli indirizzi è sempre in minuscolo (l'unicità dell'identità lo è).
CREATE INDEX ix_invitations_email_status
    ON platform.invitations (lower(email), status);

-- ── lo stato «rifiutato» ─────────────────────────────────────────────────────
-- Non c'è alcun vincolo di dominio da estendere sulla colonna `status` (varchar senza CHECK, V2): lo
-- stato nuovo vive nell'enumerazione Java. La riga sta qui perché il posto in cui si cerca «quali stati
-- esistono» è la migrazione, e trovare quattro stati nella banca dati e cinque nel codice è il modo di
-- non capirlo mai. Stati: pending → accepted | revoked | expired | rejected.
--   * revoked  = l'ha chiuso chi ha invitato;
--   * rejected = l'ha chiuso la persona invitata (UC 0118 §6). Sono due atti diversi, di due soggetti
--                diversi, e confonderli renderebbe illeggibile la storia dell'invito.
