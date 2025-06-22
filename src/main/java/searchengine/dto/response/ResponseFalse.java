package searchengine.dto.response;

import lombok.Data;

@Data
public class ResponseFalse {
    private boolean result = false;
    private String error;
}
