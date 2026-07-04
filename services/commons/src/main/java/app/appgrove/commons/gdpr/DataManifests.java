package app.appgrove.commons.gdpr;

import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Helper per derivare il {@link DataManifest} (inventario tecnico della SPI) dalle annotazioni
 * {@link PersonalData} delle entità — unica sorgente di verità nel codice; la coerenza con i
 * manifesti YAML legali è garantita dal check bloccante di UC 0030. Usato dalle implementazioni
 * di {@link AppDataContract#manifest()}.
 */
public final class DataManifests {

    private DataManifests() {}

    /** Aggiunge a {@code out} una entry per ogni campo {@link PersonalData} dell'entità. */
    public static void collectPersonalData(Class<?> entity, String entityName, List<DataManifest.Entry> out) {
        for (Field field : entity.getDeclaredFields()) {
            PersonalData pd = field.getAnnotation(PersonalData.class);
            if (pd != null) {
                out.add(new DataManifest.Entry(
                        entityName, columnName(field), pd.category(), pd.purpose(), pd.legalBasis(), pd.retention()));
            }
        }
    }

    /** Nome colonna del campo: {@code @Column(name=...)} se presente, altrimenti il nome del campo. */
    public static String columnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.name().isBlank()) {
            return column.name();
        }
        return field.getName();
    }
}
