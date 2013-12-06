
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class Server extends JFrame {    
	Model mod = new Model();
	JButton button = new JButton(); 
	JTextField field = new JTextField();  



	public static final int PORT = 9999;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JFrame ui = new Server();
				ui.pack();
				ui.setMinimumSize(ui.getSize());
				ui.setLocationRelativeTo(null);
				ui.setVisible(true);
				ui.setDefaultCloseOperation(EXIT_ON_CLOSE);
			}
		});
	}

	class Acceptor extends SwingWorker<Void, Integer> {
		@Override
		protected Void doInBackground() throws Exception {
			try (ServerSocket listen = new ServerSocket(PORT)) {
				listen.setSoTimeout(10 * 1000);
				while (!this.isCancelled()) {
					try {
						@SuppressWarnings("resource")
						//closed when connection terminates
						Socket client = listen.accept();
						Connection next = new Connection(client);
						synchronized (Server.this.connections) {
							Server.this.connections.add(next);
							publish(Server.this.connections.size());
						}
						next.start();
					} catch (SocketTimeoutException e) {
						System.out.println("Checking if cancelled");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				//nothing to do
			}
			return null;
		}

		@Override
		protected void process(List<Integer> chunks) {
			Server.this.clients.setText(chunks.get(0).toString());
		}
	}

	class Connection extends Thread {
		Socket client;
		boolean running = true;
		private PrintWriter writer;

		public Connection(Socket client) throws IOException {
			this.client = client;
			this.writer = new PrintWriter(client.getOutputStream(), true);
			this.writer.println(Server.this.model.getState());
		}

		@Override
		public void run() {
			try {
				BufferedReader read = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
				while (this.running) {
					String newState = read.readLine();
					System.out.println("Read: " + newState);
					if (newState != null) {
						synchronized (Server.this.model) {
							Server.this.model.setState(newState);
							int newNumState = Integer.parseInt(newState);
							mod.guess(newNumState);
						}
					} else {
						this.running = false;
					}
				}
			} catch (IOException e) {
				//nothing to do
			} finally {
				done();
				synchronized (Server.this.connections) {
					Server.this.connections.remove(this);
				}
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						Server.this.clients.setText(String.valueOf(Server.this.connections.size()));
					}
				});
			}
		}

		void done() {
			try {
				this.client.close();
			} catch (IOException e) {
				//nothing to do
			}
		}

		void update() {
			this.writer.println(Server.this.model.getState());
		}
	}

	private Acceptor acceptor;
	private JTextField clients, state;
	private List<Connection> connections = new ArrayList<>();
	private JPanel content;
	private Data model;

	public Server() {
		super("Server");
		this.model = new Data("?");
		this.model.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				//update gui
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						Server.this.state.setText(Server.this.model.getState());
					}
				});
				synchronized (Server.this.connections) {
					//update clients
					for (Connection c : Server.this.connections) {
						c.update();
					}
				}
			}
		});
		this.content = new JPanel(new GridLayout(1, 2));
		this.clients = field;
		this.state = new JTextField(this.model.getState());
		this.state.setEnabled(false);
		this.content.add(new JLabel("Chose number to guess:", SwingConstants.RIGHT)); 
		this.content.add(this.clients); 
		this.content.add(button);
		this.setContentPane(this.content);   

		button.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				String fieldText = field.getText();  
				int fieldNumber = Integer.parseInt(fieldText);   
				
				mod.setNumGuess(fieldNumber);  
				System.out.println(mod.getNumGuess());

			}

		});
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				synchronized (Server.this.connections) {
					for (Connection c : Server.this.connections) {
						c.done();
					}
				}
				Server.this.acceptor.cancel(true);
			}
		});
		this.acceptor = new Acceptor();
		this.acceptor.execute();
	}
}

class Data {
	public String state;
	private PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);

	public Data(String initalState) {
		this.state = initalState;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.propertySupport.addPropertyChangeListener(listener);
	}

	public String getState() {
		return this.state;
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.propertySupport.removePropertyChangeListener(listener);
	}

	public void setState(String state) {
		String old = this.state;
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		this.state = state;
		this.propertySupport.firePropertyChange("state", old, state);
	}
}