package transfer

import (
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"remotedexter/desktop/shared/logger"
	"remotedexter/desktop/shared/protocol"
	"sync"
	"time"
)

// FileTransferConfig holds configuration for file transfers
type FileTransferConfig struct {
	MaxFileSize      int64
	MaxConcurrent    int
	ChunkSize        int
	RetryAttempts    int
	RetryDelay       time.Duration
	ProgressCallback func(fileID string, progress float64, speed float64)
	StatusCallback   func(fileID string, status string, message string)
}

// DefaultFileTransferConfig returns default configuration
func DefaultFileTransferConfig() *FileTransferConfig {
	return &FileTransferConfig{
		MaxFileSize:      2 * 1024 * 1024 * 1024, // 2 GB
		MaxConcurrent:    2,
		ChunkSize:        64 * 1024, // 64 KB
		RetryAttempts:    3,
		RetryDelay:       1 * time.Second,
		ProgressCallback: func(fileID string, progress float64, speed float64) {},
		StatusCallback:   func(fileID string, status string, message string) {},
	}
}

// FileTransfer represents an active file transfer
type FileTransfer struct {
	FileID      string
	FileName    string
	Size        int64
	Transferred int64
	StartTime   time.Time
	LastUpdate  time.Time
	Status      string
	Error       error
	file        *os.File
	cancelChan  chan bool
	doneChan    chan bool
}

// FileSender manages file sending operations
type FileSender struct {
	config      *FileTransferConfig
	transfers   map[string]*FileTransfer
	mu          sync.RWMutex
	sendCommand func(cmd protocol.CommandRequest) error
}

// NewFileSender creates a new file sender
func NewFileSender(config *FileTransferConfig, sendCommand func(cmd protocol.CommandRequest) error) *FileSender {
	if config == nil {
		config = DefaultFileTransferConfig()
	}

	return &FileSender{
		config:      config,
		transfers:   make(map[string]*FileTransfer),
		sendCommand: sendCommand,
	}
}

// SendFile initiates a file transfer to the remote device
func (fs *FileSender) SendFile(filePath string) error {
	fs.mu.Lock()
	defer fs.mu.Unlock()

	// Check concurrent transfer limit
	if len(fs.transfers) >= fs.config.MaxConcurrent {
		return fmt.Errorf("maximum concurrent transfers reached (%d)", fs.config.MaxConcurrent)
	}

	// Validate file
	info, err := os.Stat(filePath)
	if err != nil {
		return fmt.Errorf("failed to stat file: %v", err)
	}

	if info.Size() > fs.config.MaxFileSize {
		return fmt.Errorf("file too large: %d bytes (max: %d)", info.Size(), fs.config.MaxFileSize)
	}

	// Generate file ID
	fileID := generateFileID(filePath, info.Size())

	// Create transfer record
	transfer := &FileTransfer{
		FileID:     fileID,
		FileName:   filepath.Base(filePath),
		Size:       info.Size(),
		StartTime:  time.Now(),
		LastUpdate: time.Now(),
		Status:     "offering",
		cancelChan: make(chan bool, 1),
		doneChan:   make(chan bool, 1),
	}

	fs.transfers[fileID] = transfer

	// Send file offer
	offer := protocol.FileOffer{
		FileID:   fileID,
		FileName: transfer.FileName,
		Size:     transfer.Size,
		MimeType: getMimeType(filePath),
	}

	offerData, err := json.Marshal(offer)
	if err != nil {
		delete(fs.transfers, fileID)
		return fmt.Errorf("failed to marshal file offer: %v", err)
	}

	cmd := protocol.CommandRequest{
		Type:    "file_offer",
		Payload: offerData,
	}

	if err := fs.sendCommand(cmd); err != nil {
		delete(fs.transfers, fileID)
		return fmt.Errorf("failed to send file offer: %v", err)
	}

	fs.config.StatusCallback(fileID, "offered", fmt.Sprintf("Offered file %s (%d bytes)", transfer.FileName, transfer.Size))

	// Start transfer in background
	go fs.transferFile(transfer, filePath)

	return nil
}

// HandleFileAccept processes a file acceptance response
func (fs *FileSender) HandleFileAccept(fileID string) {
	fs.mu.Lock()
	transfer, exists := fs.transfers[fileID]
	fs.mu.Unlock()

	if !exists {
		logger.Warn("Received accept for unknown transfer", logger.NewField("file_id", fileID))
		return
	}

	if transfer.Status != "offering" {
		logger.Warn("Received accept for transfer not in offering state",
			logger.NewField("file_id", fileID),
			logger.NewField("status", transfer.Status))
		return
	}

	transfer.Status = "accepted"
	fs.config.StatusCallback(fileID, "accepted", "File transfer accepted by remote device")
}

// HandleFileReject processes a file rejection response
func (fs *FileSender) HandleFileReject(fileID string, reason string) {
	fs.mu.Lock()
	transfer, exists := fs.transfers[fileID]
	fs.mu.Unlock()

	if !exists {
		logger.Warn("Received reject for unknown transfer", logger.NewField("file_id", fileID))
		return
	}

	transfer.Status = "rejected"
	transfer.Error = fmt.Errorf("transfer rejected: %s", reason)
	fs.config.StatusCallback(fileID, "rejected", fmt.Sprintf("File transfer rejected: %s", reason))

	// Clean up
	fs.cleanupTransfer(fileID)
}

// CancelTransfer cancels an active file transfer
func (fs *FileSender) CancelTransfer(fileID string) error {
	fs.mu.Lock()
	transfer, exists := fs.transfers[fileID]
	fs.mu.Unlock()

	if !exists {
		return fmt.Errorf("transfer not found: %s", fileID)
	}

	if transfer.Status == "completed" || transfer.Status == "failed" {
		return fmt.Errorf("transfer already finished: %s", transfer.Status)
	}

	// Signal cancellation
	select {
	case transfer.cancelChan <- true:
	default:
	}

	transfer.Status = "cancelled"
	fs.config.StatusCallback(fileID, "cancelled", "File transfer cancelled")

	fs.cleanupTransfer(fileID)
	return nil
}

// GetTransferStatus returns the status of a file transfer
func (fs *FileSender) GetTransferStatus(fileID string) (*FileTransfer, error) {
	fs.mu.RLock()
	defer fs.mu.RUnlock()

	transfer, exists := fs.transfers[fileID]
	if !exists {
		return nil, fmt.Errorf("transfer not found: %s", fileID)
	}

	return transfer, nil
}

// ListActiveTransfers returns all active transfers
func (fs *FileSender) ListActiveTransfers() []*FileTransfer {
	fs.mu.RLock()
	defer fs.mu.RUnlock()

	transfers := make([]*FileTransfer, 0, len(fs.transfers))
	for _, transfer := range fs.transfers {
		transfers = append(transfers, transfer)
	}

	return transfers
}

// transferFile performs the actual file transfer
func (fs *FileSender) transferFile(transfer *FileTransfer, filePath string) {
	defer func() {
		fs.cleanupTransfer(transfer.FileID)
	}()

	// Open file
	file, err := os.Open(filePath)
	if err != nil {
		transfer.Status = "failed"
		transfer.Error = fmt.Errorf("failed to open file: %v", err)
		fs.config.StatusCallback(transfer.FileID, "failed", fmt.Sprintf("Failed to open file: %v", err))
		return
	}
	transfer.file = file
	defer file.Close()

	// Wait for acceptance or cancellation
	select {
	case <-transfer.doneChan:
		// acceptance received; continue with transfer
	case <-transfer.cancelChan:
		return
	case <-time.After(30 * time.Second): // Timeout waiting for acceptance
		transfer.Status = "failed"
		transfer.Error = fmt.Errorf("timeout waiting for acceptance")
		fs.config.StatusCallback(transfer.FileID, "failed", "Timeout waiting for acceptance")
		return
	}

	// Transfer file in chunks
	buffer := make([]byte, fs.config.ChunkSize)
	offset := int64(0)

	for {
		select {
		case <-transfer.cancelChan:
			return
		default:
		}

		n, err := file.Read(buffer)
		if err != nil && err != io.EOF {
			transfer.Status = "failed"
			transfer.Error = fmt.Errorf("failed to read file: %v", err)
			fs.config.StatusCallback(transfer.FileID, "failed", fmt.Sprintf("Failed to read file: %v", err))
			return
		}

		eof := err == io.EOF || n == 0

		// Send chunk
		chunk := protocol.FileChunk{
			FileID: transfer.FileID,
			Offset: offset,
			Data:   buffer[:n],
			EOF:    eof,
		}

		chunkData, err := json.Marshal(chunk)
		if err != nil {
			transfer.Status = "failed"
			transfer.Error = fmt.Errorf("failed to marshal chunk: %v", err)
			fs.config.StatusCallback(transfer.FileID, "failed", fmt.Sprintf("Failed to marshal chunk: %v", err))
			return
		}

		cmd := protocol.CommandRequest{
			Type:    "file_chunk",
			Payload: chunkData,
		}

		// Retry logic
		var sendErr error
		for attempt := 0; attempt < fs.config.RetryAttempts; attempt++ {
			if sendErr = fs.sendCommand(cmd); sendErr == nil {
				break
			}
			if attempt < fs.config.RetryAttempts-1 {
				time.Sleep(fs.config.RetryDelay)
			}
		}

		if sendErr != nil {
			transfer.Status = "failed"
			transfer.Error = fmt.Errorf("failed to send chunk after %d attempts: %v", fs.config.RetryAttempts, sendErr)
			fs.config.StatusCallback(transfer.FileID, "failed", fmt.Sprintf("Failed to send chunk: %v", sendErr))
			return
		}

		offset += int64(n)
		transfer.Transferred = offset
		transfer.LastUpdate = time.Now()

		// Update progress
		progress := float64(offset) / float64(transfer.Size) * 100.0
		elapsed := time.Since(transfer.StartTime).Seconds()
		speed := float64(offset) / elapsed // bytes per second
		fs.config.ProgressCallback(transfer.FileID, progress, speed)

		if eof {
			break
		}
	}

	transfer.Status = "completed"
	fs.config.StatusCallback(transfer.FileID, "completed", fmt.Sprintf("File transfer completed: %s", transfer.FileName))
}

// cleanupTransfer removes a completed transfer from the active list
func (fs *FileSender) cleanupTransfer(fileID string) {
	fs.mu.Lock()
	defer fs.mu.Unlock()

	if transfer, exists := fs.transfers[fileID]; exists {
		if transfer.file != nil {
			transfer.file.Close()
		}
		delete(fs.transfers, fileID)
	}
}

// generateFileID generates a unique file ID based on path and size
func generateFileID(filePath string, size int64) string {
	hash := sha256.Sum256([]byte(fmt.Sprintf("%s:%d:%d", filePath, size, time.Now().UnixNano())))
	return fmt.Sprintf("%x", hash[:16]) // 32 character hex string
}

// getMimeType returns a basic MIME type based on file extension
func getMimeType(filePath string) string {
	ext := filepath.Ext(filePath)
	switch ext {
	case ".txt":
		return "text/plain"
	case ".jpg", ".jpeg":
		return "image/jpeg"
	case ".png":
		return "image/png"
	case ".pdf":
		return "application/pdf"
	case ".zip":
		return "application/zip"
	default:
		return "application/octet-stream"
	}
}
