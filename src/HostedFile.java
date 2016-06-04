import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * HostedFile represents a file that one or more Clients have requested. Also, the 
 * FileServer uses this class to associate a locally-cached file with the client(s)
 * that have requested it.
 *
 */
public class HostedFile {

	// the actual file as it exists in the local filesystem
	private Path file = null;
	
	// the representation as given to clients upon request/update
	private FileContents fileContents = null;
	
	// the filename, as requested by clients
	private String filename;
	
	// a collection of clients that are currently using this file
	private Map<String, ConnectedClient> clients = new HashMap<String, ConnectedClient>();
	
	// overall state of the file, initially "not shared"
	private ServerFileState fileState = ServerFileState.NOT_SHARED;
	
	public HostedFile(String filename) {
		
		// how the client referred to the file
		this.filename = filename;
		
		// get the (real) reference to the filesystem
		file = Paths.get(filename);
		
		// read file contents if possible
		try {

			fileContents = new FileContents(Files.readAllBytes(file));

		} catch (IOException e) {

			// unable to read file contents!
			
		}
			
	}

	/**
	 * Get the RMI-friendly representation of the file, for transmission to clients
	 * @return The RMI file representation
	 */
	public FileContents getFileContents() {
		return fileContents;
	}

	/**
	 * Get the name of the file
	 * @return The file name
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Update the file, using the RMI representation from a client
	 * @param fileContents The new version of the file
	 */
	public void setFileContents(FileContents fileContents) {
	
		try {
			
			// write back changes to file on filesystem
			Files.write(file, fileContents.get(), new OpenOption[]{});

			// set local file contents
			this.fileContents = fileContents;
			
			// all clients must invalidate their cached copies
			boolean invalidationSuccess = invalidateClients();
			if (!invalidationSuccess) return;
			
			// this file is not shared any more
			fileState = ServerFileState.NOT_SHARED;

		} catch (IOException e) {

			// TODO Auto-generated catch block
			e.printStackTrace();
		
		}
	
	}
	
	/**
	 * Get the access state of this file
	 * @return The current state of the file
	 */
	public ServerFileState getState() {
		return fileState;
	}
	
	/**
	 * Get the "owner" of this file. The file owner is the one (and only one!) that has
	 * requested write privileges.
	 * @return The owner of this file, or null if not owned
	 */
	public ConnectedClient getOwner() {
		
		for (Entry<String, ConnectedClient> client : clients.entrySet()) {
			
			ServerFileState clientState = client.getValue().getFileAccessMode();

			// "write_shared" or "ownership_change" is an indication of ownership
			if (clientState == ServerFileState.WRITE_SHARED || clientState == ServerFileState.OWNERSHIP_CHANGE) {
				return client.getValue();
			}
			
		}
		
		// no owner exists
		return null;
		
	}
	
	/**
	 * A client would like to be registered as a reader only
	 * @param clientIPName The hostname or IP address of the client
	 * @param port The port number the client is accepting requests on
	 */
	public void registerReader(String clientIPName, int port) {
		
		// update file state if this is the first client to register
		if (fileState == ServerFileState.NOT_SHARED) fileState = ServerFileState.READ_SHARED;
		
		// register this client, read-only
		registerClient(clientIPName, port, ServerFileState.READ_SHARED);
		
	}
	
	/**
	 * A client would like to be registered as "owner" of the file
	 * @param clientIPName The hostname or IP address of the owning client
	 * @param port The port number the client is accepting requests on
	 * @return Operation success (TRUE) or failure (FALSE)
	 */
	public boolean registerOwner(String clientIPName, int port) {
		
		// is there an owner already?
		ConnectedClient owner = getOwner();
		if (owner == null) {
			
			// this client is now the owner
			fileState = ServerFileState.WRITE_SHARED;
			
			// register this client, write mode
			registerClient(clientIPName, port, ServerFileState.WRITE_SHARED);
			
			// success!
			return true;
			
		}
		
		// this client is not the owner
		fileState = ServerFileState.OWNERSHIP_CHANGE;
		
		// tell the owner to write back its' changes
		boolean writebackSuccess = owner.writeback();
		if (!writebackSuccess) return false;	// operation failed, file status indeterminate (retain)
		
		// take ownership now
		fileState = ServerFileState.WRITE_SHARED;
		
		// register this client, write mode
		registerClient(clientIPName, port, ServerFileState.WRITE_SHARED);
		
		// success!
		return true;
		
	}
	
	private void registerClient(String clientIPName, int port, ServerFileState clientFileState) {
		
		// if client is already using this file, get it
		ConnectedClient client = clients.get(clientIPName);
		
		// client not using this file
		if (client == null) {
			
			client = new ConnectedClient(clientIPName, port);
			clients.put(clientIPName, client);
			
			
		}

		// set access mode
		client.setFileAccessMode(clientFileState);

	}

	/**
	 * Remove a client associated with this file
	 * @param clientIPName The hostname/IP address of the client to de-register
	 */
	public void deRegisterClient(String clientIPName) {
		clients.remove(clientIPName);
	}
	
	private boolean invalidateClients() {

		boolean operationSuccess = true;
		
		// iterate over collection of connected clients
		for (ConnectedClient fileClient : clients.values()) {
			
			// ... invalidate each
			boolean invalidateSuccess = fileClient.invalidate();
			
			// update access mode for this client
			if (!invalidateSuccess) {
				fileClient.setFileAccessMode(ServerFileState.NOT_SHARED);
				operationSuccess = false;	// this operation failed
			}
			
		}

		trimClients();
		
		return operationSuccess;
		
	}
	
	private void trimClients() {
		
		// iterate through all connected clients
		Iterator<ConnectedClient> fileClients = clients.values().iterator();
		while (fileClients.hasNext()) {
		
			// ... and remove those not sharing the file
			ConnectedClient client = fileClients.next();
			if (client.getFileAccessMode() == ServerFileState.NOT_SHARED) fileClients.remove();
			
		}
		
	}
	
}