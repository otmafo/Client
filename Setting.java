public class Setting {
    // 服务器配置
    public static String SERVER_IP = "127.0.0.1";
    public static int SERVER_PORT = 5250;
    
    // 命令常量
    public static String COMMAND_LOGIN = "login";
    public static String COMMAND_LOGIN_SUC = "login_suc";
    public static String COMMAND_LOGIN_FAILED = "login_failed";
    public static String COMMAND_LOGOUT = "logout";
    public static String COMMAND_SENDMSG = "sendmsg";
    public static String COMMAND_USERLIST = "userlist";
    public static String COMMAND_MSG = "msg";
    
    // 新增命令
    public static String COMMAND_REGISTER = "register";
    public static String COMMAND_REGISTER_SUCCESS = "register_success";
    public static String COMMAND_REGISTER_FAILED = "register_failed";
    public static String COMMAND_UPDATE_PROFILE = "update_profile";
    public static String COMMAND_UPDATE_SUCCESS = "update_success";
    public static String COMMAND_UPDATE_FAILED = "update_failed";
    
    // 数据库配置
    public static String DB_URL = "jdbc:mysql://localhost:3306/mychet";
    public static String DB_USER = "0tmafo";
    public static String DB_PASSWORD = "20041217Xat";
    
    // 邮件配置
    public static String ADMIN_EMAIL = "xieaoting@qq.com";
    public static String SMTP_HOST = "smtp.qq.com";
    public static String SMTP_USER = "xieaoting@qq.com";
    public static String SMTP_PASSWORD = "tulcfungiqymdida";
    public static int SMTP_PORT = 587;
}