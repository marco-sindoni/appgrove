package app.appgrove.core.legal;

import java.util.Set;

/**
 * Componenti legali (allineati a {@code content/legal/_config.yaml}). La <b>ri-accettazione runtime</b>
 * (UC 0056) riguarda i soli componenti <b>vincolanti</b>: {@code terms} (accettazione esplicita),
 * {@code privacy} e {@code cookie} (presa d'atto). {@code refund} è dentro i Termini e {@code subprocessors}
 * non blocca (→ notifica/preavviso, differito): non innescano la schermata bloccante.
 */
public enum LegalComponent {
    terms,
    privacy,
    cookie,
    refund,
    subprocessors;

    /** Componenti che innescano il gate di (ri-)accettazione al login (#14 riga 143). */
    public static final Set<LegalComponent> BINDING = Set.of(terms, privacy, cookie);

    /** Atto richiesto per il componente: i Termini si <b>accettano</b>, Privacy/Cookie si <b>prendono atto</b>. */
    public LegalActType requiredAct() {
        return this == terms ? LegalActType.accept : LegalActType.acknowledge;
    }

    public boolean isBinding() {
        return BINDING.contains(this);
    }
}
