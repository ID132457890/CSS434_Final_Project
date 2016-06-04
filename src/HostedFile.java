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
	
	/**
	 * Instantiate a HostedFile, setting contents from the specified file
	 * @param filename The filename of the file to read/track/manage
	 */
	public HostedFile(String filename) {
	
		if (FileServer.DEBUG_MODE) System.out.println("Creating HostedFile for " + filename);

		// how the client referred to the file
		this.filename = filename;
		
		// get the (real) reference to the filesystem
		file = Paths.get(filename);
		
		// read file contents if possible
		try {
			
			if (FileServer.DEBUG_MODE) System.out.println("Attempting to read file contents...");

			fileContents = new FileContents(Files.readAllBytes(file));

			if (FileServer.DEBUG_MODE) System.out.println("File contents read!");

		} catch (IOException e) {

			// Unable to read file contents!
			if (FileServer.DEBUG_MODE) System.out.println("Unable to read file contents: " + e.getMessage());

			// Swallowing exceptions is bad. But, in this case, what else to do?
			
		}
			
	}

	/**
	 * Remove a client associated with this file
	 * @param clientIPName The hostname/IP address of the client to de-register
	 */
	public void deRegisterClient(String clientIPName) {

		if (FileServer.DEBUG_MODE) System.out.println("Deregistering client " + clientIPName + " from " + filename);

		clients.remove(clientIPName);
		
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
	 * Get the "owner" of this file. The file owner is the one (and only one!) that has
	 * requested write privileges.
	 * @return The owner of this file, or null if not owned
	 */
	public ConnectedClient getOwner() {
		
		if (FileServer.DEBUG_MODE) System.out.println("Attempting to locate owner of " + filename);

		for (Entry<String, ConnectedClient> client : clients.entrySet()) {
			
			ServerFileState clientState = client.getValue().getFileAccessMode();

			// "write_shared" or "ownership_change" is an indication of ownership
			if (clientState == ServerFileState.WRITE_SHARED || clientState == ServerFileState.OWNERSHIP_CHANGE) {

				if (FileServer.DEBUG_MODE) System.out.println("Found owner (" + client.getKey() + ") having mode " + clientState);

				return client.getValue();
			
			}
			
		}
		
		// no owner exists
		if (FileServer.DEBUG_MODE) System.out.println("No owner found");
		return null;
		
	}
	
	/**
	 * Get the access state of this file
	 * @return The current state of the file
	 */
	public ServerFileState getState() {
		return fileState;
	}
	
	/**
	 * Helper method that will inform all clients accessing this file that their
	 * local copies are no longer valid (another client modified the file)
	 * @return Operation success (TRUE) or failure (FALSE)
	 */
	private boolean invalidateClients() {

		if (FileServer.DEBUG_MODE) System.out.println("Attempting to invalidate all client copies of " + filename);

		boolean operationSuccess = true;
		
		// iterate over collection of connected clients
		for (ConnectedClient fileClient : clients.values()) {
			
			// ... invalidate each
			boolean invalidateSuccess = fileClient.invalidate();
			
			if (FileServer.DEBUG_MODE) System.out.println("Invalidation request for " + fileClient.getClientIPName() + " results " + invalidateSuccess);

			// update access mode for this client
			if (!invalidateSuccess) {
				if (FileServer.DEBUG_MODE) System.out.println("Invalidation failure - forcing to NOT_SHARED");
				fileClient.setFileAccessMode(ServerFileState.NOT_SHARED);
				operationSuccess = false;	// this operation failed
			}
			
		}

		trimClients();
		
		if (FileServer.DEBUG_MODE) System.out.println("Overall success of all client invalidations: " + operationSuccess);

		return operationSuccess;
		
	}
	
	/**
	 * Internal helper method for associating a client with this file
	 * @param clientIPName The hostname/IP address of the client
	 * @param port The port number the client is listening on for RMI requests
	 * @param clientFileState The client-requested file access mode
	 */
	private void registerClient(String clientIPName, int port, ServerFileState clientFileState) {
		
		if (FileServer.DEBUG_MODE) System.out.println("Registering client " + clientIPName + " on port " + port + " with new file state of " + clientFileState);
		
		// if client is already using this file, get it
		ConnectedClient client = clients.get(clientIPName);
		
		// client not using this file
		if (client == null) {

			if (FileServer.DEBUG_MODE) System.out.println("Client was not previously using this file, adding to list");

			client = new ConnectedClient(clientIPName, port);
			clients.put(clientIPName, client);
			
		} else {
			
			if (FileServer.DEBUG_MODE) System.out.println("Client is already using this file");

		}

		// set access mode
		if (FileServer.DEBUG_MODE) System.out.println("Setting client access mode to " + clientFileState);
		client.setFileAccessMode(clientFileState);

	}
	
	/**
	 * A client would like to be registered as "owner" of the file
	 * @param clientIPName The hostname or IP address of the owning client
	 * @param port The port number the client is accepting requests on
	 * @return Operation success (TRUE) or failure (FALSE)
	 */
	public boolean registerOwner(String clientIPName, int port) {

		if (FileServer.DEBUG_MODE) System.out.println("Setting client " + clientIPName + " at port " + port + " as owner of " + filename);

		// is there an owner already?
		if (FileServer.DEBUG_MODE) System.out.println("Finding file owner...");
		ConnectedClient owner = getOwner();
		if (owner == null) {
			
			if (FileServer.DEBUG_MODE) System.out.println("No other client owns this file");

			// this client is now the owner
			if (FileServer.DEBUG_MODE) System.out.println("Setting file state to WRITE_SHARED");
			fileState = ServerFileState.WRITE_SHARED;
			
			// register this client, write mode
			registerClient(clientIPName, port, ServerFileState.WRITE_SHARED);
			
			// success!
			if (FileServer.DEBUG_MODE) System.out.println("Client is registered as owner");
			return true;
			
		}
		
		// this client is not the owner
		if (FileServer.DEBUG_MODE) System.out.println("Current owner is: " + owner.getClientIPName());
		if (FileServer.DEBUG_MODE) System.out.println("Setting file state to OWNERSHIP_CHANGE");
		fileState = ServerFileState.OWNERSHIP_CHANGE;
		
		// tell the owner to write back its' changes
		boolean writebackSuccess = owner.writeback();
		if (!writebackSuccess) return false;	// operation failed, file status indeterminate (retain)
		
		// take ownership now
		if (FileServer.DEBUG_MODE) System.out.println("Setting file state to WRITE_SHARED");
		fileState = ServerFileState.WRITE_SHARED;
		
		// register this client, write mode
		registerClient(clientIPName, port, ServerFileState.WRITE_SHARED);
		
		// success!
		return true;
		
	}

	/**
	 * A client would like to be registered as a reader only
	 * @param clientIPName The hostname or IP address of the client
	 * @param port The port number the client is accepting requests on
	 */
	public void registerReader(String clientIPName, int port) {
		
		if (FileServer.DEBUG_MODE) System.out.println("Registering client " + clientIPName + " at port " + port + " as a reader");

		// update file state if this is the first client to register
		if (fileState == ServerFileState.NOT_SHARED) {
			
			if (FileServer.DEBUG_MODE) System.out.println("Changing file state from NOT_SHARED to READ_SHARED");

			fileState = ServerFileState.READ_SHARED;

		}
		
		// register this client, read-only
		registerClient(clientIPName, port, ServerFileState.READ_SHARED);
		
	}
	
	/**
	 * Update the file, using the RMI representation from a client
	 * @param fileContents The new version of the file
	 */
	public void setFileContents(FileContents fileContents) {
	
		try {
			
			if (FileServer.DEBUG_MODE) System.out.println("Attempting to overwrite " + filename);

			// write back changes to file on filesystem
			Files.write(file, fileContents.get(), new OpenOption[]{});

			// set local file contents
			this.fileContents = fileContents;
			
			// all clients must invalidate their cached copies
			boolean invalidationSuccess = invalidateClients();
			if (!invalidationSuccess) return;
			
			// this file is not shared any more
			if (FileServer.DEBUG_MODE) System.out.println("Setting file state to NOT_SHARED");
			fileState = ServerFileState.NOT_SHARED;

		} catch (IOException e) {

			// Not sure what else to do with this
			if (FileServer.DEBUG_MODE) System.out.println("Caught exception trying to update file: " + e.getMessage());
		
		}
	
	}

	/**
	 * Helper method to remove clients that are no longer accessing this file
	 */
	private void trimClients() {
		
		if (FileServer.DEBUG_MODE) System.out.println("Cleaning up list of clients sharing this file");

		// iterate through all connected clients
		Iterator<ConnectedClient> fileClients = clients.values().iterator();
		while (fileClients.hasNext()) {
		
			// ... and remove those not sharing the file
			ConnectedClient client = fileClients.next();
			if (client.getFileAccessMode() == ServerFileState.NOT_SHARED) {
				
				if (FileServer.DEBUG_MODE) System.out.println("Removing " + client.getClientIPName() + " from the list");

				fileClients.remove();

			}
			
		}
		
	}
	
}