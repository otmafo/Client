import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class Client extends JFrame implements ActionListener {
    private static final long serialVersionUID = 2389760719589425440L;
    private String user_name;
    private String password;
    private Socket socket;
    JTextPane text_history;
    Document docs_history;
    JTextArea text_in;
    JButton btn_send;
    JLabel label_destuser;
    JComboBox<String> cbbox_destuser;
    SimpleAttributeSet attrset_cmd = new SimpleAttributeSet();
    SimpleAttributeSet attrset_msg = new SimpleAttributeSet();
    PrintWriter out = null;
    private Map<String, String> nicknameMap = new HashMap<>(); // 用户名->昵称映射

    public static void main(String args[]) {
        new LoginOrRegisterFrame();
    }

    public Client(String _user_name, String _password, Socket _socket) throws IOException {
        super("聊天室 - " + _user_name);
        this.setLayout(null);
        this.setBounds(200, 200, 500, 480);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.addWindowListener(new CloseHandler());

        user_name = _user_name;
        password = _password;
        socket = _socket;

        text_history = new JTextPane();
        text_history.setBounds(5, 5, 485, 290);
        text_history.setEditable(false);
        JScrollPane scroll_text_history = new JScrollPane(text_history);
        scroll_text_history.setBounds(5, 5, 485, 250);
        this.add(scroll_text_history);
        docs_history = text_history.getDocument();

        text_in = new JTextArea();
        text_in.setBounds(5, 280, 485, 110);
        text_in.setLineWrap(true);
        text_in.setEnabled(false);
        JScrollPane scroll_text_in = new JScrollPane(text_in);
        scroll_text_in.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll_text_in.setBounds(5, 260, 485, 110);
        this.add(scroll_text_in);

        btn_send = new JButton("发送(alt+回车)");
        btn_send.setBounds(370, 370, 120, 25);
        btn_send.addActionListener(this);
        btn_send.setEnabled(false);
        btn_send.setMnemonic(KeyEvent.VK_ENTER);
        this.add(btn_send);

        label_destuser = new JLabel("发送给:");
        label_destuser.setBounds(215, 370, 50, 25);
        this.add(label_destuser);

        cbbox_destuser = new JComboBox<String>();
        cbbox_destuser.setBounds(265, 370, 100, 25);
        this.add(cbbox_destuser);

        StyleConstants.setForeground(attrset_cmd, Color.BLUE);
        StyleConstants.setBold(attrset_cmd, true);

        StyleConstants.setForeground(attrset_msg, Color.BLACK);

        this.setVisible(true);
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        new ListenService(user_name, password, socket).start();
    }

    class CloseHandler extends WindowAdapter {
        public void windowClosing(final WindowEvent ev) {
            try {
                out.println(Setting.COMMAND_LOGOUT);
                out.flush();
                out.close();
                socket.close();
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev.getActionCommand().equals("发送(alt+回车)")) {
            String dest_user = (String) cbbox_destuser.getSelectedItem();
            if (dest_user == null)
                return;

            String destNickname = nicknameMap.getOrDefault(dest_user, dest_user);
            String msg = text_in.getText();
            
            insertText("[我] 对 [" + destNickname + "] 说: (" + getTime() + ")\n", attrset_cmd);
            insertText(msg + "\n", attrset_msg);

            out.println(Setting.COMMAND_SENDMSG);
            out.println(dest_user);
            out.println(msg.replaceAll("\\n", "\\\\n"));
            out.flush();

            text_in.setText("");
        }
    }

    public void insertText(String str, SimpleAttributeSet attrset) {
        try {
            docs_history.insertString(docs_history.getLength(), str, attrset);
        } catch (BadLocationException ble) {
            System.out.println("BadLocationException:" + ble);
        }
    }

    public String getTime() {
        SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss");
        return df.format(new Date());
    }

    class ListenService extends Thread {
        private Socket socket;
        private String user_name;
        private String password;
        private BufferedReader in = null;
        private PrintWriter out = null;

        public ListenService(String _user_name, String _password, Socket _socket) {
            try {
                user_name = _user_name;
                password = _password;
                socket = _socket;
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                // 发送登陆命令
                String command = Setting.COMMAND_LOGIN + "\n"
                        + user_name + "\n"
                        + hashPassword(password) + "\n";
                out.print(command);
                out.flush();

                while (true) {
                    if ((command = in.readLine()) != null) {
                        System.out.println("Received command: " + command);
                        if (command.equals(Setting.COMMAND_MSG)) {
                            String user_src = in.readLine();
                            String srcNickname = in.readLine(); // 发送者昵称
                            String msg = in.readLine().replaceAll("\\\\n", "\n");
                            if (!user_src.equals(user_name)) {
                                insertText("[" + srcNickname + "] 对 [我] 说: (" + getTime() + ")\n", attrset_cmd);
                                insertText(msg + "\n", attrset_msg);
                            }
                        } else if (command.equals(Setting.COMMAND_USERLIST)) {
                            String user_list = in.readLine();
                            String userPairs[] = user_list.split("\\|");
                            cbbox_destuser.removeAllItems();
                            nicknameMap.clear();

                            cbbox_destuser.addItem("所有人");
                            for (String pair : userPairs) {
                                if (pair.isEmpty()) continue;
                                String[] parts = pair.split(":", 2);
                                if (parts.length == 2) {
                                    String username = parts[0];
                                    String nickname = parts[1];
                                    nicknameMap.put(username, nickname);
                                    if (!username.equals(user_name)) {
                                        cbbox_destuser.addItem(username);
                                    }
                                }
                            }
                        } else if (command.equals(Setting.COMMAND_LOGIN_SUC)) {
                            String user_login = in.readLine();
                            String nickname = in.readLine(); // 新昵称
                            nicknameMap.put(user_login, nickname);
                            
                            if (user_login.equals(user_name)) {
                                // 自己登录成功
                                SwingUtilities.invokeLater(() -> {
                                    text_in.setEnabled(true);
                                    btn_send.setEnabled(true);
                                    label_destuser.setVisible(true);
                                    cbbox_destuser.setVisible(true);
                                    System.out.println("Text input enabled after login: " + text_in.isEnabled());
                                    System.out.println("Send button enabled after login: " + btn_send.isEnabled());
                                    revalidate();
                                    repaint();
                                });
                                
                                // 添加设置菜单
                                JMenuItem changeNicknameItem = new JMenuItem("更改昵称");
                                changeNicknameItem.addActionListener(e -> {
                                    String newNickname = JOptionPane.showInputDialog("请输入新的昵称：");
                                    if (newNickname != null && !newNickname.isEmpty()) {
                                        out.println(Setting.COMMAND_CHANGE_NICKNAME);
                                        out.println(newNickname);
                                        out.flush();
                                    }
                                });

                                JMenuItem changePasswordItem = new JMenuItem("更改密码");
                                changePasswordItem.addActionListener(e -> {
                                    String newPassword = JOptionPane.showInputDialog("请输入新的密码：");
                                    if (newPassword != null && !newPassword.isEmpty()) {
                                        out.println(Setting.COMMAND_CHANGE_PASSWORD);
                                        out.println(hashPassword(newPassword));
                                        out.flush();
                                    }
                                });

                                JMenuBar menuBar = new JMenuBar();
                                JMenu settingsMenu = new JMenu("设置");
                                settingsMenu.add(changeNicknameItem);
                                settingsMenu.add(changePasswordItem);
                                menuBar.add(settingsMenu);
                                setJMenuBar(menuBar);
                            } else {
                                insertText("[" + nickname + "] 进入了聊天室. (" + getTime() + ")\n", attrset_cmd);
                            }
                        } else if (command.equals(Setting.COMMAND_LOGIN_FAILED)) {
                            insertText(user_name + " 登陆失败\n", attrset_cmd);
                        } else if (command.equals(Setting.COMMAND_LOGOUT)) {
                            String user_logout = in.readLine();
                            String nickname = in.readLine(); // 退出用户昵称
                            insertText("[" + nickname + "] 退出了聊天室. (" + getTime() + ")\n", attrset_cmd);
                        } else if (command.equals(Setting.COMMAND_REGISTER)) {
                            String email = JOptionPane.showInputDialog("请输入邮箱：");
                            if (email != null && !email.isEmpty()) {
                                out.println(Setting.COMMAND_REGISTER);
                                out.println(user_name);
                                out.println(email);
                                out.flush();
                            }
                        } else if (command.equals(Setting.COMMAND_NICKNAME_UPDATED)) {
                            String updatedUser = in.readLine();
                            String newNickname = in.readLine();
                            nicknameMap.put(updatedUser, newNickname);
                            insertText("用户 [" + updatedUser + "] 的昵称已更新为: " + newNickname + " (" + getTime() + ")\n", attrset_cmd);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String hashPassword(String password) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(password.getBytes());
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}

class LoginOrRegisterFrame extends JFrame implements ActionListener {
    private JTextField tfUsername;
    private JPasswordField pfPassword;
    private JButton btnLogin;
    private JButton btnRegister;

    public LoginOrRegisterFrame() {
        setTitle("登录/注册");
        setSize(350, 200);
        setLayout(new GridLayout(4, 2, 10, 10));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 用户名输入
        add(new JLabel("用户名:"));
        tfUsername = new JTextField();
        add(tfUsername);

        // 密码输入
        add(new JLabel("密码:"));
        pfPassword = new JPasswordField();
        add(pfPassword);

        // 登录按钮
        btnLogin = new JButton("登录");
        btnLogin.addActionListener(this);
        add(btnLogin);

        // 注册按钮
        btnRegister = new JButton("注册");
        btnRegister.addActionListener(this);
        add(btnRegister);

        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String username = tfUsername.getText().trim();
        String password = new String(pfPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码不能为空");
            return;
        }

        if (e.getSource() == btnLogin) {
            // 登录逻辑
            try {
                Socket socket = new Socket(Setting.SERVER_IP, Setting.SERVER_PORT);
                new Client(username, password, socket);
                dispose(); // 关闭登录窗口
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "连接服务器失败: " + ex.getMessage());
            }
        } else if (e.getSource() == btnRegister) {
            // 注册逻辑
            String email = JOptionPane.showInputDialog(this, "请输入邮箱地址:");
            if (email != null && !email.isEmpty()) {
                try {
                    // 连接到服务器发送注册请求
                    Socket socket = new Socket(Setting.SERVER_IP, Setting.SERVER_PORT);
                    PrintWriter out = new PrintWriter(
                        new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())), true);
                    
                    out.println(Setting.COMMAND_REGISTER);
                    out.println(username);
                    out.println(email);
                    out.flush();
                    
                    JOptionPane.showMessageDialog(this, "注册请求已发送，请等待管理员处理");
                    socket.close();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "发送注册请求失败: " + ex.getMessage());
                }
            }
        }
    }
}