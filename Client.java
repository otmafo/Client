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

    public static void main(String args[]) {
        try {
            String name = JOptionPane.showInputDialog("请输入用户名：", "user1");
            if (name == null || name.length() == 0)
                return;
            String password = JOptionPane.showInputDialog("请输入密码：");
            if (password == null || password.length() == 0)
                return;

            Socket socket = new Socket(Setting.SERVER_IP, Setting.SERVER_PORT);
            new Client(name, password, socket);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
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
        scroll_text_history.setBounds(5, 5, 485, 290);
        this.add(scroll_text_history);
        docs_history = text_history.getDocument();

        text_in = new JTextArea();
        text_in.setBounds(5, 305, 485, 110);
        text_in.setLineWrap(true);
        text_in.setEnabled(false);
        JScrollPane scroll_text_in = new JScrollPane(text_in);
        scroll_text_in.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll_text_in.setBounds(5, 305, 485, 110);
        this.add(scroll_text_in);

        btn_send = new JButton("发送(alt+回车)");
        btn_send.setBounds(370, 420, 120, 25);
        btn_send.addActionListener(this);
        btn_send.setEnabled(false);
        btn_send.setMnemonic(KeyEvent.VK_ENTER);
        this.add(btn_send);

        label_destuser = new JLabel("发送给:");
        label_destuser.setBounds(215, 420, 50, 25);
        this.add(label_destuser);

        cbbox_destuser = new JComboBox<String>();
        cbbox_destuser.setBounds(265, 420, 100, 25);
        this.add(cbbox_destuser);

        StyleConstants.setForeground(attrset_cmd, Color.BLUE);
        StyleConstants.setBold(attrset_cmd, true);

        StyleConstants.setForeground(attrset_msg, Color.BLACK);
        System.out.println("Text input enabled: " + text_in.isEnabled());
        System.out.println("Send button enabled: " + btn_send.isEnabled());
        System.out.println("Destination user combo box visible: " + cbbox_destuser.isVisible());

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

            String msg = text_in.getText();
            insertText("[我] 对 [" + dest_user + "] 说: (" + getTime() + ")\n", attrset_cmd);
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
                        System.out.println("Received command: " + command); // 添加日志输出
                        if (command.equals(Setting.COMMAND_MSG)) {
                            String user_src = in.readLine();
                            String msg = in.readLine().replaceAll("\\\\n", "\n");
                            if (!user_src.equals(user_name)) {
                                insertText("[" + user_src + "] 对 [我] 说: (" + getTime() + ")\n", attrset_cmd);
                                insertText(msg + "\n", attrset_msg);
                            }
                        } else if (command.equals(Setting.COMMAND_USERLIST)) {
                            String user_list = in.readLine();
                            String user[] = user_list.split("\\|");
                            cbbox_destuser.removeAllItems();

                            cbbox_destuser.addItem("所有人");
                            for (int i = 0; i < user.length; i++) {
                                if (!user[i].equals(user_name))
                                    cbbox_destuser.addItem(user[i]);
                            }
                        } else if (command.equals(Setting.COMMAND_LOGIN_SUC)) {
                            String user_login = in.readLine();
                            insertText("[" + user_login + "] 进入了聊天室. (" + getTime() + ")\n", attrset_cmd);
                            SwingUtilities.invokeLater(() -> {
                                text_in.setEnabled(true);
                                btn_send.setEnabled(true);
                                System.out.println("Text input enabled after login: " + text_in.isEnabled());
                                System.out.println("Send button enabled after login: " + btn_send.isEnabled());
                                revalidate(); // 重新验证布局
                                repaint();    // 重绘界面
                            });
                            
                            // 显示更改昵称和密码的选项
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
                        } else if (command.equals(Setting.COMMAND_LOGIN_FAILED)) {
                            insertText(user_name + " 登陆失败\n", attrset_cmd);
                        } else if (command.equals(Setting.COMMAND_LOGOUT)) {
                            String user_logout = in.readLine();
                            insertText("[" + user_logout + "] 退出了聊天室. (" + getTime() + ")\n", attrset_cmd);
                        } else if (command.equals(Setting.COMMAND_REGISTER)) {
                            String email = JOptionPane.showInputDialog("请输入邮箱：");
                            if (email != null && !email.isEmpty()) {
                                out.println(Setting.COMMAND_REGISTER);
                                out.println(user_name);
                                out.println(email);
                                out.flush();
                            }
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