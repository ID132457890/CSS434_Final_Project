import java.io.*;
import java.util.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;

public class FileClient extends UnicastRemoteObject implements  ClientInterface
{
    private ServerInterface server = null;
    private BufferedReader input = null;

    //File stuff
    public String clientIP = "";
    public FileState currentFileState = FileState.Invalid;

    private String currentFileName = "";
    private FileContents fileContents = null;

    /*
    Initializes the file client with a server ip and port
     */
    public FileClient(String serverIP, String port) throws RemoteException
    {
        try
        {
            //Tries to access the server object
            System.out.println("rmi://" + serverIP + ":" + port + "/fileserver");
            server = (ServerInterface)Naming.lookup("rmi://" + serverIP + ":" + port + "/fileserver");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            InetAddress addr = InetAddress.getLocalHost();
            clientIP = addr.getHostName();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        input = new BufferedReader(new InputStreamReader(System.in));
    }

    /*
    Main client loop, where the typing happens
     */
    public void startClient()
    {
        while (true)
        {
            //Create a new thread for upload
            WriteThread thread = new WriteThread();
            thread.start();

            String fileName = "";
            String readWriteString = "";

            try
            {
                System.out.println("FileClient: Next file to open");
                System.out.print("File name: ");

                //Get the file name
                fileName = input.readLine();

                //Validate the name
                if (fileName.equals(""))
                {
                    thread.kill();
                    continue;
                }

                //Get the read/write option
                System.out.println("How(r/w): ");
                readWriteString = input.readLine();

                //Validate the read write string
                if (!readWriteString.equals("r") && !readWriteString.equals("w"))
                {
                    thread.kill();
                    continue;
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            //The user made a read/write, so thread not needed
            thread.kill();

            //Check if the file exists
            if (!this.checkIfExists(fileName, readWriteString))
            {
                //If it's owned as writing, then upload to server
                if (currentFileState == FileState.WriteOwned)
                {
                    this.upload();
                }

                //Otherwise not available, so download
                this.download(fileName, readWriteString);
            }

            //Finally, show the file in emacs
            this.showInEmacs(readWriteString);
        }
    }

    //Client interface implementation
    public boolean invalidate() throws RemoteException
    {
        if (currentFileState == FileState.ReadShared)
        {
            currentFileState = FileState.Invalid;

            return true;
        }

        return false;
    }

    //Client interface implementation
    public boolean writeback() throws RemoteException
    {
        if (currentFileState == FileState.WriteOwned)
        {
            currentFileState = FileState.ReleaseOwnership;

            return true;
        }

        return false;
    }

    public static void main(String[] args)
    {
        if (args.length != 2)
        {
            System.out.println("usage: java FileClient serverIP port");
            System.exit(-1);
        }

        try
        {
            //Creates the file client
            FileClient client = new FileClient(args[0], args[1]);
            Naming.rebind("rmi://localhost:" + args[1] + "/fileclient", client);
            client.startClient();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public synchronized boolean checkIfExists(String fileName, String readWrite)
    {
        if (!currentFileName.equals(fileName))
        {
            //False if the name of the current file doesn't match
            return false;
        }
        else if (currentFileState == FileState.ReleaseOwnership)
        {
            //False if the file is about to not be owned anymore
            return false;
        }
        else if (readWrite.equals("r") && currentFileState != FileState.Invalid)
        {
            //True if user wants to read and the file isn't invalid
            return true;
        }
        else if (readWrite.equals("w") && currentFileState == FileState.WriteOwned)
        {
            //True if user wants to write and the file is in the write ownership
            return true;
        }

        return false;
    }

    /*
        Downloads the file from the server.
    */
    public boolean download(String fileName, String readWrite)
    {
        //Set the file to the new permission types
        if (currentFileState == FileState.Invalid)
        {
            if (readWrite.equals("r"))
            {
                currentFileState = FileState.ReadShared;
            }
            else if (readWrite.equals("w"))
            {
                currentFileState = FileState.WriteOwned;
            }
        }
        else if (currentFileState == FileState.ReadShared)
        {
            if (readWrite.equals("w"))
            {
                currentFileState = FileState.WriteOwned;
            }
        }

        //Get the file name
        currentFileName = fileName;

        try
        {
            //Download the file with rmi interface
            fileContents = server.download(clientIP, fileName, readWrite);

            return true;
        }
        catch (RemoteException e)
        {
            e.printStackTrace();

            return false;
        }
    }

    /*
        Uploads the file to the server.
    */
    public boolean upload()
    {
        //Set the new permissions
        if (currentFileState == FileState.WriteOwned)
        {
            currentFileState = FileState.Invalid;
        }
        else if (currentFileState == FileState.ReleaseOwnership)
        {
            currentFileState = FileState.ReadShared;
        }

        try
        {
            //Upload through rmi
            server.upload(clientIP, currentFileName, fileContents);
        }
        catch (RemoteException e)
        {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    /*
        Helper for executing a unix call
         */
    public boolean executeUnix(String firstParamater, String secondParameter, String thirdParameter)
    {
        String[] params = null;

        if (thirdParameter == null)
        {
            params = new String[2];
        }
        else
        {
            params = new String[3];
            params[2] = thirdParameter;
        }

        params[0] = firstParamater;
        params[1] = secondParameter;

        try
        {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(params);

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }

    /*
        Shows the file in emacs with permission parameter
         */
    public boolean showInEmacs(String readWrite)
    {
        //Check if didn't execute correctly
        if (!this.executeUnix("chmod", "600", "/tmp/useraccount.txt"))
        {
            return false;
        }
        else
        {
            try
            {
                //Write the data to the file
                FileOutputStream stream = new FileOutputStream("/tmp/useraccount.txt");
                stream.write(fileContents.get());
                stream.flush();
                stream.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }

            //Set the appropriate permissions
            String rwParam = "";
            if (readWrite.equals("r"))
            {
                rwParam = "400";
            }
            else
            {
                rwParam = "600";
            }

            //Check if now the unix call was successful
            if (this.executeUnix("chmod", rwParam, "/tmp/useraccount.txt"))
            {
                //Check if using emac was successful
                boolean success = this.executeUnix("emacs", "/tmp/useraccount.txt", null);

                //Check if has write permissions
                if (success && readWrite.equals("w"))
                {
                    try
                    {
                        //Read from the file when changes in emac have been completed
                        FileInputStream stream = new FileInputStream("/tmp/useraccount.txt");
                        fileContents = new FileContents(new byte[stream.available()]);
                        stream.read(fileContents.get());
                        stream.close();

                        return true;
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        return false;
                    }
                }
            }

            return false;
        }
    }

    //Enumeration to hold the file state
    public enum FileState
    {
        Invalid, ReadShared, WriteOwned, ReleaseOwnership;
    }



    /*
    Custom thread for uploading a file that is needed to be uploaded to the server.
     */
    private class WriteThread extends Thread
    {
        private boolean running = false;
        private FileClient attachedClient = null;

        public void WriteThread(FileClient client)
        {
            running = true;

            attachedClient = client;
        }

        public void run()
        {
            while (running)
            {
                //Keep checking if the file needs to be uplaoded
                if (attachedClient.currentFileState == FileState.ReleaseOwnership)
                {
                    attachedClient.upload();
                }
            }
        }

        synchronized void kill()
        {
            running = false;

            try
            {
                this.join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}