/**
 * ServerFileState enumeration represents possible states of a file
 * existing in Server-side cache
 *
 */
public enum ServerFileState {

	/**
	 * File is cached internally, but no clients are using this file
	 */
	NOT_SHARED,

	/**
	 * Client will upload changes to the file after local edit session completes
	 */
	OWNERSHIP_CHANGE,

	/**
	 * File is shared by one or more clients, however, none "own" the file for writing
	 */
	READ_SHARED,

	/**
	 * A client has claimed ownership of the file for writing
	 */
	WRITE_SHARED

}
