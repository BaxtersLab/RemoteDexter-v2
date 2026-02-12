package security

import (
	"bytes"
	"crypto/rand"
	"encoding/json"
	"fmt"
	"os"
	"testing"
	"time"

	"remotedexter/desktop/internal/security"
	"remotedexter/desktop/shared/noise"
	"remotedexter/desktop/shared/protocol"
)

// TestSecurityRegressionSuite runs comprehensive security regression tests
func TestSecurityRegressionSuite(t *testing.T) {
	// Create temporary directory for tests
	tempDir, err := os.MkdirTemp("", "rd-security-test-*")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	t.Run("TrustStoreSecurity", func(t *testing.T) { testTrustStoreSecurity(t, tempDir) })
	t.Run("NoiseProtocolValidation", func(t *testing.T) { testNoiseProtocolValidation(t) })
	t.Run("CommandValidation", func(t *testing.T) { testCommandValidation(t) })
	t.Run("ReplayProtection", func(t *testing.T) { testReplayProtection(t) })
	t.Run("RateLimiting", func(t *testing.T) { testRateLimiting(t, tempDir) })
	t.Run("KeyRotation", func(t *testing.T) { testKeyRotation(t, tempDir) })
	t.Run("TamperedFrameDetection", func(t *testing.T) { testTamperedFrameDetection(t) })
	t.Run("MalformedPayloadHandling", func(t *testing.T) { testMalformedPayloadHandling(t) })
}

// testTrustStoreSecurity validates trust store security properties
func testTrustStoreSecurity(t *testing.T, tempDir string) {
	store, err := security.NewFileBasedTrustStore(tempDir)
	if err != nil {
		t.Fatalf("Failed to create trust store: %v", err)
	}

	// Test device addition and trust verification
	testKey := make([]byte, 32)
	if _, err := rand.Read(testKey); err != nil {
		t.Fatalf("Failed to generate test key: %v", err)
	}

	// Add device
	if err := store.AddDevice("test-device", testKey); err != nil {
		t.Fatalf("Failed to add device: %v", err)
	}

	// Verify trust
	if !store.IsTrusted(testKey) {
		t.Error("Device should be trusted after addition")
	}

	// Test revocation
	devices, err := store.ListDevices()
	if err != nil {
		t.Fatalf("Failed to list devices: %v", err)
	}
	if len(devices) != 1 {
		t.Errorf("Expected 1 device, got %d", len(devices))
	}

	if err := store.RemoveDevice(devices[0].DeviceID); err != nil {
		t.Fatalf("Failed to revoke device: %v", err)
	}

	if store.IsTrusted(testKey) {
		t.Error("Device should not be trusted after revocation")
	}
}

// testNoiseProtocolValidation validates cryptographic operations
func testNoiseProtocolValidation(t *testing.T) {
	// Test key generation
	localKeypair, err := noise.GenerateKeypair()
	if err != nil {
		t.Fatalf("Failed to generate local keypair: %v", err)
	}

	remoteKeypair, err := noise.GenerateKeypair()
	if err != nil {
		t.Fatalf("Failed to generate remote keypair: %v", err)
	}

	// Test handshake
	localSession, err := noise.NewSession(localKeypair, remoteKeypair.Public, true)
	if err != nil {
		t.Fatalf("Failed to create local session: %v", err)
	}

	remoteSession, err := noise.NewSession(remoteKeypair, localKeypair.Public, false)
	if err != nil {
		t.Fatalf("Failed to create remote session: %v", err)
	}

	// Perform handshake
	localMsg, err := localSession.InitiateHandshake()
	if err != nil {
		t.Fatalf("Failed to initiate handshake: %v", err)
	}

	if err := remoteSession.ReceiveHandshake(localMsg); err != nil {
		t.Fatalf("Failed to receive handshake: %v", err)
	}

	remoteMsg, err := remoteSession.RespondHandshake()
	if err != nil {
		t.Fatalf("Failed to respond to handshake: %v", err)
	}

	if err := localSession.CompleteHandshake(remoteMsg); err != nil {
		t.Fatalf("Failed to complete handshake: %v", err)
	}

	// Test encryption/decryption
	testData := []byte("Hello, secure world!")
	encrypted, err := localSession.Encrypt(testData)
	if err != nil {
		t.Fatalf("Failed to encrypt: %v", err)
	}

	decrypted, err := remoteSession.Decrypt(encrypted)
	if err != nil {
		t.Fatalf("Failed to decrypt: %v", err)
	}

	if !bytes.Equal(testData, decrypted) {
		t.Error("Decrypted data does not match original")
	}
}

// testCommandValidation validates command sanitization and validation
func testCommandValidation(t *testing.T) {
	validator := protocol.NewCommandValidator()

	// Test valid commands
	validCommands := []protocol.CommandRequest{
		{Type: "ping", Payload: []byte("")},
		{Type: "set_input_mode", Payload: []byte("mouse")},
		{Type: "start_streaming", Payload: []byte("")},
	}

	for _, cmd := range validCommands {
		if err := validator.ValidateCommand(cmd); err != nil {
			t.Errorf("Valid command %s should pass validation: %v", cmd.Type, err)
		}
	}

	// Test invalid commands
	invalidCommands := []protocol.CommandRequest{
		{Type: "", Payload: []byte("invalid")},                    // Empty type
		{Type: "unknown_command", Payload: []byte("")},            // Unknown command
		{Type: "set_input_mode", Payload: []byte("invalid_mode")}, // Invalid payload
		{Type: "ping", Payload: make([]byte, 1025)},               // Oversized payload
	}

	for _, cmd := range invalidCommands {
		if err := validator.ValidateCommand(cmd); err == nil {
			t.Errorf("Invalid command %s should fail validation", cmd.Type)
		}
	}

	// Test command sanitization
	maliciousPayload := []byte(`{"command": "rm -rf /", "data": "<script>alert('xss')</script>"}`)
	sanitized := validator.SanitizePayload(maliciousPayload)

	// Ensure dangerous content is removed
	if bytes.Contains(sanitized, []byte("rm -rf")) {
		t.Error("Malicious command should be sanitized")
	}
	if bytes.Contains(sanitized, []byte("<script>")) {
		t.Error("XSS payload should be sanitized")
	}
}

// testReplayProtection validates replay attack prevention
func testReplayProtection(t *testing.T) {
	localKeypair, err := noise.GenerateKeypair()
	if err != nil {
		t.Fatalf("Failed to generate local keypair: %v", err)
	}

	remoteKeypair, err := noise.GenerateKeypair()
	if err != nil {
		t.Fatalf("Failed to generate remote keypair: %v", err)
	}

	localSession, err := noise.NewSession(localKeypair, remoteKeypair.Public, true)
	if err != nil {
		t.Fatalf("Failed to create local session: %v", err)
	}

	remoteSession, err := noise.NewSession(remoteKeypair, localKeypair.Public, false)
	if err != nil {
		t.Fatalf("Failed to create remote session: %v", err)
	}

	// Complete handshake
	localMsg, _ := localSession.InitiateHandshake()
	remoteSession.ReceiveHandshake(localMsg)
	remoteMsg, _ := remoteSession.RespondHandshake()
	localSession.CompleteHandshake(remoteMsg)

	// Encrypt a message
	testData := []byte("test message")
	encrypted, err := localSession.Encrypt(testData)
	if err != nil {
		t.Fatalf("Failed to encrypt: %v", err)
	}

	// Decrypt successfully
	decrypted, err := remoteSession.Decrypt(encrypted)
	if err != nil {
		t.Fatalf("Failed to decrypt: %v", err)
	}
	if !bytes.Equal(testData, decrypted) {
		t.Error("First decryption failed")
	}

	// Attempt replay attack - should fail
	_, err = remoteSession.Decrypt(encrypted)
	if err == nil {
		t.Error("Replay attack should be detected and prevented")
	}
}

// testRateLimiting validates abuse prevention
func testRateLimiting(t *testing.T, tempDir string) {
	// Create a simple rate limiter for testing
	rl := NewRateLimiter(100, 50)

	// Test normal rate limiting
	start := time.Now()
	commandCount := 0
	maxCommands := 150 // Above the burst limit of 100

	for i := 0; i < maxCommands; i++ {
		if rl.Allow() {
			commandCount++
		} else {
			break
		}
	}

	if commandCount < 100 {
		t.Errorf("Expected at least 100 commands before rate limiting, got %d", commandCount)
	}

	// Test that rate limiting recovers over time
	time.Sleep(3 * time.Second) // Wait for token refill

	if !rl.Allow() {
		t.Error("Rate limiting should have recovered after waiting")
	}
}

// testKeyRotation validates key rotation functionality
func testKeyRotation(t *testing.T, tempDir string) {
	store, err := security.NewFileBasedTrustStore(tempDir)
	if err != nil {
		t.Fatalf("Failed to create trust store: %v", err)
	}

	// Add some test devices
	for i := 0; i < 3; i++ {
		key := make([]byte, 32)
		rand.Read(key)
		store.AddDevice(fmt.Sprintf("device-%d", i), key)
	}

	originalDevices, err := store.ListDevices()
	if err != nil {
		t.Fatalf("Failed to list original devices: %v", err)
	}

	// Perform key rotation
	rotator := security.NewKeyRotator(store)
	if err := rotator.RotateKeys(); err != nil {
		t.Fatalf("Key rotation failed: %v", err)
	}

	// Verify devices are still present but with updated keys
	rotatedDevices, err := store.ListDevices()
	if err != nil {
		t.Fatalf("Failed to list rotated devices: %v", err)
	}

	if len(rotatedDevices) != len(originalDevices) {
		t.Errorf("Device count changed after rotation: %d -> %d", len(originalDevices), len(rotatedDevices))
	}

	// Verify device names are preserved but keys are different
	for i, orig := range originalDevices {
		rotated := rotatedDevices[i]
		if orig.DeviceName != rotated.DeviceName {
			t.Errorf("Device name changed: %s -> %s", orig.DeviceName, rotated.DeviceName)
		}
		if bytes.Equal(orig.PublicKey, rotated.PublicKey) {
			t.Errorf("Device key was not rotated for %s", orig.DeviceName)
		}
	}
}

// testTamperedFrameDetection validates detection of tampered encrypted frames
func testTamperedFrameDetection(t *testing.T) {
	localKeypair, err := noise.GenerateKeypair()
	if err != nil {
		t.Fatalf("Failed to generate local keypair: %v", err)
	}

	remoteKeypair, err := noise.GenerateKeypair()
	if err != nil {
		t.Fatalf("Failed to generate remote keypair: %v", err)
	}

	localSession, err := noise.NewSession(localKeypair, remoteKeypair.Public, true)
	if err != nil {
		t.Fatalf("Failed to create local session: %v", err)
	}

	remoteSession, err := noise.NewSession(remoteKeypair, localKeypair.Public, false)
	if err != nil {
		t.Fatalf("Failed to create remote session: %v", err)
	}

	// Complete handshake
	localMsg, _ := localSession.InitiateHandshake()
	remoteSession.ReceiveHandshake(localMsg)
	remoteMsg, _ := remoteSession.RespondHandshake()
	localSession.CompleteHandshake(remoteMsg)

	// Encrypt a message
	testData := []byte("test message")
	encrypted, err := localSession.Encrypt(testData)
	if err != nil {
		t.Fatalf("Failed to encrypt: %v", err)
	}

	// Tamper with the encrypted data
	if len(encrypted) > 10 {
		encrypted[10] ^= 0xFF // Flip a bit
	}

	// Attempt to decrypt tampered data - should fail
	_, err = remoteSession.Decrypt(encrypted)
	if err == nil {
		t.Error("Tampered frame should be detected and rejected")
	}
}

// testMalformedPayloadHandling validates handling of malformed payloads
func testMalformedPayloadHandling(t *testing.T) {
	validator := protocol.NewCommandValidator()

	// Test various malformed payloads
	malformedPayloads := [][]byte{
		[]byte(""),         // Empty payload
		[]byte("not json"), // Invalid JSON
		[]byte(`{"type": "ping", "payload": null`), // Null payload
		[]byte(`{"type": 123, "payload": "test"}`), // Wrong type for type field
		[]byte(`{"payload": "missing type"}`),      // Missing required field
		make([]byte, 10*1024*1024),                 // Extremely large payload
	}

	for i, payload := range malformedPayloads {
		cmd := protocol.CommandRequest{
			Type:    "ping",
			Payload: payload,
		}

		err := validator.ValidateCommand(cmd)
		if err == nil {
			t.Errorf("Malformed payload %d should fail validation", i)
		}
	}

	// Test JSON parsing with nested structures
	nestedJSON := `{
		"command": "test",
		"data": {
			"nested": {
				"deeply": {
					"nested": "value"
				}
			}
		}
	}`

	cmd := protocol.CommandRequest{
		Type:    "ping",
		Payload: []byte(nestedJSON),
	}

	// This should pass basic validation (though command type validation may fail)
	sanitized := validator.SanitizePayload(cmd.Payload)
	if len(sanitized) == 0 {
		t.Error("Valid JSON payload should not be completely sanitized")
	}

	// Verify it's still valid JSON after sanitization
	var result map[string]interface{}
	if err := json.Unmarshal(sanitized, &result); err != nil {
		t.Errorf("Sanitized payload should still be valid JSON: %v", err)
	}
}
