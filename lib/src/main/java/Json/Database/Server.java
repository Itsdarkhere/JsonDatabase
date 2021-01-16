package Json.Database;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Server {

    public static void main(String[] args) {
        String address = "127.0.0.1";
        int port = 23451;
        ExecutorService executor = Executors.newFixedThreadPool(100);
        boolean on = true;



        try {
            System.out.println("Server started!");
            //initialize server
            ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName(address));

            //while "on" server is accepting new connections
            while (on) {

                //accept a new client connection
                Socket socket = server.accept();
                DataInputStream input = new DataInputStream(socket.getInputStream());

                //receive input from user
                String jsonString = input.readUTF();
                Gson gson = new Gson();
                Input inputInForm = gson.fromJson(jsonString, Input.class);

                Switcher switcher = new Switcher(socket, inputInForm, gson);

                //giving incoming tasks to threads via executorService
                executor.submit(() -> {

                    try {
                        switcher.run();

                        while (true) {

                            if (switcher.isSocketAlive()) {
                                executor.shutdown();
                                socket.close();
                                server.close();
                                break;

                            }




                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                });

                //checking if exit command has been called
                if (executor.isShutdown()) {
                    on = false;
                    server.close();
                }



            }

        } catch (SocketException | UnknownHostException e) {
            System.out.println(e.getMessage());
            System.out.println(e.getCause());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static class Switcher {
        private final Input inputInForm;
        private final Gson gson;
        private final Socket socket;


        Switcher(Socket socket, Input inputInForm, Gson gson) {

            this.socket = socket;
            this.gson = gson;
            this.inputInForm = inputInForm;

        }

        void run() throws IOException {
            //lock for locking the file when reading or editing
            ReadWriteLock lock = new ReentrantReadWriteLock();
            Lock writeLock = lock.writeLock();

            //useful stuff
            HashMap<JsonElement, JsonElement> database;
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            String message;
            String messageInJson;
            Response response;
            String[] split;
            String HKey = null;
            JsonObject jO = null;
            HashMap<JsonElement, JsonElement> map;


            //Selecting action based on the received input (get, set, delete, exit).
            try {

                switch (gson.fromJson(inputInForm.getType(), String.class)) {

                    case "get":
                        //getting database is surrounded in a try catch since it throws an error if its not found.
                        //if the db does not exist, it does not contain the key. So the catch handles the response.
                        try {

                            database = getDatabase();

                        } catch (Exception f) {
                            System.out.println(f.getMessage());
                            message = "{\"response\":\"ERROR\", \"reason\":\"No such key\"}";
                            response = gson.fromJson(message, Response.class);
                            messageInJson = gson.toJson(response);
                            output.writeUTF(messageInJson);
                            break;
                        }

                        //implementing searching through jsonObjects for internal JsonElements
                        split = inputInForm.getKey().toString().split("");
                        //if its a JsonArray
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


                    case "set":


                        //if there is no database on record, will create an empty one
                        try {
                            database = getDatabase();

                        } catch (Exception f) {
                            database = new HashMap<JsonElement, JsonElement>();
                        }

                        //implementing searching through jsonObjects for internal JsonElements
                        split = inputInForm.getKey().toString().split("");
                        JsonObject object = null;
                        JsonObject o1 = new JsonObject();
                        String trimmedKey = null;
                        JsonObject j = null;

                        //if its a JsonArray
                        if (split[0].equals("[")) {
                            JsonArray J = gson.fromJson(inputInForm.getKey(), JsonArray.class);
                            for (int i = 0; i < J.size(); i++) {
                                //gets object at first from database
                                if (i == 0) {
                                    //if the db does not contain the value, creates a new one from scratch.
                                    if (database.get(J.get(i).toString()) == null) {
                                        for (int d = J.size()-1; d > 0; d--) {
                                            if (d == J.size()-1) {
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

                            saveDatabase(database);
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
                            saveDatabase(database);
                            writeLock.unlock();
                            message = "{\"response\":\"OK\"}";
                            response = gson.fromJson(message, Response.class);
                            messageInJson = gson.toJson(response);
                            output.writeUTF(messageInJson);
                            break;

                        }



                    case "delete":

                        try {
                            database = getDatabase();

                        } catch (Exception f) {
                            message = "{\"response\":\"ERROR\", \"reason\":\"No such key\"}";
                            response = gson.fromJson(message, Response.class);
                            messageInJson = gson.toJson(response);
                            output.writeUTF(messageInJson);
                            break;

                        }
                        JsonObject temp = new JsonObject();
                        JsonObject toUse = new JsonObject();
                        //implementing searching through jsonObjects for internal JsonElements
                        split = inputInForm.getKey().toString().split("");
                        //if its a JsonArray
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
                                        saveDatabase(database);
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
                                saveDatabase(database);
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
                        break;



                    case "exit":
                        //close the server on "exit" command
                        message = "{\"response\":\"OK\"}";
                        response = gson.fromJson(message, Response.class);
                        messageInJson = gson.toJson(response);
                        output.writeUTF(messageInJson);
                        socket.close();
                        break;

                    default:
                        //close server and stops accepting new client connections
                        message = "{\"response\":\"ERROR\"}";
                        response = gson.fromJson(message, Response.class);
                        messageInJson = gson.toJson(response);
                        output.writeUTF(messageInJson);
                        break;
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


        //synchronized method for saving the database, is called when changes are made
        synchronized void saveDatabase(HashMap<JsonElement, JsonElement> map) throws IOException {
            Writer writer = new FileWriter("C:\\Users\\Valtt\\IdeaProjects\\" +
                    "JSON Database\\JSON Database\\task\\src\\server\\data\\duuuUumb.json");
            new Gson().toJson(map, writer);
            writer.close();


        }

        //synchronized method for getting the database
        synchronized HashMap<JsonElement, JsonElement> getDatabase() throws FileNotFoundException {
            Gson gson = new Gson();
            HashMap<JsonElement, JsonElement> map = gson.fromJson(new FileReader("C:\\Users\\" +
                    "Valtt\\IdeaProjects\\JSON Database\\JSON Database\\task\\src\\server\\data\\duuuUumb.json"), HashMap.class);
            return  map;

        }
        void jsonArray() {

        }


    }

}
//Class that specifies the form that the input from client goes to and makes it easy to access.
class Input {
    JsonElement type;
    JsonElement key;
    JsonElement value;

    Input(JsonElement type, JsonElement key, JsonElement value) {
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
class Response {
    JsonElement response;
    JsonElement reason;
    JsonElement value;

    Response(JsonElement response, JsonElement reason, JsonElement value) {
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
