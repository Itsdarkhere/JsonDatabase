package Json.Database;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.*;

public class Client {
    @Parameter(names={"-t"})
    String type;
    @Parameter(names={"-k"})
    String key;
    @Parameter(names={"-v"})
    String value;
    @Parameter(names={"-in"})
    String fileName;

    public static void main(String[] args) {

        String address = "127.0.0.1";
        int port = 23451;
        try {
            System.out.println("Client started!");
            Socket socket = new Socket(InetAddress.getByName(address), port);
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());


            Client main = new Client();
            JCommander.newBuilder()
                    .addObject(main)
                    .build()
                    .parse(args);

            if (main.value != null) {
                String toSend = "{\"type\":\"" + main.type + "\", \"key\":\"" + main.key + "\", " +
                        "\"value\":\"" + main.value + "\"}";
                output.writeUTF(toSend);
                System.out.println("Sent: " + toSend);

            } else if (main.fileName != null){
                System.out.println(main.fileName);
                String toSend;
                JsonObject jsonObject = new Gson().fromJson(new FileReader("C:\\Users\\" +
                        "Valtt\\IdeaProjects\\JSON Database\\" +
                        "JSON Database\\task\\src\\client\\data\\" + main.fileName), JsonObject.class);

                JsonElement type = jsonObject.get("type");
                JsonElement key = jsonObject.get("key");
                JsonElement value = jsonObject.get("value");


                Input inputInForm = new Input(type, key, value);
                if (inputInForm.getValue() != null) {
                    toSend = "{\"type\":" + inputInForm.type + ",\"key\":" + inputInForm.key + "," +
                            "\"value\":" + inputInForm.value + "}";

                } else {
                    toSend = "{\"type\":" + inputInForm.type + ",\"key\":" + inputInForm.key + "}";

                }

                output.writeUTF(toSend);
                System.out.println("Sent: " + toSend);

            } else if (main.key != null) {
                String toSend = "{\"type\":\"" + main.type + "\", \"key\":\"" + main.key + "\"}";
                output.writeUTF(toSend);
                System.out.println("Sent: " + toSend);

            } else {
                String toSend = "{\"type\":\"" + main.type + "\"}";
                output.writeUTF(toSend);
                System.out.println("Sent: " + toSend);

            }

            String received = input.readUTF();
            System.out.println("Received: " + received);


        } catch (SocketException | UnknownHostException s) {
            System.out.println("eh" + s.getMessage());
        } catch (IOException e) {
            System.out.println(e.getCause());
        }


    }
}


