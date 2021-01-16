package Json.Database;

import com.google.gson.JsonElement;

//Class that specifies the form that the input from client goes to and makes it easy to access.
public class Input {
    JsonElement type;
    JsonElement key;
    JsonElement value;

    Input (JsonElement type, JsonElement key, JsonElement value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }
    JsonElement getType() {
        return type;
    }
    JsonElement getKey() {
        return key;
    }
    JsonElement getValue() {
        return value;
    }

}