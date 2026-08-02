package app.appgrove.core.support;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Riconoscimento delle <b>categorie particolari di dati</b> (articolo 9 del Regolamento europeo:
 * salute, convinzioni religiose o filosofiche, opinioni politiche, appartenenza sindacale, origine
 * razziale o etnica, dati genetici o biometrici, vita e orientamento sessuale) nel testo libero di
 * un ticket — UC 0075 §5 "Escalation categorie particolari".
 *
 * <p>Cosa fa: alza la priorità a {@code high} e accende il contrassegno «da rivedere», così che una
 * richiesta delicata non resti in fondo alla coda. Cosa <b>non</b> fa, di proposito: non registra
 * quale categoria avrebbe riconosciuto, non conserva il risultato dell'analisi altrove e non prende
 * decisioni al posto di nessuno. Registrare «questa persona ha scritto di salute» significherebbe
 * creare un dato di categoria particolare <i>derivato</i> là dove prima c'era solo testo libero: la
 * segnalazione serve a chiamare un essere umano, non a classificare l'interessato.
 *
 * <p>Il riconoscitore è <b>deterministico</b> e ammette volentieri i falsi positivi: segnalare in
 * più costa una lettura, segnalare in meno costa una richiesta delicata trattata come le altre. Non
 * c'è nessun modello statistico e nessun servizio esterno — sarebbe un responsabile del trattamento
 * in più per un problema che si risolve con un elenco di parole.
 *
 * <p>Le parole-spia sono in italiano e in inglese, le due lingue in cui la piattaforma parla ai
 * propri clienti. Il confronto avviene su testo normalizzato (minuscolo, accenti rimossi) e ancorato
 * all'inizio di parola, così «razza» non viene trovata dentro «terrazza».
 */
public final class SpecialCategoryScreening {

    /**
     * Radici delle parole-spia: il confronto è per <b>inizio di parola</b>, quindi «diagnos» copre
     * diagnosi/diagnosis/diagnostico senza moltiplicare le voci. Elenco volutamente leggibile e
     * modificabile: è documentazione oltre che codice.
     */
    private static final List<String> STEMS = List.of(
            // salute e cure
            "salute", "malatt", "diagnos", "terapi", "patologi", "sintom", "ricover", "ospedal",
            "referto", "cartella clinica", "farmac", "medicinal", "invalidit", "disabil",
            "handicap", "gravidanz", "psicolog", "psichiatr", "depress", "oncolog", "tumor",
            "cancro", "hiv", "aids", "diabet", "vaccin", "allergi", "infortun",
            "health", "illness", "disease", "diagnosi", "therap", "symptom", "hospital",
            "medical record", "medication", "disabilit", "pregnan", "psycholog", "psychiatr",
            "depression", "oncolog", "tumour", "tumor", "cancer", "diabet", "vaccin", "allerg",
            "injur",
            // origine razziale o etnica
            "razzial", "razza", "etnia", "etnic", "ethnic", "racial",
            // opinioni politiche e appartenenza sindacale
            "opinioni politiche", "partito politico", "sindacat", "sindacal", "political opinion",
            "trade union", "union member",
            // convinzioni religiose o filosofiche
            "religios", "religion", "confession religios", "credo religioso", "religiou",
            "belief",
            // dati genetici e biometrici
            "genetic", "biometric", "impronta digitale", "fingerprint", "dna",
            // vita e orientamento sessuale
            "orientamento sessuale", "vita sessuale", "sexual orientation", "sex life",
            "transgender", "omosessual", "homosexual");

    private static final List<Pattern> PATTERNS = STEMS.stream()
            .map(stem -> Pattern.compile("\\b" + Pattern.quote(stem), Pattern.UNICODE_CHARACTER_CLASS))
            .toList();

    private SpecialCategoryScreening() {}

    /**
     * Vero se il testo contiene almeno una parola-spia. I frammenti nulli sono ignorati, così si può
     * passare oggetto e messaggio insieme senza preoccuparsi.
     */
    public static boolean flags(String... fragments) {
        for (String fragment : fragments) {
            if (fragment == null || fragment.isBlank()) {
                continue;
            }
            String normalized = normalize(fragment);
            for (Pattern pattern : PATTERNS) {
                if (pattern.matcher(normalized).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Minuscolo e accenti rimossi: «Malattìa» e «malattia» devono valere lo stesso. */
    private static String normalize(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
