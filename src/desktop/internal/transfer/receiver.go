package transfer

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"remotedexter/desktop/shared/logger"
	"remotedexter/desktop/shared/protocol"
	"sync"
	"time"
)

// FileReceiver manages file receiving operations
type FileReceiver struct {
	config         *FileTransferConfig
	transfers      map[string]*FileTransfer
	mu             sync.RWMutex
	sendCommand    func(cmd protocol.CommandRequest) error
	acceptCallback func(fileID, fileName string, size int64) bool
	destDir        string
}

// NewFileReceiver creates a new file receiver
func NewFileReceiver(config *FileTransferConfig, sendCommand func(cmd protocol.CommandRequest) error, acceptCallback func(fileID, fileName string, size int64) bool, destDir string) *FileReceiver {
	if config == nil {
		config = DefaultFileTransferConfig()
	}

	return &FileReceiver{
		config:         config,
		transfers:      make(map[string]*FileTransfer),
		sendCommand:    sendCommand,
		acceptCallback: acceptCallback,
		destDir:        destDir,
	}
}

// HandleFileOffer processes an incoming file offer
func (fr *FileReceiver) HandleFileOffer(offer protocol.FileOffer) {
	fr.mu.Lock()
	defer fr.mu.Unlock()

	// Check concurrent transfer limit
	if len(fr.transfers) >= fr.config.MaxConcurrent {
		fr.rejectFile(offer.FileID, "Maximum concurrent transfers reached")
		return
	}

	// Validate file size
	if offer.Size > fr.config.MaxFileSize {
		fr.rejectFile(offer.FileID, fmt.Sprintf("File too large: %d bytes (max: %d)", offer.Size, fr.config.MaxFileSize))
		return
	}

	// Check if user accepts the transfer
	if fr.acceptCallback != nil && !fr.acceptCallback(offer.FileID, offer.FileName, offer.Size) {
		fr.rejectFile(offer.FileID, "User rejected the transfer")
		return
	}

	// Create transfer record
	transfer := &FileTransfer{
		FileID:     offer.FileID,
		FileName:   offer.FileName,
		Size:       offer.Size,
		StartTime:  time.Now(),
		LastUpdate: time.Now(),
		Status:     "accepted",
		cancelChan: make(chan bool, 1),
		doneChan:   make(chan bool, 1),
	}

	fr.transfers[offer.FileID] = transfer

	// Send acceptance
	accept := protocol.FileAccept{
		FileID: offer.FileID,
	}

	acceptData, err := json.Marshal(accept)
	if err != nil {
		logger.Error("Failed to marshal file accept", logger.NewField("error", err))
		delete(fr.transfers, offer.FileID)
		return
	}

	cmd := protocol.CommandRequest{
		Type:    "file_accept",
		Payload: acceptData,
	}

	if err := fr.sendCommand(cmd); err != nil {
		logger.Error("Failed to send file accept", logger.NewField("error", err))
		delete(fr.transfers, offer.FileID)
		return
	}

	fr.config.StatusCallback(offer.FileID, "accepted", fmt.Sprintf("Accepted file transfer: %s (%d bytes)", offer.FileName, offer.Size))

	// Create destination file
	destPath := filepath.Join(fr.destDir, offer.FileName)
	file, err := os.Create(destPath)
	if err != nil {
		transfer.Status = "failed"
		transfer.Error = fmt.Errorf("failed to create destination file: %v", err)
		fr.config.StatusCallback(offer.FileID, "failed", fmt.Sprintf("Failed to create file: %v", err))
		fr.cleanupTransfer(offer.FileID)
		return
	}
	transfer.file = file
}

// HandleFileChunk processes an incoming file chunk
func (fr *FileReceiver) HandleFileChunk(chunk protocol.FileChunk) {
	fr.mu.Lock()
	transfer, exists := fr.transfers[chunk.FileID]
	fr.mu.Unlock()

	if !exists {
		logger.Warn("Received chunk for unknown transfer", logger.NewField("file_id", chunk.FileID))
		return
	}

	if transfer.Status != "accepted" && transfer.Status != "receiving" {
		logger.Warn("Received chunk for transfer not in correct state",
			logger.NewField("file_id", chunk.FileID),
			logger.NewField("status", transfer.Status))
		return
	}

	transfer.Status = "receiving"

	// Validate chunk offset
	if chunk.Offset != transfer.Transferred {
		transfer.Status = "failed"
		transfer.Error = fmt.Errorf("chunk offset mismatch: expected %d, got %d", transfer.Transferred, chunk.Offset)
		fr.config.StatusCallback(chunk.FileID, "failed", fmt.Sprintf("Chunk offset mismatch: expected %d, got %d", transfer.Transferred, chunk.Offset))
		fr.cleanupTransfer(chunk.FileID)
		return
	}

	// Write chunk data
	if transfer.file == nil {
		transfer.Status = "failed"
		transfer.Error = fmt.Errorf("destination file not open")
		fr.config.StatusCallback(chunk.FileID, "failed", "Destination file not open")
		fr.cleanupTransfer(chunk.FileID)
		return
	}

	_, err := transfer.file.Write(chunk.Data)
	if err != nil {
		transfer.Status = "failed"
		transfer.Error = fmt.Errorf("failed to write chunk: %v", err)
		fr.config.StatusCallback(chunk.FileID, "failed", fmt.Sprintf("Failed to write chunk: %v", err))
		fr.cleanupTransfer(chunk.FileID)
		return
	}

	transfer.Transferred += int64(len(chunk.Data))
	transfer.LastUpdate = time.Now()

	// Update progress
	progress := float64(transfer.Transferred) / float64(transfer.Size) * 100.0
	elapsed := time.Since(transfer.StartTime).Seconds()
	speed := float64(transfer.Transferred) / elapsed // bytes per second
	fr.config.ProgressCallback(chunk.FileID, progress, speed)

	// Check for completion
	if chunk.EOF {
		// Verify final size
		if transfer.Transferred != transfer.Size {
			transfer.Status = "failed"
			transfer.Error = fmt.Errorf("final size mismatch: expected %d, got %d", transfer.Size, transfer.Transferred)
			fr.config.StatusCallback(chunk.FileID, "failed", fmt.Sprintf("Final size mismatch: expected %d, got %d", transfer.Size, transfer.Transferred))
		} else {
			transfer.Status = "completed"
			fr.config.StatusCallback(chunk.FileID, "completed", fmt.Sprintf("File transfer completed: %s", transfer.FileName))
		}
		fr.cleanupTransfer(chunk.FileID)
	}
}

// CancelTransfer cancels an active file transfer
func (fr *FileReceiver) CancelTransfer(fileID string) error {
	fr.mu.Lock()
	transfer, exists := fr.transfers[fileID]
	fr.mu.Unlock()

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
	fr.config.StatusCallback(fileID, "cancelled", "File transfer cancelled")

	fr.cleanupTransfer(fileID)
	return nil
}

// GetTransferStatus returns the status of a file transfer
func (fr *FileReceiver) GetTransferStatus(fileID string) (*FileTransfer, error) {
	fr.mu.RLock()
	defer fr.mu.RUnlock()

	transfer, exists := fr.transfers[fileID]
	if !exists {
		return nil, fmt.Errorf("transfer not found: %s", fileID)
	}

	return transfer, nil
}

// ListActiveTransfers returns all active transfers
func (fr *FileReceiver) ListActiveTransfers() []*FileTransfer {
	fr.mu.RLock()
	defer fr.mu.RUnlock()

	transfers := make([]*FileTransfer, 0, len(fr.transfers))
	for _, transfer := range fr.transfers {
		transfers = append(transfers, transfer)
	}

	return transfers
}

// rejectFile sends a file rejection response
func (fr *FileReceiver) rejectFile(fileID string, reason string) {
	reject := protocol.FileReject{
		FileID: fileID,
		Reason: reason,
	}

	rejectData, err := json.Marshal(reject)
	if err != nil {
		logger.Error("Failed to marshal file reject", logger.NewField("error", err))
		return
	}

	cmd := protocol.CommandRequest{
		Type:    "file_reject",
		Payload: rejectData,
	}

	if err := fr.sendCommand(cmd); err != nil {
		logger.Error("Failed to send file reject", logger.NewField("error", err))
		return
	}

	logger.Info("Rejected file transfer", logger.NewField("file_id", fileID), logger.NewField("reason", reason))
}

// cleanupTransfer removes a completed transfer from the active list
func (fr *FileReceiver) cleanupTransfer(fileID string) {
	fr.mu.Lock()
	defer fr.mu.Unlock()

	if transfer, exists := fr.transfers[fileID]; exists {
		if transfer.file != nil {
			transfer.file.Close()
		}
		delete(fr.transfers, fileID)
	}
}
