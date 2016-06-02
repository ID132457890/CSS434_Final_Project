import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * HostedFile represents a file that one or more Clients have requested. Also, the 
 * FileServer uses this class to associate a locally-cached file with the client(s)
 * that have requested it.
 *
 */
public class HostedFile {

	// the actual file as it exists in the local filesystem
	private Path file;
	
	// the representation as given to clients upon request/update
	private FileContents fileContents = null;
	
	// the filename, as requested by clients
	private String filename;
	
	// a collection of clients that are currently using this file
	private Map<String, ConnectedClient> clients = new HashMap<String, ConnectedClient>();
	
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
	 * Get clients that are currently using this file
	 * @return
	 */
	public Map<String, ConnectedClient> getClients() {
		return clients;
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
		this.fileContents = fileContents;
	}
	
}