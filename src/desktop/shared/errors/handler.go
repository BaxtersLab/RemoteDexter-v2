package errors

import (
	"fmt"
	"log"
	"time"
)

// ErrorLevel represents the severity of an error
type ErrorLevel int

const (
	LevelInfo ErrorLevel = iota
	LevelWarn
	LevelError
)

// ErrorCategory represents the type of error
type ErrorCategory int

const (
	CategoryNetwork ErrorCategory = iota
	CategorySecurity
	CategoryProtocol
	CategoryTransport
	CategoryStreaming
	CategoryInput
	CategoryFileTransfer
	CategoryUI
	CategorySystem
)

// RemoteDexterError represents a structured error in the system
type RemoteDexterError struct {
	Level       ErrorLevel
	Category    ErrorCategory
	Code        string
	Message     string
	Details     string
	Timestamp   time.Time
	Recoverable bool
	UserMessage string
	Cause       error
}

// Error implements the error interface
func (e *RemoteDexterError) Error() string {
	return fmt.Sprintf("[%s] %s: %s", e.Code, e.Message, e.Details)
}

// ErrorHandler manages error handling and recovery across the system
type ErrorHandler struct {
	errorChan chan *RemoteDexterError
	logger    *log.Logger
}

// NewErrorHandler creates a new error handler
func NewErrorHandler(logger *log.Logger) *ErrorHandler {
	return &ErrorHandler{
		errorChan: make(chan *RemoteDexterError, 100),
		logger:    logger,
	}
}

// Start begins the error handling loop
func (eh *ErrorHandler) Start() {
	go eh.handleErrors()
}

// ReportError reports an error to the centralized handler
func (eh *ErrorHandler) ReportError(level ErrorLevel, category ErrorCategory, code, message, details string, recoverable bool, cause error) {
	err := &RemoteDexterError{
		Level:       level,
		Category:    category,
		Code:        code,
		Message:     message,
		Details:     details,
		Timestamp:   time.Now(),
		Recoverable: recoverable,
		UserMessage: eh.convertToUserMessage(level, category, code, message),
		Cause:       cause,
	}

	select {
	case eh.errorChan <- err:
	default:
		// Channel is full, log directly
		eh.logger.Printf("Error channel full, logging directly: %v", err)
	}
}

// handleErrors processes errors from the channel
func (eh *ErrorHandler) handleErrors() {
	for err := range eh.errorChan {
		eh.processError(err)
	}
}

// processError handles individual errors
func (eh *ErrorHandler) processError(err *RemoteDexterError) {
	// Log the error
	eh.logError(err)

	// Handle recovery for recoverable errors
	if err.Recoverable {
		eh.attemptRecovery(err)
	}

	// Notify UI if necessary
	if err.Level >= LevelWarn {
		eh.notifyUI(err)
	}
}

// logError logs the error with appropriate level
func (eh *ErrorHandler) logError(err *RemoteDexterError) {
	logMsg := fmt.Sprintf("%s [%s] %s: %s",
		err.Timestamp.Format("2006-01-02 15:04:05"),
		err.Code,
		err.Message,
		err.Details)

	switch err.Level {
	case LevelInfo:
		eh.logger.Println("INFO:", logMsg)
	case LevelWarn:
		eh.logger.Println("WARN:", logMsg)
	case LevelError:
		eh.logger.Println("ERROR:", logMsg)
	}

	// Log cause if present
	if err.Cause != nil {
		eh.logger.Printf("CAUSE: %v", err.Cause)
	}
}

// attemptRecovery tries to recover from recoverable errors
func (eh *ErrorHandler) attemptRecovery(err *RemoteDexterError) {
	switch err.Category {
	case CategoryNetwork:
		eh.handleNetworkRecovery(err)
	case CategoryTransport:
		eh.handleTransportRecovery(err)
	case CategoryStreaming:
		eh.handleStreamingRecovery(err)
	case CategoryFileTransfer:
		eh.handleFileTransferRecovery(err)
	default:
		eh.logger.Printf("No recovery strategy for category: %v", err.Category)
	}
}

// handleNetworkRecovery handles network-related error recovery
func (eh *ErrorHandler) handleNetworkRecovery(err *RemoteDexterError) {
	switch err.Code {
	case "NET_TIMEOUT":
		eh.logger.Println("Attempting network timeout recovery...")
		// In a real implementation, this would retry the operation
	case "NET_DISCONNECT":
		eh.logger.Println("Attempting network reconnection...")
		// In a real implementation, this would attempt reconnection
	}
}

// handleTransportRecovery handles transport-related error recovery
func (eh *ErrorHandler) handleTransportRecovery(err *RemoteDexterError) {
	switch err.Code {
	case "TRANS_FALLBACK":
		eh.logger.Println("Attempting transport fallback...")
		// In a real implementation, this would try alternative transports
	}
}

// handleStreamingRecovery handles streaming-related error recovery
func (eh *ErrorHandler) handleStreamingRecovery(err *RemoteDexterError) {
	switch err.Code {
	case "STREAM_DROP":
		eh.logger.Println("Attempting streaming recovery...")
		// In a real implementation, this would restart streaming
	}
}

// handleFileTransferRecovery handles file transfer error recovery
func (eh *ErrorHandler) handleFileTransferRecovery(err *RemoteDexterError) {
	switch err.Code {
	case "FT_CHUNK_LOST":
		eh.logger.Println("Attempting file transfer chunk recovery...")
		// In a real implementation, this would retry the chunk
	}
}

// notifyUI sends error notifications to the UI
func (eh *ErrorHandler) notifyUI(err *RemoteDexterError) {
	// In a real implementation, this would send to UI event bus
	eh.logger.Printf("UI Notification: %s", err.UserMessage)
}

// convertToUserMessage converts technical errors to user-friendly messages
func (eh *ErrorHandler) convertToUserMessage(level ErrorLevel, category ErrorCategory, code, message string) string {
	messages := map[string]string{
		"NET_TIMEOUT":        "Connection timed out. Please check your network and try again.",
		"NET_DISCONNECT":     "Connection lost. Attempting to reconnect...",
		"NET_UNREACHABLE":    "Cannot reach the remote device. Check if it's online and try again.",
		"TRANS_USB_LOST":     "USB connection lost. Please check the cable.",
		"TRANS_WIFI_LOST":    "Wi-Fi connection lost. Attempting to reconnect...",
		"TRANS_BT_LOST":      "Bluetooth connection lost. Please check device pairing.",
		"TRANS_NO_AVAILABLE": "No suitable connection method available. Please check device connections.",
		"SEC_HANDSHAKE_FAIL": "Secure connection failed. Please try pairing the device again.",
		"SEC_KEY_INVALID":    "Security key invalid. Device may need to be re-paired.",
		"STREAM_START_FAIL":  "Failed to start screen sharing. Please try again.",
		"STREAM_ENCODER_ERR": "Screen encoding error. Restarting stream...",
		"STREAM_DECODER_ERR": "Screen decoding error. Please restart the session.",
		"INPUT_INJECT_FAIL":  "Input control failed. Please restart input control.",
		"INPUT_PERMISSION":   "Input permissions missing. Please grant input permissions.",
		"FT_REJECTED":        "File transfer was rejected by the remote device.",
		"FT_SIZE_EXCEEDED":   "File too large. Maximum size is 2GB.",
		"FT_STORAGE_FULL":    "Not enough storage space on device.",
		"FT_PERMISSION":      "File access permission denied.",
		"SYS_RESOURCE_LOW":   "System resources low. Please close other applications.",
		"SYS_MEMORY_LOW":     "Memory running low. Please restart the application.",
	}

	if userMsg, exists := messages[code]; exists {
		return userMsg
	}

	switch category {
	case CategoryNetwork:
		return "Network connection issue. Please check your connection and try again."
	case CategorySecurity:
		return "Security error occurred. Please try re-pairing your device."
	case CategoryTransport:
		return "Connection error. Please check device connections and try again."
	case CategoryStreaming:
		return "Screen sharing error. Please restart the session."
	case CategoryInput:
		return "Input control error. Please restart input control."
	case CategoryFileTransfer:
		return "File transfer error. Please try again."
	default:
		return "An error occurred. Please try again or restart the application."
	}
}

// GetErrorStats returns error statistics for diagnostics
func (eh *ErrorHandler) GetErrorStats() map[string]int {
	return map[string]int{
		"total_errors":     0,
		"network_errors":   0,
		"transport_errors": 0,
		"security_errors":  0,
		"streaming_errors": 0,
		"input_errors":     0,
		"file_errors":      0,
	}
}

// Close shuts down the error handler
func (eh *ErrorHandler) Close() {
	close(eh.errorChan)
}

// NewNetworkError creates a network-related error
func NewNetworkError(code, message, details string, recoverable bool) *RemoteDexterError {
	return &RemoteDexterError{
		Level:       LevelError,
		Category:    CategoryNetwork,
		Code:        code,
		Message:     message,
		Details:     details,
		Timestamp:   time.Now(),
		Recoverable: recoverable,
	}
}

// NewTransportError creates a transport-related error
func NewTransportError(code, message, details string, recoverable bool) *RemoteDexterError {
	level := LevelError
	if recoverable {
		level = LevelWarn
	}

	return &RemoteDexterError{
		Level:       level,
		Category:    CategoryTransport,
		Code:        code,
		Message:     message,
		Details:     details,
		Timestamp:   time.Now(),
		Recoverable: recoverable,
	}
}

// NewStreamingError creates a streaming-related error
func NewStreamingError(code, message, details string, recoverable bool) *RemoteDexterError {
	level := LevelError
	if recoverable {
		level = LevelWarn
	}

	return &RemoteDexterError{
		Level:       level,
		Category:    CategoryStreaming,
		Code:        code,
		Message:     message,
		Details:     details,
		Timestamp:   time.Now(),
		Recoverable: recoverable,
	}
}
