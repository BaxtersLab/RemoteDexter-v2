# RemoteDexter Android File Transfer Integration

This document describes how to integrate the file transfer components into the RemoteDexter Android application.

## Components Overview

### TransferManager
The main coordinator class that manages file sending and receiving operations.

- **Location**: `com.remotedexter.transfer.TransferManager`
- **Purpose**: Coordinates between FileSender and FileReceiver, handles protocol commands

### FileSender
Handles sending files from Android device to desktop.

- **Location**: `com.remotedexter.transfer.FileSender`
- **Features**:
  - File selection via Storage Access Framework (SAF)
  - Chunked file reading and sending
  - Progress tracking and error handling
  - Concurrent transfer management

### FileReceiver
Handles receiving files from desktop to Android device.

- **Location**: `com.remotedexter.transfer.FileReceiver`
- **Features**:
  - Automatic file acceptance (configurable)
  - Secure file writing to app-private storage
  - Chunk validation and reassembly
  - Progress tracking and error handling

### FileTransferActivity
Example activity demonstrating file transfer integration.

- **Location**: `com.remotedexter.FileTransferActivity`
- **Purpose**: Sample implementation showing how to use TransferManager in an Android activity

## Integration Steps

### 1. Add TransferManager to Your Main Activity/Service

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var transferManager: TransferManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize transfer manager with your command sender
        transferManager = TransferManager(
            context = this,
            sendCommand = { command -> yourTransportLayer.sendCommand(command) },
            onProgress = { fileId, progress, speed ->
                // Update UI with progress
                updateTransferProgress(fileId, progress, speed)
            },
            onStatus = { fileId, status, message ->
                // Show status updates to user
                showTransferStatus(fileId, status, message)
            }
        )
    }

    // Handle incoming file transfer commands from desktop
    fun onCommandReceived(command: String, data: ByteArray) {
        transferManager.handleCommand(command, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        transferManager.shutdown()
    }
}
```

### 2. Add File Transfer UI Elements

Add buttons or menu items to trigger file transfers:

```kotlin
// Send file button
sendFileButton.setOnClickListener {
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "*/*"
        addCategory(Intent.CATEGORY_OPENABLE)
    }
    filePickerLauncher.launch(intent)
}

// File picker result handler
private val filePickerLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        result.data?.data?.let { uri ->
            val fileId = transferManager.sendFile(uri)
            if (fileId == null) {
                Toast.makeText(this, "Failed to start transfer", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

### 3. Handle File Transfer Permissions

Add these permissions to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

For Android 13+ (API 33+), use granular permissions:

```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
```

### 4. Integrate with Transport Layer

Connect the TransferManager to your existing transport layer:

```kotlin
// In your transport receiver
fun onMessageReceived(message: ByteArray) {
    // Parse command type and data
    val (command, data) = parseMessage(message)

    // Handle file transfer commands
    if (isFileTransferCommand(command)) {
        transferManager.handleCommand(command, data)
    } else {
        // Handle other commands
        handleOtherCommand(command, data)
    }
}

// Helper function to identify file transfer commands
private fun isFileTransferCommand(command: String): Boolean {
    return command.startsWith("file_")
}
```

### 5. Add Progress and Status UI

Create UI components to show transfer progress:

```kotlin
private fun updateTransferProgress(fileId: String, progress: Float, speed: Float) {
    // Update progress bar
    progressBar.progress = progress.toInt()

    // Update status text
    statusText.text = "Transferring... ${progress.toInt()}% (${speed.toInt()} B/s)"
}

private fun showTransferStatus(fileId: String, status: String, message: String) {
    when (status) {
        "completed" -> {
            Toast.makeText(this, "Transfer completed: $message", Toast.LENGTH_SHORT).show()
            // Reset UI
            resetTransferUI()
        }
        "failed" -> {
            Toast.makeText(this, "Transfer failed: $message", Toast.LENGTH_LONG).show()
            // Show retry option
            showRetryButton(fileId)
        }
        "cancelled" -> {
            Toast.makeText(this, "Transfer cancelled", Toast.LENGTH_SHORT).show()
            resetTransferUI()
        }
    }
}
```

## Configuration Options

### Transfer Limits
- **Max Concurrent Transfers**: 2 (configurable in FileSender/FileReceiver)
- **Max File Size**: 2GB (configurable in FileReceiver)
- **Chunk Size**: 64KB (configurable in FileSender)

### Storage Location
- Received files are stored in app-private external storage: `context.getExternalFilesDir("Downloads")`
- Files are automatically renamed to avoid conflicts

### Security Features
- All transfers are encrypted via the existing Noise protocol
- File validation with size and offset checking
- Automatic cleanup of failed transfers

## Error Handling

The components include comprehensive error handling for:

- File access permission issues
- Network/transport failures
- File corruption during transfer
- Storage space limitations
- Concurrent transfer limits

Monitor the `onStatus` callback for error notifications and user feedback.

## Testing

Test file transfers with various file types and sizes:

1. Small text files (< 1MB)
2. Large media files (> 100MB)
3. Binary files (images, executables)
4. Files with special characters in names
5. Transfers during poor network conditions

## Dependencies

Ensure these dependencies are included in `build.gradle`:

```gradle
dependencies {
    // For JSON serialization (if using kotlinx.serialization)
    implementation 'org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1'

    // For AndroidX components
    implementation 'androidx.activity:activity-ktx:1.7.2'
    implementation 'androidx.appcompat:appcompat:1.6.1'
}
```

## Protocol Integration

The file transfer components expect these protocol message types (defined in `messages.go`):

- `FileOffer`: Desktop offering a file for transfer
- `FileAccept`: Mobile accepting the file offer
- `FileReject`: Mobile rejecting the file offer
- `FileChunk`: File data chunks (bidirectional)

Ensure your protocol serialization/deserialization handles these types correctly.