package app.appgrove.core.catalog;

/**
 * Che tipo di voce di catalogo è una riga di {@code platform.app} (UC 0103).
 *
 * <p>Esiste perché l'abbonamento dei <b>posti</b> è appeso al catalogo — scelta dell'epica E22.2, presa
 * per riusare interi il pagamento, gli eventi del fornitore, il ciclo di vita e la fatturazione invece di
 * riscriverli — ma i posti <b>non sono una applicazione</b>: non si aprono, non stanno in vetrina, non
 * concedono l'accesso a nulla.
 *
 * <p><b>Perché un attributo e non un elenco di slug da escludere.</b> È la stessa domanda che la change
 * 0092 ha lasciato aperta, e la risposta è che un elenco di slug è una regola che invecchia in silenzio:
 * la voce di piattaforma numero due non comparirebbe in nessun elenco e nessun collaudo diventerebbe
 * rosso. Con l'attributo, ogni lettura che elenca applicazioni chiede {@link #application} e le voci
 * future sono coperte per costruzione.
 */
public enum AppKind {

    /** Applicazione del marketplace: si vende, si apre, concede diritti d'accesso. */
    application,

    /**
     * Voce di piattaforma: porta un abbonamento ma non è una applicazione. Va esclusa da ogni superficie
     * che elenca applicazioni; resta visibile nella console di amministrazione, <b>marcata</b>, perché
     * chi amministra deve poter vedere che esiste.
     */
    platform
}
