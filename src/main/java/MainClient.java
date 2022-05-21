import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Stream;

public class MainClient {

    private static String HOST;
    private static String PORT;
    private static String USER_NAME;
    private static String currentPath = "/";
    private static String currentPathInSystem = System.getProperty("user.dir");

    public static void main(String[] args) throws IOException {
        if (args.length == 3) {
            HOST = args[0];
            PORT = args[1];
            USER_NAME = args[2];
            while (true) {
                System.out.println("Write command:");
                Scanner input = new Scanner(System.in);
                String[] actions = input.nextLine().split(" ");
                Action currentAction = Action.getByName(actions[0]);
                if (currentAction == null) {
                    break;
                }

                if (currentAction.equals(Action.MKDIR)) {
                    mkdir(currentAction, actions[1]);
                } else if (currentAction.equals(Action.PUT)) {
                    putOrAppend(currentAction, actions[1], null);
                } else if (currentAction.equals(Action.GET)) {
                    get(currentAction, actions[1]);
                } else if (currentAction.equals(Action.APPEND)) {
                    putOrAppend(currentAction, actions[2], actions[1]);
                } else if (currentAction.equals(Action.DELETE)) {
                    delete(currentAction, actions[1]);
                } else if (currentAction.equals(Action.LS)) {
                    ls(currentAction);
                } else if (currentAction.equals(Action.CD)) {
                    cd(actions[1]);
                } else if (currentAction.equals(Action.LLS)) {
                    lls();
                } else if (currentAction.equals(Action.LCD)) {
                    lcd(actions[1]);
                } else if (currentAction.equals(Action.EXIT)) {
                    break;
                } else {
                    actionNotFound();
                }
            }
        } else {
            System.out.println("Received " + args.length + " argument(s). " + "Expected 3 arguments: server, port, user name.");
        }
    }

    private static void get(Action action, String fileName) throws IOException {
        String url = "http://" + HOST + ":" + PORT + "/webhdfs/v1" + currentPath + fileName + "?user.name=" + USER_NAME + "&op=" + action.getRequestData();
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(action.getRequestType());
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.connect();
        print(connection);
        connection.disconnect();
    }

    private static void lls() {
        Stream.of(Objects.requireNonNull(new File(currentPathInSystem).listFiles()))
                .map(File::getName)
                .forEach(System.out::println);
    }

    private static void lcd(String path) {
        String tmpPath = editPath(path, currentPathInSystem);
        if (new File(tmpPath).exists()) {
            currentPathInSystem = tmpPath;
            System.out.println("cd to " + tmpPath);
        } else {
            System.out.println("path: " + tmpPath + " doesn't exist!");
        }
    }

    private static void mkdir(Action action, String dirName) throws IOException {
        String url = "http://" + HOST + ":" + PORT + "/webhdfs/v1" + currentPath + dirName + "?user.name=" + USER_NAME + "&op=" + action.getRequestData();
        connectAndPrintResult(action, url);
    }

    private static void delete(Action action, String fileName) throws IOException {
        String url = "http://" + HOST + ":" + PORT + "/webhdfs/v1" + currentPath + fileName + "?user.name=" + USER_NAME + "&op=" + action.getRequestData();
        connectAndPrintResult(action, url);
    }

    private static void ls(Action action) throws IOException {
        String url = "http://" + HOST + ":" + PORT + "/webhdfs/v1" + currentPath + "?user.name=" + USER_NAME + "&op=" + action.getRequestData();
        connectAndPrintResult(action, url);
    }

    private static void cd(String path) {
        currentPath = editPath(path, currentPath);
        System.out.println("Path after edit path = " + currentPath);
    }

    private static String editPath(String path, String currentPath) {
        if (path.equals("..")) {
            String[] tmpPath = currentPath.split("/");
            currentPath = "/";
            StringBuilder sb = new StringBuilder(currentPath);
            for (int i = 0; i < tmpPath.length - 1; i++) {
                sb.append(tmpPath[i]).append("/");
            }
            currentPath = sb.toString();
        } else {
            currentPath += path + "/";
        }
        if (currentPath.startsWith("//")) {
            currentPath = currentPath.substring(1);
        }
        return currentPath;
    }

    private static void putOrAppend(Action action, String localFileName, String fileToAppend) throws IOException {
        String url = "http://" + HOST + ":" + PORT + "/webhdfs/v1" + currentPath + localFileName + "?user.name=" + USER_NAME + "&op=" + action.getRequestData();
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(action.getRequestType());
        connection.setInstanceFollowRedirects(false);
        connection.connect();
        System.out.println("Location:" + connection.getHeaderField("Location"));
        String redirectUrl = null;
        if (connection.getResponseCode() == 307)
            redirectUrl = connection.getHeaderField("Location");
        connection.disconnect();

        if (redirectUrl != null) {
            connection = (HttpURLConnection) new URL(redirectUrl).openConnection();
            connection.setRequestMethod(action.getRequestType());
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            FileInputStream is;
            if (fileToAppend != null) {
                is = new FileInputStream(fileToAppend);
            } else {
                is = new FileInputStream(localFileName);
            }
            final int _SIZE = is.available();
            connection.setRequestProperty("Content-Length", "" + _SIZE);
            connection.setFixedLengthStreamingMode(_SIZE);
            connection.connect();
            OutputStream os = connection.getOutputStream();
            copy(is, os);
            is.close();
            os.close();
            System.out.println("File put successfully!");
            connection.disconnect();
        }
    }

    protected static void copy(InputStream input, OutputStream result) throws IOException {
        byte[] buffer = new byte[12288]; // 8K=8192 12K=12288 64K=
        int n;
        while (-1 != (n = input.read(buffer))) {
            result.write(buffer, 0, n);
            result.flush();
        }
        result.flush();
    }

    protected static void print(HttpURLConnection connection) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        System.out.println(response);
    }

    private static void connectAndPrintResult(Action action, String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(action.getRequestType());
        connection.setRequestProperty("Content-Type", "application/json");

        connection.connect();
        String resp = printLs(connection);
        connection.disconnect();

        System.out.println(resp);
    }

    private static String printLs(HttpURLConnection conn) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream is = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        is.close();

        return sb.toString();
    }

    private static void actionNotFound() {
        System.out.println("Action not found");
    }
}
