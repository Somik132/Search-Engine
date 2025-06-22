package searchengine.dto.search;

import lombok.Data;

@Data
public class ResponsePage implements Comparable<ResponsePage>{
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;

    @Override
    public int compareTo(ResponsePage other) {
        return (int) (other.relevance - this.relevance);
    }
}
