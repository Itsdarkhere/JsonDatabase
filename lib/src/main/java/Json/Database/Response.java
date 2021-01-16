package Json.Database;

import com.google.gson.JsonElement;

public class Response {
    JsonElement response;
    JsonElement reason;
    JsonElement value;

    Response (JsonElement response, JsonElement reason, JsonElement value) {
        this.response = response;
        this.reason = reason;
        this.value = value;

    }
    @Override
    public String toString() {
        if (reason == null && value == null) {
            return "{\"response\":\"" + response + "\"}";
        } else if (reason != null) {
            return "{\"response\":\"" + response + "\", \"reason\":\""+ reason +"\")}";
        } else {
            return "{\"response\":\"OK\", \"value\":\"" + value + "\")}";
        }

    }
}
