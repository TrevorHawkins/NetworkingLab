
import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class Client extends JFrame {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					JFrame ui = new Client();
					ui.pack();
					ui.setMinimumSize(ui.getSize());
					ui.setLocationRelativeTo(null);
					ui.setVisible(true);
					ui.setDefaultCloseOperation(EXIT_ON_CLOSE);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private Socket connection;
	private JButton send;
	private JTextField state; 
	private JLabel text;
	private PrintWriter write;

	long id = System.currentTimeMillis();

	ActionListener tx = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String text = Client.this.state.getText();
			System.out.println("Writing:" + text);
			Client.this.write.println(text);
		}
	};

	public Client() throws IOException {
		super("Client");
		this.connection = new Socket("localhost", Server.PORT);
		this.write = new PrintWriter(this.connection.getOutputStream(), true);
		this.state = new JTextField(20); 
		this.text = new JLabel("Stuff");
		this.send = new JButton("Send");
		final SwingWorker<Void, String> reader = new SwingWorker<Void, String>() {
			@Override
			protected Void doInBackground() throws Exception {
				try (BufferedReader read = new BufferedReader(new InputStreamReader(Client.this.connection.getInputStream()));) {
					while (!this.isCancelled()) {
						String newState = read.readLine();
						if (newState == null) this.cancel(true);
						else this.publish(newState);
					}
				} catch (Exception e) {
					//nothing to do
				}
				System.out.println("Done");
				Client.this.state.setEnabled(false); 
				Client.this.text.setEnabled(false);
				Client.this.send.setEnabled(false);
				setTitle("Closed");
				return null;
			}

			@Override
			protected void process(List<String> chunks) {
				Client.this.state.setText(chunks.get(0)); 
				Client.this.text.setText(chunks.get(0));
			}

		};
		this.state.addActionListener(this.tx); 
		this.send.addActionListener(this.tx);
		getContentPane().add(this.state, BorderLayout.NORTH);   
		getContentPane().add(this.text, BorderLayout.CENTER);
		getContentPane().add(this.send, BorderLayout.SOUTH);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				reader.cancel(true);
				try {
					Client.this.connection.close();
				} catch (IOException e1) {
					//nothing to do
				}
			}
		});
		reader.execute();
	}
}