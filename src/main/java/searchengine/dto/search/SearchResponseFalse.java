package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchResponseFalse {
    private boolean result = false;
    private String error;
}
