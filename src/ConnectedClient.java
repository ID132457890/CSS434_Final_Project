/**
 * Connected Client is a container for information related to a client that
 * is being tracked by the Server
 *
 */
public class ConnectedClient {

	private String clientIPName;
	private ServerFileState fileAccessMode;

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
	 * Set the client hostname/IP address
	 * @param clientIPName The network address of the client
	 */
	public void setClientIPName(String clientIPName) {
		this.clientIPName = clientIPName;
	}

	/**
	 * Set the access mode for the file the client is using
	 * @param fileAccessMode The current access mode
	 */
	public void setFileAccessMode(ServerFileState fileAccessMode) {
		this.fileAccessMode = fileAccessMode;
	}
	
}