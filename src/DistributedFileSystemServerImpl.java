import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implementation of Distributed File System Server
 *
 */
@SuppressWarnings("serial")
public class DistributedFileSystemServerImpl extends UnicastRemoteObject implements DistributedFileSystemServer {

	public static final String SERVER_RMI_SERVICE_NAME = "dfs_server";
	
	
	// required no-args constructor
	public DistributedFileSystemServerImpl() throws RemoteException {}

	@Override
	public FileContents download(String clientIPName, String filename, String mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean upload(String clientIPName, String filename, FileContents contents) {
		// TODO Auto-generated method stub
		return false;
	}

	public static void main(String[] args) {
		
		try {
			
			// should always instantiate via interface
			DistributedFileSystemServer server = new DistributedFileSystemServerImpl(); 
			
			// register server process with RMI service directory
			Naming.rebind(SERVER_RMI_SERVICE_NAME, server);
			
			
		}
		catch (Exception e) {
			
			System.err.println("Exception in main(): " + e.getMessage());
			
			// negative return code is an error in execution
			System.exit(-1);
			
		}
		
	}

}
