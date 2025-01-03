# Java Socket-based FTP Server
A simple FTP server implementation using Java Socket Programming that allows file upload, download, and navigation capabilities.

## Features
- User authentication
- File upload and download
- Directory navigation
- File search functionality
- Support for multiple concurrent clients

## Project Structure
```
src/
    main/
        java/
            com/mycom/ftpserver/
                Server.java         - Main server implementation
                Client.java        - Client application
                ClientHandler.java - Handles individual client connections
                Authentication.java - User authentication
                Search.java        - File search functionality
                DirectoryNavigator.java - Directory navigation
        resources/
            config/
                private.txt       - User credentials
                list.txt         - File index
            storage/             - File storage directory
                Bangla/
                English/
                Japanese/
```

## Compilation
Navigate to the project root directory and compile all Java files:

```sh
javac -d target src/main/java/com/mycom/ftpserver/*.java
```

## Running the Application
1. Start the Server:
```sh
java -cp target com.mycom.ftpserver.Server
```

2. Start the Client:
```sh
java -cp target com.mycom.ftpserver.Client
```

## Authentication

Default credentials are stored in a file named **private.txt**. This file contains the following default login information:

- **Username**: `sabbir`
- **Password**: `12345`

Ensure the **private.txt** file is kept secure to avoid unauthorized access.

## Available Commands

Once authenticated, you can use the following commands to interact with the system:

- `ls`: Lists all files and directories in the current directory.
- `cd <directory>`: Changes the current directory to the specified directory.
- `pwd`: Displays the full path of the current directory.
- `upload <filename>`: Uploads a file to the server.
- `download <filename>`: Downloads a file from the server.
- `showfiles`: Displays a list of all files currently stored on the server.
- `search <filename>`: Searches for a specific file by name.
- `help`: Displays a list of all available commands with descriptions.
- `Exit`: Terminate the connection.

## Notes

- Replace `<filename>` and `<directory>` with the actual names of files or directories when using the corresponding commands.
- Ensure you have the necessary permissions for file operations to avoid errors.
- For additional assistance, use the `help` command.



## Usage Examples

1. **Connecting to Server**:
```
Username: sabbir
Password: 12345
```

2. **Navigating Directories**:
```
> ls
[DIR] Bangla
[DIR] English
[DIR] Japanese

> cd Bangla
Changed directory to: .../storage/Bangla
```

3. **Uploading Files**:
```
> upload myfile.txt
Sending file: myfile.txt
Upload complete: 100%
```

4. **Downloading Files**:
```
> download document.pdf
Receiving file: document.pdf
Download complete: 100%
```

5. **Searching Files**:
```
> search document.pdf
File found in English
```

## Technical Details
- Server runs on port 12345 by default
- Supports concurrent client connections using thread pool
- Uses buffered streams for efficient file transfer
- Implements basic authentication mechanism
- Files are stored in language-specific subdirectories

## Error Handling
- Invalid credentials result in connection termination
- Invalid commands display usage information
- File transfer errors are gracefully handled
- Directory navigation is restricted to storage area

## Security Features
- Password-based authentication
- Restricted access to server storage directory
- No access to system directories outside storage
- Credential file is separate from storage

## Network Protocol
- Uses TCP/IP for reliable data transfer
- Custom protocol for file transfer operations
- Supports large file transfers
- Maintains persistent connections

Remember to ensure the storage directories exist and have appropriate permissions before running the server.