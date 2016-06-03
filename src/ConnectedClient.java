import java.rmi.Naming;
import java.rmi.RemoteException;

/**
 * Connected Client is a container for information related to a client that
 * is being tracked by the Server
 *
 */
public class ConnectedClient {

	private static final String RMI_URL_PREFIX = "rmi://";
	
	private String clientIPName;
	private ServerFileState fileAccessMode;
	private ClientInterface client;
	
	public ConnectedClient(String clientIPName, int port) {
		
		this.clientIPName = clientIPName;
		
		// connect to the remote client
		try {
			
			client = (ClientInterface) Naming.lookup(RMI_URL_PREFIX + clientIPName + ":" + port + FileServer.CLIENT_RMI_SERVICE_NAME);
		
		} catch (Exception e) {

			System.err.println("Exception connecting to client (" + clientIPName + "," + port + "): " + e.getMessage());
			
			// negative return code is an error in execution
			System.exit(-1);		}
		
	}

	/**
	 * Get the client hostname/IP address
	 * @return The client's network address
	 */
	public String getClientIPName() {
		return clientIPName;
	}

	/**
	 * For the file this client is working with, the access mode
	 * @return The access mode of the file the client is using
	 */
	public ServerFileState getFileAccessMode() {
		return fileAccessMode;
	}

	/**
	 * Set the access mode for the file the client is using
	 * @param fileAccessMode The current access mode
	 */
	public void setFileAccessMode(ServerFileState fileAccessMode) {
		this.fileAccessMode = fileAccessMode;
	}
	
	/**
	 * Instruct the client to mark it's copy of the file as invalid
	 * @return Operation success (TRUE) or failure (FALSE)
	 */
	public boolean invalidate() {
		
		try {
			return client.invalidate();
		} catch (RemoteException e) {
			return false;
		}

	}

	/**
	 * Instruct the client to upload it's copy of the file
	 * @return Operation success (TRUE) or failure (FALSE)
	 */
	public boolean writeback( ) {

		try {
			return client.writeback();
		} catch (RemoteException e) {
			return false;
		}
		
	}
	
}