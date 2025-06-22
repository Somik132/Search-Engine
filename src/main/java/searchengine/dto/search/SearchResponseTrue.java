package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponseTrue {
    private boolean result = true;
    private int count;
    private List<ResponsePage> data;
}
