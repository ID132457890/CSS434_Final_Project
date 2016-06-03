import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of Distributed File System Server
 *
 */
@SuppressWarnings("serial")
public class FileServer extends UnicastRemoteObject implements ServerInterface {

	public static final String CLIENT_RMI_SERVICE_NAME = "fileclient";
	private static final String RMI_URL_PREFIX = "rmi://localhost:";
	
	// the files/clients being hosted by this server
	private Map<String, HostedFile> hostedFiles = new HashMap<String, HostedFile>();
	
	private int port;
	
	// required no-args constructor
	public FileServer() throws RemoteException {}
	
	public FileServer(int port) throws RemoteException {
		this.port = port;
	}
	
	@Override
	public FileContents download(String clientIPName, String filename, String mode) {

		// get the referenced file
		HostedFile file = getFile(filename);
	
		// valid filename?
		if (file == null) return null;
		
		// any client may get the file in read mode
		if (mode.equalsIgnoreCase(ServerInterface.READ_MODE)) {
			
			file.registerReader(clientIPName, port);
			return file.getFileContents();
			
		}
	
		// this client wants to obtain ownership of the file for writing
		if (mode.equalsIgnoreCase(ServerInterface.WRITE_MODE)) {
			
			file.registerOwner(clientIPName, port);
			return file.getFileContents();
			
		}
		
		// unrecognized mode
		return null;
	
	}

	@Override
	public boolean upload(String clientIPName, String filename, FileContents contents) {
		// TODO Auto-generated method stub
		return false;
	}

	public static void main(String[] args) {

		// need a single argument - the port to accept requests on
        if (args.length != 1) {
            
        	System.out.println("usage: java FileServer port");
            System.exit(-1);
        
        }

		try {
			
			// should always instantiate via interface
			ServerInterface server = new FileServer(Integer.parseInt(args[0])); 
			
			// register server process with RMI service directory
			Naming.rebind(RMI_URL_PREFIX + args[0] + "/" + RMI_SERVICE_NAME, server);
			
		}
		catch (Exception e) {
			
			System.err.println("Exception in main(): " + e.getMessage());
			
			// negative return code is an error in execution
			System.exit(-1);
			
		}
		
	}
	
	/**
	 * Get a file from cache or from the filesystem
	 * @param filename The filename of the file to retrieve
	 * @return The file, as a populated HostedFile class
	 */
	public HostedFile getFile(String filename) {

		// filename provided?
		if (filename == null || filename.length() == 0) return null;
		
		// in cache?
		HostedFile returnFile = hostedFiles.get(filename);
		
		// not in cache - retrieve and populate from filesystem
		if (returnFile == null) {
			
			returnFile = new HostedFile(filename);
			
			// was there a file by the specified name?
			if (returnFile.getFileContents() == null || returnFile.getFileContents().get().length == 0) return null;
			
			// valid file - put into cache
			hostedFiles.put(filename, returnFile);

		}
		
		return returnFile;
		
	}
	
}