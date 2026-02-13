package noise

import (
	"crypto/rand"
	"fmt"
	"remotedexter/desktop/shared/protocol"
)

// TrustValidator interface for checking device trust
type TrustValidator interface {
	IsTrusted(publicKey []byte) bool
	UpdateLastSeen(deviceID string) error
}

// Handshake performs the Noise IK handshake with trust validation
type Handshake struct {
	localStaticPrivate []byte
	localStaticPublic  []byte
	trustValidator     TrustValidator
}

// NewHandshake creates a new handshake instance
func NewHandshake(staticPrivate, staticPublic []byte, validator TrustValidator) *Handshake {
	return &Handshake{
		localStaticPrivate: staticPrivate,
		localStaticPublic:  staticPublic,
		trustValidator:     validator,
	}
}

// InitiateHandshake starts the handshake process
func (h *Handshake) InitiateHandshake(remoteStaticPublic []byte) (*protocol.NoiseInit, error) {
	// Validate that the remote device is trusted
	if !h.trustValidator.IsTrusted(remoteStaticPublic) {
		return nil, fmt.Errorf("handshake rejected: untrusted device")
	}

	// Generate ephemeral keypair
	ephemeralPrivate, ephemeralPublic, err := GenerateKeyPair()
	if err != nil {
		return nil, fmt.Errorf("failed to generate ephemeral keypair: %v", err)
	}

	// Create initial payload (empty for now)
	payload := []byte{}

	initMsg := &protocol.NoiseInit{
		EphemeralPublicKey: ephemeralPublic,
		StaticPublicKey:    h.localStaticPublic,
		Payload:            payload,
	}

	return initMsg, nil
}

// ProcessHandshakeResponse processes the handshake response
func (h *Handshake) ProcessHandshakeResponse(response *protocol.NoiseResponse, remoteStaticPublic []byte) ([]byte, error) {
	// Validate that the remote device is still trusted
	if !h.trustValidator.IsTrusted(remoteStaticPublic) {
		return nil, fmt.Errorf("handshake rejected: device no longer trusted")
	}

	// In a full implementation, this would:
	// 1. Decrypt the response payload
	// 2. Verify the remote ephemeral public key
	// 3. Compute the shared secret
	// 4. Derive session keys

	// For now, generate a mock session key
	sessionKey := make([]byte, 32)
	if _, err := rand.Read(sessionKey); err != nil {
		return nil, fmt.Errorf("failed to generate session key: %v", err)
	}

	// Update last seen for the device
	// Note: In a real implementation, we'd need the device ID here
	// For now, we'll skip this as it requires more complex state management

	return sessionKey, nil
}

// AcceptHandshake processes an incoming handshake initiation
func (h *Handshake) AcceptHandshake(init *protocol.NoiseInit) (*protocol.NoiseResponse, []byte, error) {
	// Validate that the initiating device is trusted
	if !h.trustValidator.IsTrusted(init.StaticPublicKey) {
		return nil, nil, fmt.Errorf("handshake rejected: untrusted device")
	}

	// Generate ephemeral keypair for response
	ephemeralPrivate, ephemeralPublic, err := GenerateKeyPair()
	if err != nil {
		return nil, nil, fmt.Errorf("failed to generate ephemeral keypair: %v", err)
	}

	// Create response payload (empty for now)
	payload := []byte{}

	response := &protocol.NoiseResponse{
		EphemeralPublicKey: ephemeralPublic,
		Payload:            payload,
	}

	// Generate session key
	sessionKey := make([]byte, 32)
	if _, err := rand.Read(sessionKey); err != nil {
		return nil, nil, fmt.Errorf("failed to generate session key: %v", err)
	}

	return response, sessionKey, nil
}
