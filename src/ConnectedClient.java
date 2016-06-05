import java.rmi.Naming;
import java.rmi.RemoteException;

/**
 * Connected Client is a container for information related to a client that
 * is being tracked by the Server, and also serves as an RMI proxy to the
 * client.
 *
 */
public class ConnectedClient {

	private static final String RMI_URL_PREFIX = "rmi://";
	
	private String clientIPName;
	private ServerFileState fileAccessMode;
	private ClientInterface client;
	
	/**
	 * Constructor for ConnectedClient, used to instantiate and init the client RMI proxy
	 * @param clientIPName The hostname/IP address of the client
	 * @param port The port number the client is receiving RMI requests on
	 */
	public ConnectedClient(String clientIPName, int port) {
		
		this.clientIPName = clientIPName;
		
		// connect to the remote client
		try {
			
			String RMIUrl = RMI_URL_PREFIX + clientIPName + ":" + port + "/" + FileServer.CLIENT_RMI_SERVICE_NAME;
			
			if (FileServer.DEBUG_MODE) System.out.println("Attempting to connect to client at: " + RMIUrl);

			client = (ClientInterface) Naming.lookup(RMIUrl);

			if (FileServer.DEBUG_MODE) System.out.println("Client connection made!");
			
		} catch (Exception e) {

			System.err.println("Exception connecting to client (" + clientIPName + "," + port + "): " + e.getMessage());
			
			// negative return code is an error in execution
			System.exit(-1);		
			
		}
		
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
	 * Instruct the client to mark it's copy of the file as invalid
	 * @return Operation success (TRUE) or failure (FALSE)
	 */
	public boolean invalidate() {

		if (FileServer.DEBUG_MODE) System.out.println("Attempting to invalidate cache on client: " + clientIPName);

		try {
			
			boolean success = client.invalidate();
			
			if (FileServer.DEBUG_MODE) System.out.println("Client cache invalidation attempt returned " + success);

			return success;
			
		} catch (RemoteException e) {

			if (FileServer.DEBUG_MODE) System.out.println("Exception caught trying to invalidate client cache:" + e.getMessage());
			return false;
			
		}

	}
	
	/**
	 * Set the access mode for the file the client is using
	 * @param fileAccessMode The current access mode
	 */
	public void setFileAccessMode(ServerFileState fileAccessMode) {
		this.fileAccessMode = fileAccessMode;
	}

	/**
	 * Instruct the client to upload it's copy of the file
	 * @return Operation success (TRUE) or failure (FALSE)
	 */
	public boolean writeback( ) {

		if (FileServer.DEBUG_MODE) System.out.println("Sending writeback request to client: " + clientIPName);

		try {
			
			boolean success = client.writeback();
			
			if (FileServer.DEBUG_MODE) System.out.println("Writeback request returned " + success);

			return success;
			
		} catch (RemoteException e) {

			if (FileServer.DEBUG_MODE) System.out.println("Exception caught trying to request client writeback:" + e.getMessage());
			return false;
		
		}
		
	}
	
}