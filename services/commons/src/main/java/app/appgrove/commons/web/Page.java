package app.appgrove.commons.web;

import java.util.List;

/** Risultato paginato offset-based (page/size + totali). */
public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> Page<T> of(List<T> content, PageRequest request, long totalElements) {
        int totalPages = request.size() == 0 ? 0 : (int) Math.ceil((double) totalElements / request.size());
        return new Page<>(content, request.page(), request.size(), totalElements, totalPages);
    }
}
