package app.appgrove.core.catalog;

import java.util.UUID;

/**
 * Le voci di catalogo <b>di piattaforma</b> (UC 0103): quelle che portano un abbonamento senza essere
 * applicazioni. Oggi è una sola, i <b>posti</b>.
 *
 * <p>Lo slug è la chiave stabile e l'identificativo ne discende in modo <b>deterministico</b>, con lo
 * stesso algoritmo del resto del catalogo ({@link CatalogIds}): così la riga inserita dalla migrazione ha
 * lo stesso identificativo in ogni ambiente, e il codice può risolverla senza cercarla per tentativi. Il
 * valore è scritto anche nella migrazione — un collaudo verifica che i due coincidano, perché due copie
 * dello stesso identificativo che divergono in silenzio sarebbero una riga orfana in tabella.
 */
public final class PlatformCatalog {

    /** Slug della voce dei posti. Chiave stabile: cambiarlo è una migrazione, non una rinomina. */
    public static final String SEATS_SLUG = "platform-seats";

    private PlatformCatalog() {}

    /** Identificativo della voce dei posti, derivato dallo slug come ogni altra voce di catalogo. */
    public static UUID seatsAppId() {
        return CatalogIds.appId(SEATS_SLUG);
    }
}
