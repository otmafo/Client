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
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
 
public class Client extends JFrame implements ActionListener {
	private static final long serialVersionUID = 2389760719589425440L;
	private String user_name;
	private Socket socket;
	private String email;
    private JMenuItem menuUpdateProfile;
    private JMenuBar menuBar;
    private JMenu menuAccount;
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
            // 创建登录/注册选择对话框
            Object[] options = {"登录", "注册"};
            int choice = JOptionPane.showOptionDialog(null, 
                "请选择操作", "聊天室",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
            
            if (choice == 0) { // 登录
                JPanel loginPanel = new JPanel(new GridLayout(3, 2));
                JTextField usernameField = new JTextField(15);
                JPasswordField passwordField = new JPasswordField(15);
                
                loginPanel.add(new JLabel("用户名:"));
                loginPanel.add(usernameField);
                loginPanel.add(new JLabel("密码:"));
                loginPanel.add(passwordField);
                
                int result = JOptionPane.showConfirmDialog(null, loginPanel, 
                    "登录", JOptionPane.OK_CANCEL_OPTION);
                
                if (result == JOptionPane.OK_OPTION) {
                    String username = usernameField.getText();
                    String password = new String(passwordField.getPassword());
                    
                    Socket socket = new Socket(Setting.SERVER_IP, Setting.SERVER_PORT);
                    new Client(username, password, socket);
                }
            } else if (choice == 1) { // 注册
                JPanel registerPanel = new JPanel(new GridLayout(4, 2));
                JTextField usernameField = new JTextField(15);
                JTextField emailField = new JTextField(15);
                
                registerPanel.add(new JLabel("用户名:"));
                registerPanel.add(usernameField);
                registerPanel.add(new JLabel("邮箱:"));
                registerPanel.add(emailField);
                
                int result = JOptionPane.showConfirmDialog(null, registerPanel, 
                    "注册", JOptionPane.OK_CANCEL_OPTION);
                
                if (result == JOptionPane.OK_OPTION) {
                    String username = usernameField.getText();
                    String email = emailField.getText();
                    
                    Socket socket = new Socket(Setting.SERVER_IP, Setting.SERVER_PORT);
                    registerUser(username, email, socket);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

	// 注册用户方法
    private static void registerUser(String username, String email, Socket socket) {
        try {
            PrintWriter out = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            
            // 发送注册命令
            out.println(Setting.COMMAND_REGISTER);
            out.println(username);
            out.println(email);
            out.flush();
            
            // 等待服务器响应
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            String response = in.readLine();
            
            if (Setting.COMMAND_REGISTER_SUCCESS.equals(response)) {
                JOptionPane.showMessageDialog(null, 
                    "注册请求已发送给管理员，请等待账户激活", 
                    "注册成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                String errorMsg = in.readLine();
                JOptionPane.showMessageDialog(null, 
                    "注册失败: " + errorMsg, 
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	public Client(String _user_name, String password, Socket _socket) throws IOException {
        super("聊天室 - " + _user_name);
		this.setLayout(null);
		this.setBounds(200, 200, 500, 480);
		this.setResizable(false);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.addWindowListener(new CloseHandler());
		
		// 添加菜单栏
        menuBar = new JMenuBar();
        menuAccount = new JMenu("账户");
        
        menuUpdateProfile = new JMenuItem("修改资料");
        menuUpdateProfile.addActionListener(this);
        menuAccount.add(menuUpdateProfile);
        
        menuBar.add(menuAccount);
        this.setJMenuBar(menuBar);

		user_name = _user_name;
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
		
		this.setVisible(true);
		out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
		new ListenService(user_name, socket).start();
		// 发送登录信息（包含密码）
        out = new PrintWriter(new BufferedWriter(
            new OutputStreamWriter(socket.getOutputStream())),true);
        new ListenService(user_name, password, socket).start();
	}
	
	class CloseHandler extends WindowAdapter {
		//窗口关闭时执行
		public void windowClosing(final WindowEvent ev) {
			try {
				// 向服务器发送退出命令
				out.println(Setting.COMMAND_LOGOUT);
				// 刷新输出流
				out.flush();
				out.close();
				socket.close();
				System.exit(0);
			} catch (IOException e) {
				// 捕获异常并打印堆栈信息
				e.printStackTrace();
				// 退出程序
				System.exit(0);
			}
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent ev) {
		if(ev.getActionCommand()=="发送(alt+回车)") {
			String dest_user = (String)cbbox_destuser.getSelectedItem();
			if(dest_user == null)
				return;
			
			String msg = text_in.getText();
			insertText("[我] 对 [" + dest_user +  "] 说: (" + getTime() + ")\n", attrset_cmd);
			insertText(msg + "\n", attrset_msg);
			
			out.println(Setting.COMMAND_SENDMSG);
			out.println(dest_user);
			out.println(msg.replaceAll("\\n", "\\\\n"));
			out.flush();
			
			text_in.setText("");
		}else if (ev.getSource() == menuUpdateProfile) {
            showUpdateProfileDialog();
        }
	}

	// 显示更新资料对话框
    private void showUpdateProfileDialog() {
        JPanel updatePanel = new JPanel(new GridLayout(3, 2));
        JTextField nicknameField = new JTextField(15);
        JPasswordField newPasswordField = new JPasswordField(15);
        JPasswordField confirmPasswordField = new JPasswordField(15);
        
        updatePanel.add(new JLabel("新昵称:"));
        updatePanel.add(nicknameField);
        updatePanel.add(new JLabel("新密码:"));
        updatePanel.add(newPasswordField);
        updatePanel.add(new JLabel("确认密码:"));
        updatePanel.add(confirmPasswordField);
        
        int result = JOptionPane.showConfirmDialog(this, updatePanel, 
            "修改资料", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String newNickname = nicknameField.getText();
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            
            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, 
                    "两次输入的密码不一致", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // 发送更新命令
            out.println(Setting.COMMAND_UPDATE_PROFILE);
            out.println(newNickname);
            out.println(newPassword);
            out.flush();
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
		private BufferedReader in = null;
		private PrintWriter out = null;
		private String password;
		
		public ListenService(String _user_name, String _password, Socket _socket) {
			try {
				user_name = _user_name;
				socket = _socket;
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			password = _password;
		}
		
		public void run() {
			try {
				String command = "";
				
				//发送登陆命令
				command = Setting.COMMAND_LOGIN + "\n"
                        + user_name + "\n"
                        + password + "\n";
				out.print(command);
				out.flush();
				
				while(true) {
					if((command = in.readLine()) != null) {
						if(command.equals(Setting.COMMAND_MSG)) {
							String user_src = in.readLine();
							String msg = in.readLine().replaceAll("\\\\n", "\n");
							if(!user_src.equals(user_name)) {
								insertText("[" + user_src + "] 对 [我] 说: (" + getTime() + ")\n", attrset_cmd);
								insertText(msg + "\n", attrset_msg);
							}
						} else if(command.equals(Setting.COMMAND_USERLIST)) {
							String user_list = in.readLine();
							String user[] = user_list.split("\\|");
							cbbox_destuser.removeAllItems();
							
							cbbox_destuser.addItem("所有人");
							for(int i=0; i<user.length; i++) {
								if(!user[i].equals(user_name))
									cbbox_destuser.addItem(user[i]);
							}
						} else if(command.equals(Setting.COMMAND_LOGIN_SUC)) {
							String user_login = in.readLine();
							insertText("[" + user_login + "] 进入了聊天室. (" + getTime() + ")\n", attrset_cmd);
							text_in.setEnabled(true);
							btn_send.setEnabled(true);
						} else if(command.equals(Setting.COMMAND_LOGIN_FAILED)) {
							insertText(user_name + "登陆失败\n", attrset_cmd);
						} else if(command.equals(Setting.COMMAND_LOGOUT)) {
							String user_logout = in.readLine();
							insertText("[" + user_logout + "] 退出了聊天室. (" + getTime() + ")\n", attrset_cmd);
						}
						if (command.equals(Setting.COMMAND_UPDATE_SUCCESS)) {
                            JOptionPane.showMessageDialog(Client.this, 
                                "资料更新成功", "成功", JOptionPane.INFORMATION_MESSAGE);
                        } else if (command.equals(Setting.COMMAND_UPDATE_FAILED)) {
                            String error = in.readLine();
                            JOptionPane.showMessageDialog(Client.this, 
                                "更新失败: " + error, "错误", JOptionPane.ERROR_MESSAGE);
                        }
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}