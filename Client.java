import java.awt.Color;
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
 
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
 
public class Client extends JFrame implements ActionListener {
	private static final long serialVersionUID = 2389760719589425440L;
	private String user_name;
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
			String name = JOptionPane.showInputDialog("请输入用户名：","user1");
			if(name == null || name.length() == 0)
				return;
			
			Socket socket = new Socket(Setting.SERVER_IP, Setting.SERVER_PORT);
			new Client(name, socket);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	public Client(String _user_name, Socket _socket) throws IOException {
		super("聊天室 - " + _user_name);
		this.setLayout(null);
		this.setBounds(200, 200, 500, 480);
		this.setResizable(false);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.addWindowListener(new CloseHandler());
		
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
		
		public ListenService(String _user_name, Socket _socket) {
			try {
				user_name = _user_name;
				socket = _socket;
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void run() {
			try {
				String command = "";
				
				//发送登陆命令
				command = Setting.COMMAND_LOGIN + "\n"
						+ user_name + "\n";
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
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}