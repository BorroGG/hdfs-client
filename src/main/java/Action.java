public enum Action {
    MKDIR("mkdir", "MKDIRS&recursive=true", "PUT"),
    PUT("put", "CREATE&overwrite=true", "PUT"),
    GET("get", "OPEN", "GET"),
    APPEND("append", "APPEND", "POST"),
    DELETE("delete", "DELETE&recursive=true", "DELETE"),
    LS("ls", "LISTSTATUS", "GET"),
    CD("cd", "LISTSTATUS", "GET"),
    LLS("lls","", ""),
    LCD("lcd", "", ""),
    EXIT("exit", "", "");

    private final String commandName;
    private final String requestData;
    private final String requestType;

    Action(String commandName, String requestData, String requestType) {
        this.commandName = commandName;
        this.requestData = requestData;
        this.requestType = requestType;
    }

    public static Action getByName(String commandName) {
        for (Action item : values()) {
            if (item.commandName.equals(commandName)) {
                return item;
            }
        }
        return null;
    }

    public String getRequestData() {
        return requestData;
    }

    public String getRequestType() {
        return requestType;
    }
}
