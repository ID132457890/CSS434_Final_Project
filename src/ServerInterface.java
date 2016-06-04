import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Public API for Distributed File System Server functionality
 *
 */
public interface ServerInterface extends Remote {

	public static final String RMI_SERVICE_NAME = "fileserver";

	// file access modes specified by FileClients
	public static final String READ_MODE = "r";
	public static final String WRITE_MODE = "w";

	/**
	 * Download a file from the DFS server
	 * @param clientIPName The IP address or hostname of the client requesting the file
	 * @param filename The path/name of the file being requested
	 * @param mode The access mode of the file. If "r", client does not intend to make changes
	 * to the file. If "w", the client intends to take ownership of the file and make changes
	 * to it in the future. The client sets this mode to indicate how it plans to utilize the file.
	 * @return The contents of the file requested
	 * @throws RemoteException
	 */
	public FileContents download( String clientIPName, String filename, String mode ) throws RemoteException;
	
	/**
	 * Upload new contents for the specified file. The file must be owned by the client
	 * (in WRITE_SHARED mode) before changes are accepted by the server.
	 * @param clientIPName The IP address or hostname of the client (which must be the owner)
	 * @param filename The path/name of the file being updated
	 * @param contents The new contents of the file
	 * @return Operation success (TRUE) or failure (FALSE). If the return from this method is
	 * FALSE, the client MUST assume that the file contents HAVE NOT been updated.
	 * @throws RemoteException
	 */
	public boolean upload( String clientIPName, String filename, FileContents contents ) throws RemoteException;
	
}
