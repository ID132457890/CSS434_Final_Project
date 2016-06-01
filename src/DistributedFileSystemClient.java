import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Public API for Distributed File System Client functionality
 *
 */
public interface DistributedFileSystemClient extends Remote {

	public static final String RMI_SERVICE_NAME = "dfs_client";

	/**
	 * Invalidate the cached copy of the file currently open by the client.
	 * The DFS server calls this method if another client takes "ownership" of
	 * the file and intents to make changes to it. 
	 * @return Operation success - TRUE if the client was able to invalidate the
	 * cached copy, FALSE if it is not able to invalidate the cached copy.
	 * @throws RemoteException
	 */
	public boolean invalidate() throws RemoteException;
	
	/**
	 * Request upload of current cache contents back to the DFS server. This method
	 * is called by the DFS server to indicate that it wishes to obtain the latest
	 * contents of the file.
	 * @return Operation success - TRUE if the client accepts the writeback request,
	 * FALSE if it cannot upload contents.
	 * @throws RemoteException
	 */
	public boolean writeback() throws RemoteException;
	
}
