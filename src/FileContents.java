import java.io.IOException;
import java.io.Serializable;

/**
 * FileContents contains the actual contents of a file shared
 * between the server and client
 *
 */
@SuppressWarnings("serial")
public class FileContents implements Serializable {

	// actual contents of the file
	private byte[] contents;
	
	/**
	 * Construct a representation of a file, given the actual file contents
	 * @param contents The contents of the file
	 */
	public FileContents( byte[] contents ) {
		this.contents = contents;
	}
	
	/**
	 * Send file contents to the console.
	 * @throws IOException 
	 */
	public void print() throws IOException {
		System.out.println( "File Contents = " + contents );
	}
	
	/**
	 * Get the contents of the file
	 * @return File contents
	 */
	public byte[] get() {
		return contents;
	}
	
}