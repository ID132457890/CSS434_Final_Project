/**
 * ClientFileState enumeration represents possible states of a file
 * existing in Client-side cache
 *
 */
public enum ClientFileState {

	/**
	 * Local cached copy of the file is invalid
	 */
	INVALID,
	
	/**
	 * Client will upload changes to the file after session completes
	 */
	RELEASE_OWNERSHIP,

	/**
	 * File is shared by one or more clients, none "own" the file for writing
	 */
	READ_SHARED,

	/**
	 * Client has claimed ownership of the file for writing
	 */
	WRITE_OWNED

}
