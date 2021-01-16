package Json.Database;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.*;
import java.util.HashMap;

public class DatabaseMethods {

    //synchronized method for saving the database, is called when changes are made
    synchronized void saveDatabase(HashMap<JsonElement, JsonElement> map) throws IOException {
        Writer writer = new FileWriter("storage.json");
        new Gson().toJson(map, writer);
        writer.close();

    }

    //synchronized method for getting the database
    synchronized HashMap<JsonElement, JsonElement> getDatabase() throws FileNotFoundException {
        Gson gson = new Gson();
        return gson.fromJson(new FileReader("storage.json"),
                HashMap.class);

    }
}
