import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of Distributed File System Server
 * 
 * This class contains a "main()", so it is intended to be launched from
 * a console session. The only argument necessary is the port that will
 * be used for receiving RMI requests from FileClients.
 * 
 * With the exception of reading bytes from the filesystem to instantiate
 * HostedFiles, the server class delegates all file operations to the
 * appropriate instance of HostedFile. By maintaining a collection of
 * "active" HostedFiles, the server is effectively caching requests.
 *
 */
@SuppressWarnings("serial")
public class FileServer extends UnicastRemoteObject implements ServerInterface {

	public static final boolean DEBUG_MODE = true;
	
	public static final String CLIENT_RMI_SERVICE_NAME = "fileclient";
	private static final String RMI_URL_PREFIX = "rmi://localhost:";
	
	public static void main(String[] args) {

		// need a single argument - the port to accept requests on
        if (args.length != 1) {
            
        	System.out.println("usage: java FileServer port");
            System.exit(-1);
        
        }

		try {
			
			// should always instantiate via interface
			if (DEBUG_MODE) System.out.println("Launching FileServer on port " + args[0]);
			ServerInterface server = new FileServer(Integer.parseInt(args[0])); 
			
			// register server process with RMI service directory
			String RMIUrl = RMI_URL_PREFIX + args[0] + "/" + RMI_SERVICE_NAME;
			if (DEBUG_MODE) System.out.println("RMI: Binding FileServer at: " + RMIUrl);
			Naming.rebind(RMIUrl, server);
			
		}
		catch (Exception e) {
			
			System.err.println("Exception in main(): " + e.getMessage());
			
			// negative return code is an error in execution
			System.exit(-1);
			
		}
		
	}
	
	// the files/clients being hosted by this server
	private Map<String, HostedFile> hostedFiles = new HashMap<String, HostedFile>();
	
	// the port number for incoming RMI requests
	private int port;
	
	// required no-args constructor
	public FileServer() throws RemoteException {}
	
	/**
	 * Instantiate a FileServer, listening on a specified port
	 * @param port The port on which to receive RMI requests
	 * @throws RemoteException
	 */
	public FileServer(int port) throws RemoteException {
		this.port = port;
	}

	@Override
	public FileContents download(String clientIPName, String filename, String mode) {

		if (DEBUG_MODE) System.out.println("Received download request from " + clientIPName + " for " + filename + " with mode " + mode);

		// make sure the client is no longer associated with any files - it's requesting a new one
		for (HostedFile file : hostedFiles.values()) {
			file.deRegisterClient(clientIPName);
		}
		
		// get the referenced file
		HostedFile file = getFile(filename);
	
		// valid filename?
		if (file == null) {
			if (DEBUG_MODE) System.out.println("Filename invalid! (returning NULL to client)");
			return null;
		}
		
		// any client may get the file in read mode
		if (mode.equalsIgnoreCase(ServerInterface.READ_MODE)) {
			
			file.registerReader(clientIPName, port);
			return file.getFileContents();
			
		}
	
		// this client wants to obtain ownership of the file for writing
		if (mode.equalsIgnoreCase(ServerInterface.WRITE_MODE)) {
			
			boolean registerSuccess = file.registerOwner(clientIPName, port);
			
			if (registerSuccess) {
				
				if (DEBUG_MODE) System.out.println("Client registered to file successfully - returning file");

				return file.getFileContents();

			} else {
				
				if (DEBUG_MODE) System.out.println("Unable to register client, returning NULL");

				return null;

			}
			
		}
		
		// unrecognized mode
		if (DEBUG_MODE) System.out.println("Mode [" + mode + "] not recognized by FileServer! Returning NULL");
		return null;
	
	}

	/**
	 * Get a file from cache or from the filesystem
	 * @param filename The filename of the file to retrieve
	 * @return The file, as a populated HostedFile class
	 */
	public HostedFile getFile(String filename) {

		if (DEBUG_MODE) System.out.println("Checking local cache for " + filename);
		
		// filename provided?
		if (filename == null || filename.length() == 0) {
			
			if (DEBUG_MODE) System.out.println("No filename provided! Returning NULL");

			return null;

		}
		
		// in cache?
		HostedFile returnFile = hostedFiles.get(filename);
		
		// not in cache - retrieve and populate from filesystem
		if (returnFile == null) {
			
			if (DEBUG_MODE) System.out.println("File not found in local cache, reading from filesystem");

			returnFile = new HostedFile(filename);
			
			// was there a file by the specified name?
			if (returnFile.getFileContents() == null || returnFile.getFileContents().get().length == 0) {
				
				if (DEBUG_MODE) System.out.println("File not found in filesystem! Returning NULL");

				return null;

			}
			
			// valid file - put into cache
			if (DEBUG_MODE) System.out.println("File read from filesystem, placing into local cache");
			hostedFiles.put(filename, returnFile);

		}
		
		return returnFile;
		
	}
	
	@Override
	public boolean upload(String clientIPName, String filename, FileContents contents) {

		if (DEBUG_MODE) System.out.println("Received upload request from " + clientIPName + " for " + filename);

		// get the referenced file
		if (DEBUG_MODE) System.out.println("Getting cached copy of file");
		HostedFile file = getFile(filename);
	
		// valid filename?
		if (file == null) {
			if (DEBUG_MODE) System.out.println("File not found in cache! Returning NULL");
			return false;
		}

		if (DEBUG_MODE) System.out.println("File found, checking for proper mode");
		
		// is this file in the correct mode for updates? (must have an owner)
		ConnectedClient owningClient = file.getOwner();
		if (DEBUG_MODE) {
			if (owningClient != null) {
				System.out.println("File currently owned by " + owningClient.getClientIPName());
			} else {
				System.out.println("File not owned by any client - returning FALSE");
			}
		}

		if (owningClient == null) return false; 	// nobody owns this file yet - uploading is not valid

		// is the client attempting to perform the upload the real owner of the file?
		if (!clientIPName.equalsIgnoreCase(owningClient.getClientIPName())) {
			if (DEBUG_MODE) System.out.println("Client attempting to upload is not the owner of the file!");
			return false;
		}
		
		// set new file contents
		if (DEBUG_MODE) System.out.println("Permissions/State check okay - setting new file contents");
		file.setFileContents(contents);
		
		return true;
		
	}
	
}