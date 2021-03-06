package Json.Database;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Engine {
    private final Input inputInForm;
    private final Gson gson;
    private final Socket socket;


    Engine (Socket socket, Input inputInForm, Gson gson) {

        this.socket = socket;
        this.gson = gson;
        this.inputInForm = inputInForm;

    }

    void run() throws IOException {
        //lock for locking the file when reading or editing
        ReadWriteLock lock = new ReentrantReadWriteLock();
        Lock writeLock = lock.writeLock();

        //useful stuff
        DatabaseMethods databaseMethods = new DatabaseMethods();
        HashMap<JsonElement, JsonElement> database;
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        String message;
        String messageInJson;
        Response response;
        String[] split;
        String HKey = null;
        JsonObject jO = null;
        String trimmedKey = null;

        try {

            //getting database is surrounded in a try catch since it throws an error if its not found.
            //if the db does not exist, it does not contain the key. So the catch handles the response.
            //Selecting action based on the received input (get, set, delete, exit).

            switch (gson.fromJson(inputInForm.getType(), String.class)) {
                case "get" -> {
                    try {

                        database = databaseMethods.getDatabase();

                    } catch (Exception f) {

                        System.out.println(f.getMessage());
                        message = "{\"response\":\"ERROR\", \"reason\":\"No such key\"}";
                        response = gson.fromJson(message, Response.class);
                        messageInJson = gson.toJson(response);
                        output.writeUTF(messageInJson);
                        break;
                    }

                    split = inputInForm.getKey().toString().split("");
                    if (split[0].equals("[")) {
                        JsonArray J = gson.fromJson(inputInForm.getKey(), JsonArray.class);
                        for (int i = 0; i < J.size(); i++) {
                            if (i + 1 == J.size()) {
                                //trimmed removes the "" marks from the string
                                String trimmed = J.get(i).toString().substring(1, J.get(i).toString().length() - 1);
                                //sets the value that is being looked for.
                                //if statement incase jO is null
                                if (jO == null) {
                                    jO = gson.toJsonTree(database.get(J.get(0).toString())).getAsJsonObject();
                                    HKey = jO.toString();
                                    break;
                                } else {
                                    HKey = jO.get(trimmed).toString();
                                    break;
                                }


                            } else if (i == 0) {
                                //gets from database
                                jO = gson.toJsonTree(database.get(J.get(i).toString())).getAsJsonObject();
                            } else {
                                //gets from inside the object
                                jO = gson.toJsonTree(jO.get(trimKey(J.get(i).toString()))).getAsJsonObject();
                            }


                        }

                        message = "{\"response\":\"OK\", \"value\":" + HKey + "}";
                        response = gson.fromJson(message, Response.class);
                        messageInJson = gson.toJson(response);
                        output.writeUTF(messageInJson);
                        break;

                        //if its a normal String that  we are looking for
                    } else {
                        System.out.println("normal");

                        //if the key is in the database
                        if (database.containsKey(inputInForm.getKey().toString())) {

                            message = "{\"response\":\"OK\", \"value\":\"" +
                                    database.get(inputInForm.getKey().toString()).toString() + "\"}";
                            response = gson.fromJson(message, Response.class);
                            messageInJson = gson.toJson(response);
                            output.writeUTF(messageInJson);
                            break;

                        } else {

                            message = "{\"response\":\"ERROR\", \"reason\":\"No such key\"}";
                            response = gson.fromJson(message, Response.class);
                            messageInJson = gson.toJson(response);
                            output.writeUTF(messageInJson);
                            break;
                        }

                    }
                }

                case "set" -> {
                    try {
                        database = databaseMethods.getDatabase();

                    } catch (Exception f) {
                        database = new HashMap<JsonElement, JsonElement>();
                    }
                    split = inputInForm.getKey().toString().split("");
                    JsonObject object = null;
                    JsonObject o1 = new JsonObject();
                    JsonObject j = null;
                    if (split[0].equals("[")) {
                        JsonArray J = gson.fromJson(inputInForm.getKey(), JsonArray.class);
                        for (int i = 0; i < J.size(); i++) {
                            //gets object at first from database
                            if (i == 0) {
                                //if the db does not contain the value, creates a new one from scratch.
                                if (database.get(J.get(i).toString()) == null) {
                                    for (int d = J.size() - 1; d > 0; d--) {
                                        if (d == J.size() - 1) {
                                            //creates the next object with the specified value
                                            j = new JsonObject();
                                            //remove ""
                                            String trimmedInput = trimKey(J.get(d).toString());
                                            j.add(trimmedInput, inputInForm.getValue());
                                            //object is the object we want to be using later on
                                            object = j;
                                        } else {
                                            //Creating o1 again allows me to reset and use again
                                            o1 = new JsonObject();
                                            //layers it one layer deeper
                                            //remove ""
                                            String trimmedInput = trimKey(J.get(d).toString());
                                            o1.add(trimmedInput, j);
                                            //j is the object we want to be using if it loops again
                                            j = o1;
                                            //object is the object we want to be using later on
                                            object = j;

                                            //if the object has been layered as intended it breaks from loop
                                            if (d - 1 == 0) {
                                                break;
                                            }

                                        }

                                    }
                                    if (j != null) {

                                        break;
                                    }

                                    //if it does contain the value.
                                } else {
                                    object = gson.toJsonTree(database.get(J.get(i).toString())).getAsJsonObject();
                                }


                                //Mean it has reached the object we want to change.
                            } else if (i + 1 == J.size()) {
                                trimmedKey = trimKey(J.get(i).toString());
                                if (o1 == null) {
                                    object.add(trimmedKey, inputInForm.getValue());

                                } else {

                                    o1.add(trimmedKey, inputInForm.getValue());
                                    trimmedKey = trimKey(J.get(i - 1).toString());
                                    object.remove(trimmedKey);
                                    object.add(trimmedKey, o1);

                                }


                                //advances another layer inside the json
                            } else {
                                trimmedKey = trimKey(J.get(i).toString());
                                o1 = gson.toJsonTree(object.get(trimmedKey)).getAsJsonObject();


                            }


                        }

                        writeLock.lock();
                        database.remove(J.get(0).toString());
                        database.put(J.get(0), object);

                        databaseMethods.saveDatabase(database);
                        writeLock.unlock();
                        message = "{\"response\":\"OK\"}";
                        response = gson.fromJson(message, Response.class);
                        messageInJson = gson.toJson(response);
                        output.writeUTF(messageInJson);
                        break;

                        //if its just a normal String value
                    } else {
                        writeLock.lock();
                        database.put(inputInForm.getKey(), inputInForm.getValue());
                        databaseMethods.saveDatabase(database);
                        writeLock.unlock();
                        message = "{\"response\":\"OK\"}";
                        response = gson.fromJson(message, Response.class);
                        messageInJson = gson.toJson(response);
                        output.writeUTF(messageInJson);
                        break;

                    }
                }
                case "delete" -> {
                    try {
                        database = databaseMethods.getDatabase();

                    } catch (Exception f) {
                        message = "{\"response\":\"ERROR\", \"reason\":\"No such key\"}";
                        response = gson.fromJson(message, Response.class);
                        messageInJson = gson.toJson(response);
                        output.writeUTF(messageInJson);
                        break;

                    }
                    JsonObject temp = new JsonObject();
                    JsonObject toUse = new JsonObject();
                    split = inputInForm.getKey().toString().split("");
                    if (split[0].equals("[")) {
                        JsonArray J = gson.fromJson(inputInForm.getKey(), JsonArray.class);
                        for (int i = 0; i < J.size(); i++) {
                            if (i == 0) {

                                toUse = gson.toJsonTree(database.get(J.get(i).toString())).getAsJsonObject();

                                //Means it has reached the object we want to change.
                            } else if (i + 1 == J.size()) {
                                trimmedKey = trimKey(J.get(i).toString());
                                if (temp == null) {
                                    toUse.add(trimmedKey, inputInForm.getValue());

                                } else {

                                    //removes the key
                                    temp.remove(trimmedKey);

                                    trimmedKey = trimKey(J.get(i - 1).toString());
                                    toUse.remove(trimmedKey);
                                    toUse.add(trimmedKey, temp);


                                    writeLock.lock();
                                    database.remove(J.get(0).toString());
                                    database.put(J.get(0), toUse);
                                    databaseMethods.saveDatabase(database);
                                    writeLock.unlock();
                                    message = "{\"response\":\"OK\"}";
                                    response = gson.fromJson(message, Response.class);
                                    messageInJson = gson.toJson(response);
                                    output.writeUTF(messageInJson);
                                    break;

                                }

                                //advances another layer inside the json
                            } else {
                                trimmedKey = trimKey(J.get(i).toString());
                                temp = gson.toJsonTree(toUse.get(trimmedKey)).getAsJsonObject();


                            }


                        }

                        //if its a normal String that  we are looking for
                    } else {
                        writeLock.lock();
                        if (database.containsKey(inputInForm.getKey().toString())) {
                            database.remove(inputInForm.getKey());
                            databaseMethods.saveDatabase(database);
                            writeLock.unlock();

                            message = "{\"response\":\"OK\"}";
                            response = gson.fromJson(message, Response.class);
                            messageInJson = gson.toJson(response);
                            output.writeUTF(messageInJson);
                            break;

                        } else {
                            message = "{\"response\":\"ERROR\", \"reason\":\"No such key\"}";
                            response = gson.fromJson(message, Response.class);
                            messageInJson = gson.toJson(response);
                            output.writeUTF(messageInJson);
                            break;
                        }

                    }
                }
                case "exit" -> {
                    message = "{\"response\":\"OK\"}";
                    response = gson.fromJson(message, Response.class);
                    messageInJson = gson.toJson(response);
                    output.writeUTF(messageInJson);
                    socket.close();
                }
                default -> {
                    message = "{\"response\":\"ERROR\"}";
                    response = gson.fromJson(message, Response.class);
                    messageInJson = gson.toJson(response);
                    output.writeUTF(messageInJson);
                }
            }

        } catch (Exception b) {
            System.out.println(inputInForm.getKey());
            b.printStackTrace();
            System.out.println(b.getMessage());
        }
    }

    boolean isSocketAlive() {
        return socket.isClosed() ;
    }

    //removes ""
    String trimKey(String key) {
        return key.substring(1, key.length()-1);
    }

}