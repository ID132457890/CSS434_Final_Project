import java.rmi.Remote;

/**
 * Public API for Distributed File System Server functionality
 *
 */
public interface DistributedFileSystemServer extends Remote {

	/**
	 * Download a file from the DFS server
	 * @param clientIPName The IP address or hostname of the client requesting the file
	 * @param filename The path/name of the file being requested
	 * @param mode The access mode of the file. If "r", client does not intend to make changes
	 * to the file. If "w", the client intends to take ownership of the file and make changes
	 * to it in the future. The client sets this mode to indicate how it plans to utilize the file.
	 * @return The contents of the file requested
	 */
	public FileContents download( String clientIPName, String filename, String mode );
	
	/**
	 * Upload new contents for the specified file. The file must be owned by the client
	 * (in WRITE_SHARED mode) before changes are accepted by the server.
	 * @param clientIPName The IP address or hostname of the client (which must be the owner)
	 * @param filename The path/name of the file being updated
	 * @param contents The new contents of the file
	 * @return Operation success (TRUE) or failure (FALSE). If the return from this method is
	 * FALSE, the client MUST assume that the file contents HAVE NOT been updated.
	 */
	public boolean upload( String clientIPName, String filename, FileContents contents );
	
}
