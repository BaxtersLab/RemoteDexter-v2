package session

import (
	"encoding/json"
	"fmt"
	"remotedexter/desktop/internal/input"
	"remotedexter/desktop/internal/security"
	"remotedexter/desktop/internal/streaming"
	"remotedexter/desktop/internal/transfer"
	"remotedexter/desktop/internal/transport"
	"remotedexter/desktop/shared/protocol"
	"sync"
	"time"
)

// RateLimiter implements token bucket rate limiting
type RateLimiter struct {
	tokens     float64
	maxTokens  float64
	refillRate float64 // tokens per second
	lastRefill time.Time
	mu         sync.Mutex
}

// NewRateLimiter creates a new rate limiter
func NewRateLimiter(maxTokens, refillRate float64) *RateLimiter {
	return &RateLimiter{
		tokens:     maxTokens,
		maxTokens:  maxTokens,
		refillRate: refillRate,
		lastRefill: time.Now(),
	}
}

// Allow checks if an action is allowed and consumes a token
func (rl *RateLimiter) Allow() bool {
	rl.mu.Lock()
	defer rl.mu.Unlock()

	now := time.Now()
	elapsed := now.Sub(rl.lastRefill).Seconds()
	rl.tokens += elapsed * rl.refillRate
	if rl.tokens > rl.maxTokens {
		rl.tokens = rl.maxTokens
	}
	rl.lastRefill = now

	if rl.tokens >= 1.0 {
		rl.tokens -= 1.0
		return true
	}
	return false
}

// SessionController manages the lifecycle of remote sessions
type SessionController struct {
	state        *RemoteSessionState
	selector     *transport.Selector
	streaming    *streaming.StreamingSession
	inputCapture *input.InputCapture
	trustStore   *security.FileBasedTrustStore
	validator    *protocol.CommandValidator
	sessionKey   []byte
	nonce        uint64
	isActive     bool

	// Resource limits and abuse prevention
	commandRateLimiter *RateLimiter
	recentCommands     []*protocol.CommandRequest
	abuseWarnings      int
	maxAbuseWarnings   int

	// File transfer components
	fileSender   *transfer.FileSender
	fileReceiver *transfer.FileReceiver
}

// NewSessionController creates a new session controller
func NewSessionController(dataDir string) (*SessionController, error) {
	trustStore, err := security.NewFileBasedTrustStore(dataDir)
	if err != nil {
		return nil, fmt.Errorf("failed to initialize trust store: %v", err)
	}

	return &SessionController{
		state:              NewSessionState(),
		selector:           transport.NewSelector(),
		streaming:          streaming.NewStreamingSession(),
		inputCapture:       input.NewInputCapture(),
		trustStore:         trustStore,
		validator:          protocol.NewCommandValidator(),
		commandRateLimiter: NewRateLimiter(100, 50), // 100 burst, 50 per second
		recentCommands:     make([]*protocol.CommandRequest, 0, 100),
		abuseWarnings:      0,
		maxAbuseWarnings:   3,
		isActive:           false,
	}, nil
}
}

// StartRemoteSession initiates a complete remote session
func (sc *SessionController) StartRemoteSession() error {
	if sc.isActive {
		return fmt.Errorf("session already active")
	}

	fmt.Println("Starting remote session...")

	// Step 1: Detect and establish transport connection
	transportType := sc.selector.SelectTransport()
	if transportType == "" {
		return fmt.Errorf("no suitable transport available")
	}

	sc.state.UpdateTransport(transportType)
	sc.state.SetConnected(true)
	fmt.Printf("Transport selected: %s\n", transportType)

	// Step 2: Establish Noise session
	sessionKey, err := sc.establishNoiseSession()
	if err != nil {
		sc.state.SetConnected(false)
		return fmt.Errorf("failed to establish Noise session: %v", err)
	}
	sc.sessionKey = sessionKey
	sc.nonce = 1

	// Step 3: Initialize streaming with session keys
	sc.streaming.SetSessionKeys(sessionKey, sc.nonce)
	if err := sc.streaming.StartStreaming(); err != nil {
		sc.state.IncrementError()
		sc.zeroizeSessionKeys()
		sc.state.SetConnected(false)
		return fmt.Errorf("failed to start streaming: %v", err)
	}
	sc.state.SetStreaming(true)

	// Step 4: Enable input control automatically
	sc.inputCapture.StartCapture(sessionKey, &sc.nonce)
	sc.state.SetInputEnabled(true)

	sc.isActive = true
	sc.state.ResetErrors()

	fmt.Println("Remote session started successfully")
	return nil
}

// establishNoiseSession performs the Noise protocol handshake
func (sc *SessionController) establishNoiseSession() ([]byte, error) {
	// In a real implementation, this would perform the full Noise handshake
	// For now, simulate the handshake process

	fmt.Println("Performing Noise protocol handshake...")

	// Simulate handshake delay
	time.Sleep(100 * time.Millisecond)

	// Generate session key (32 bytes for ChaCha20-Poly1305)
	sessionKey := make([]byte, 32)
	// In real implementation, this would be derived from the handshake
	copy(sessionKey, []byte("simulated-session-key-32-bytes"))

	fmt.Println("Noise session established successfully")
	return sessionKey, nil
}

// zeroizeSessionKeys securely wipes session keys from memory
func (sc *SessionController) zeroizeSessionKeys() {
	if sc.sessionKey != nil {
		// Zero out the session key
		for i := range sc.sessionKey {
			sc.sessionKey[i] = 0
		}
		sc.sessionKey = nil
	}
	sc.nonce = 0
	fmt.Println("Session keys zeroized")
}

// StopRemoteSession terminates the remote session with proper teardown sequencing
func (sc *SessionController) StopRemoteSession() error {
	if !sc.isActive {
		return fmt.Errorf("no active session")
	}

	fmt.Println("Stopping remote session...")

	// Step 1: Stop input injection first (reverse order of startup)
	sc.inputCapture.StopCapture()
	sc.state.SetInputEnabled(false)
	fmt.Println("Input control stopped")

	// Step 2: Stop streaming
	if err := sc.streaming.StopStreaming(); err != nil {
		fmt.Printf("Warning: error stopping streaming: %v\n", err)
	}
	sc.state.SetStreaming(false)
	fmt.Println("Streaming stopped")

	// Step 3: Close transport connections
	if err := sc.selector.Close(); err != nil {
		fmt.Printf("Warning: error closing transport: %v\n", err)
	}
	sc.state.SetConnected(false)
	fmt.Println("Transport connections closed")

	// Step 4: Zeroize session keys
	sc.zeroizeSessionKeys()

	// Step 5: Update UI to idle state
	sc.isActive = false
	sc.state.ResetErrors()

	fmt.Println("Remote session stopped and cleaned up")
	return nil
}

// AttachStreaming attaches streaming to the session
func (sc *SessionController) AttachStreaming() error {
	if !sc.isActive {
		return fmt.Errorf("no active session")
	}

	return sc.streaming.StartStreaming()
}

// AttachInput attaches input control to the session
func (sc *SessionController) AttachInput() error {
	if !sc.isActive {
		return fmt.Errorf("no active session")
	}

	sc.inputCapture.StartCapture(sc.sessionKey, &sc.nonce)
	sc.state.SetInputEnabled(true)
	return nil
}

// HandleTransportFailure handles transport layer failures
func (sc *SessionController) HandleTransportFailure(reason string) {
	fmt.Printf("Transport failure: %s, attempting recovery\n", reason)
	sc.state.IncrementError()

	// If we have too many errors, stop the session
	if sc.state.GetState().ErrorCount > 3 {
		fmt.Println("Too many transport failures, stopping session")
		sc.StopRemoteSession()
		return
	}

	// Attempt to restart with different transport
	// In a real implementation, this would try fallback transports
	fmt.Println("Attempting session recovery...")
}

// HandleStreamingFailure handles streaming failures
func (sc *SessionController) HandleStreamingFailure(reason string) {
	fmt.Printf("Streaming failure: %s\n", reason)
	sc.state.IncrementError()
	sc.state.SetStreaming(false)

	// Keep the session alive but disable streaming
	// User can retry streaming without restarting the whole session
}

// HandleInputFailure handles input control failures
func (sc *SessionController) HandleInputFailure(reason string) {
	fmt.Printf("Input failure: %s\n", reason)
	sc.state.IncrementError()
	sc.state.SetInputEnabled(false)

	// Keep streaming active but disable input
	// User can re-enable input without stopping streaming
}

// GetState returns the current session state
func (sc *SessionController) GetState() RemoteSessionState {
	return sc.state.GetState()
}

// IsHealthy checks if the session is healthy
func (sc *SessionController) IsHealthy() bool {
	// If session is not active, consider it "healthy" (no problems)
	if !sc.isActive {
		return true
	}
	// If session is active, check the underlying state
	return sc.state.IsHealthy()
}

// SendCommand sends a command through the session with validation and abuse prevention
func (sc *SessionController) SendCommand(cmdType string, payload []byte) (protocol.CommandResponse, error) {
	if !sc.isActive {
		return protocol.CommandResponse{}, fmt.Errorf("no active session")
	}

	// Create command request
	req := &protocol.CommandRequest{
		Type:    cmdType,
		Payload: payload,
	}

	// Validate command
	if err := sc.validator.ValidateCommand(req); err != nil {
		return protocol.CommandResponse{}, fmt.Errorf("command validation failed: %v", err)
	}

	// Check rate limiting
	if !sc.commandRateLimiter.Allow() {
		sc.abuseWarnings++
		if sc.abuseWarnings >= sc.maxAbuseWarnings {
			fmt.Println("Abuse detected: terminating session due to rate limiting violations")
			sc.StopRemoteSession()
			return protocol.CommandResponse{}, fmt.Errorf("session terminated due to abuse")
		}
		return protocol.CommandResponse{}, fmt.Errorf("rate limit exceeded")
	}

	// Check for abusive patterns
	if sc.validator.IsAbusiveCommand(req, sc.recentCommands) {
		sc.abuseWarnings++
		if sc.abuseWarnings >= sc.maxAbuseWarnings {
			fmt.Println("Abuse detected: terminating session due to abusive command patterns")
			sc.StopRemoteSession()
			return protocol.CommandResponse{}, fmt.Errorf("session terminated due to abuse")
		}
		fmt.Println("Warning: abusive command pattern detected")
	}

	// Track recent commands (keep last 50 for abuse detection)
	sc.recentCommands = append(sc.recentCommands, req)
	if len(sc.recentCommands) > 50 {
		sc.recentCommands = sc.recentCommands[1:]
	}

	// Sanitize command
	sanitizedReq := sc.validator.SanitizeCommand(req)

	// Send command
	return sc.selector.SendCommand(*sanitizedReq, sc.sessionKey, &sc.nonce)
}

// RevokeDevice removes a device from the trusted list and terminates any active sessions
func (sc *SessionController) RevokeDevice(deviceID string) error {
	fmt.Printf("Revoking device: %s\n", deviceID)

	// Remove from trust store
	if err := sc.trustStore.RemoveDevice(deviceID); err != nil {
		return fmt.Errorf("failed to remove device from trust store: %v", err)
	}

	// If there's an active session with this device, terminate it
	// In a real implementation, we'd check if the current session is with this device
	if sc.isActive {
		fmt.Println("Terminating active session due to revocation")
		sc.StopRemoteSession()
	}

	fmt.Printf("Device %s successfully revoked\n", deviceID)
	return nil
}

// AddTrustedDevice adds a new device to the trusted list
func (sc *SessionController) AddTrustedDevice(deviceName string, publicKey []byte) error {
	return sc.trustStore.AddDevice(deviceName, publicKey)
}

// ListTrustedDevices returns all trusted devices
func (sc *SessionController) ListTrustedDevices() ([]security.TrustedDevice, error) {
	return sc.trustStore.ListDevices()
}

// IsDeviceTrusted checks if a device is trusted
func (sc *SessionController) IsDeviceTrusted(publicKey []byte) bool {
	return sc.trustStore.IsTrusted(publicKey)
}

// LostDeviceProtocol clears all trusted devices and invalidates all pairings
func (sc *SessionController) LostDeviceProtocol() error {
	fmt.Println("Executing lost device protocol - clearing all trusted devices")

	// Clear all trusted devices
	// Note: In a real implementation, this would also rotate the local keypair
	if err := sc.trustStore.ClearAllDevices(); err != nil {
		return fmt.Errorf("failed to clear trusted devices: %v", err)
	}

	// Terminate any active session
	if sc.isActive {
		fmt.Println("Terminating active session due to lost device protocol")
		sc.StopRemoteSession()
	}

	fmt.Println("Lost device protocol completed - all previous pairings invalidated")
	return nil
}

// RotateKeys performs a key rotation operation, generating new keys and updating all trusted devices
func (sc *SessionController) RotateKeys() error {
	fmt.Println("Starting key rotation process...")

	// Terminate any active session first
	if sc.isActive {
		fmt.Println("Terminating active session for key rotation")
		sc.StopRemoteSession()
	}

	// Perform key rotation using the keyrotation manager
	rotator := security.NewKeyRotator(sc.trustStore)
	if err := rotator.RotateKeys(); err != nil {
		return fmt.Errorf("key rotation failed: %v", err)
	}

	fmt.Println("Key rotation completed successfully")
	return nil
}

// ProcessCommand handles incoming commands from the remote device
func (sc *SessionController) ProcessCommand(cmd protocol.CommandRequest) error {
	// Validate command
	if err := sc.validator.ValidateCommand(cmd); err != nil {
		return fmt.Errorf("command validation failed: %v", err)
	}

	// Check rate limiting
	if !sc.commandRateLimiter.Allow() {
		sc.abuseWarnings++
		if sc.abuseWarnings >= sc.maxAbuseWarnings {
			fmt.Println("Abuse detected: terminating session due to rate limiting violations")
			sc.StopRemoteSession()
			return fmt.Errorf("session terminated due to abuse")
		}
		return fmt.Errorf("rate limit exceeded")
	}

	// Check for abusive patterns
	if sc.validator.IsAbusiveCommand(&cmd, sc.recentCommands) {
		sc.abuseWarnings++
		if sc.abuseWarnings >= sc.maxAbuseWarnings {
			fmt.Println("Abuse detected: terminating session due to abusive command patterns")
			sc.StopRemoteSession()
			return fmt.Errorf("session terminated due to abuse")
		}
		fmt.Println("Warning: abusive command pattern detected")
	}

	// Track recent commands (keep last 50 for abuse detection)
	sc.recentCommands = append(sc.recentCommands, &cmd)
	if len(sc.recentCommands) > 50 {
		sc.recentCommands = sc.recentCommands[1:]
	}

	// Handle command based on type
	switch cmd.Type {
	case "file_offer":
		return sc.handleFileOffer(cmd.Payload)
	case "file_accept":
		return sc.handleFileAccept(cmd.Payload)
	case "file_reject":
		return sc.handleFileReject(cmd.Payload)
	case "file_chunk":
		return sc.handleFileChunk(cmd.Payload)
	default:
		// For other commands, they would be handled by the UI or other components
		fmt.Printf("Received unhandled command: %s\n", cmd.Type)
		return nil
	}
}

// handleFileOffer processes a file offer from the remote device
func (sc *SessionController) handleFileOffer(payload []byte) error {
	if sc.fileReceiver == nil {
		return fmt.Errorf("file receiver not initialized")
	}

	var offer protocol.FileOffer
	if err := json.Unmarshal(payload, &offer); err != nil {
		return fmt.Errorf("failed to unmarshal file offer: %v", err)
	}

	sc.fileReceiver.HandleFileOffer(offer)
	return nil
}

// handleFileAccept processes a file acceptance response
func (sc *SessionController) handleFileAccept(payload []byte) error {
	if sc.fileSender == nil {
		return fmt.Errorf("file sender not initialized")
	}

	var accept protocol.FileAccept
	if err := json.Unmarshal(payload, &accept); err != nil {
		return fmt.Errorf("failed to unmarshal file accept: %v", err)
	}

	sc.fileSender.HandleFileAccept(accept.FileID)
	return nil
}

// handleFileReject processes a file rejection response
func (sc *SessionController) handleFileReject(payload []byte) error {
	if sc.fileSender == nil {
		return fmt.Errorf("file sender not initialized")
	}

	var reject protocol.FileReject
	if err := json.Unmarshal(payload, &reject); err != nil {
		return fmt.Errorf("failed to unmarshal file reject: %v", err)
	}

	sc.fileSender.HandleFileReject(reject.FileID, reject.Reason)
	return nil
}

// handleFileChunk processes a file chunk from the remote device
func (sc *SessionController) handleFileChunk(payload []byte) error {
	if sc.fileReceiver == nil {
		return fmt.Errorf("file receiver not initialized")
	}

	var chunk protocol.FileChunk
	if err := json.Unmarshal(payload, &chunk); err != nil {
		return fmt.Errorf("failed to unmarshal file chunk: %v", err)
	}

	sc.fileReceiver.HandleFileChunk(chunk)
	return nil
}

// InitializeFileTransfer sets up file transfer components
func (sc *SessionController) InitializeFileTransfer(destDir string, acceptCallback func(fileID, fileName string, size int64) bool) error {
	// Create file sender
	sc.fileSender = transfer.NewFileSender(transfer.DefaultFileTransferConfig(), func(cmd protocol.CommandRequest) (protocol.CommandResponse, error) {
		return sc.SendCommand(cmd.Type, cmd.Payload)
	})

	// Create file receiver
	sc.fileReceiver = transfer.NewFileReceiver(transfer.DefaultFileTransferConfig(), func(cmd protocol.CommandRequest) (protocol.CommandResponse, error) {
		return sc.SendCommand(cmd.Type, cmd.Payload)
	}, acceptCallback, destDir)

	return nil
}

// SendFile initiates a file transfer to the remote device
func (sc *SessionController) SendFile(filePath string) error {
	if sc.fileSender == nil {
		return fmt.Errorf("file transfer not initialized")
	}
	return sc.fileSender.SendFile(filePath)
}

// GetFileTransferStatus returns the status of a file transfer
func (sc *SessionController) GetFileTransferStatus(fileID string) (*transfer.FileTransfer, error) {
	if sc.fileSender == nil {
		return nil, fmt.Errorf("file transfer not initialized")
	}
	return sc.fileSender.GetTransferStatus(fileID)
}

// ListActiveFileTransfers returns all active file transfers
func (sc *SessionController) ListActiveFileTransfers() []*transfer.FileTransfer {
	if sc.fileSender == nil {
		return []*transfer.FileTransfer{}
	}
	return sc.fileSender.ListActiveTransfers()
}

// CancelFileTransfer cancels an active file transfer
func (sc *SessionController) CancelFileTransfer(fileID string) error {
	if sc.fileSender == nil {
		return fmt.Errorf("file transfer not initialized")
	}
	return sc.fileSender.CancelTransfer(fileID)
}
