package noise

import (
	"fmt"
	"sync"
)

// SessionState represents the cryptographic state of a Noise session
type SessionState struct {
	sendNonce     uint64
	recvNonce     uint64
	sessionKey    []byte
	isEstablished bool
	mu            sync.RWMutex
}

// NewSessionState creates a new session state
func NewSessionState() *SessionState {
	return &SessionState{
		sendNonce:     0,
		recvNonce:     0,
		sessionKey:    nil,
		isEstablished: false,
	}
}

// Establish sets the session key and marks it as established
func (s *SessionState) Establish(sessionKey []byte) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.sessionKey = make([]byte, len(sessionKey))
	copy(s.sessionKey, sessionKey)
	s.isEstablished = true
	s.sendNonce = 0
	s.recvNonce = 0
}

// IsEstablished returns whether the session is established
func (s *SessionState) IsEstablished() bool {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.isEstablished
}

// GetSessionKey returns the current session key
func (s *SessionState) GetSessionKey() []byte {
	s.mu.RLock()
	defer s.mu.RUnlock()
	if s.sessionKey == nil {
		return nil
	}
	key := make([]byte, len(s.sessionKey))
	copy(key, s.sessionKey)
	return key
}

// NextSendNonce returns the next nonce for sending and increments it
func (s *SessionState) NextSendNonce() uint64 {
	s.mu.Lock()
	defer s.mu.Unlock()
	nonce := s.sendNonce
	s.sendNonce++
	return nonce
}

// ValidateRecvNonce validates an incoming nonce for replay protection
func (s *SessionState) ValidateRecvNonce(nonce uint64) error {
	s.mu.Lock()
	defer s.mu.Unlock()

	// Check for nonce reuse
	if nonce < s.recvNonce {
		return fmt.Errorf("replay detected: nonce reuse (received %d, expected >= %d)", nonce, s.recvNonce)
	}

	// Allow small tolerance for out-of-order messages (up to 10 messages)
	const tolerance = 10
	if nonce > s.recvNonce+tolerance {
		return fmt.Errorf("replay detected: nonce too far ahead (received %d, expected <= %d)", nonce, s.recvNonce+tolerance)
	}

	// Update expected nonce if this is the next expected one
	if nonce == s.recvNonce {
		s.recvNonce++
	}

	return nil
}

// Reset clears the session state
func (s *SessionState) Reset() {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.sendNonce = 0
	s.recvNonce = 0
	s.sessionKey = nil
	s.isEstablished = false
}

// EncryptMessage encrypts a message with replay protection
func (s *SessionState) EncryptMessage(plaintext []byte) ([]byte, error) {
	s.mu.RLock()
	if !s.isEstablished {
		s.mu.RUnlock()
		return nil, fmt.Errorf("session not established")
	}
	key := s.sessionKey
	s.mu.RUnlock()

	nonce := s.NextSendNonce()
	nonceBytes := make([]byte, 12)
	// Convert uint64 nonce to 12-byte little-endian (Noise spec)
	for i := 0; i < 8 && i < 12; i++ {
		nonceBytes[i] = byte(nonce >> (i * 8))
	}

	return AEAD_Encrypt(key, nonceBytes, plaintext)
}

// DecryptMessage decrypts a message with replay protection
func (s *SessionState) DecryptMessage(ciphertext []byte) ([]byte, error) {
	s.mu.RLock()
	if !s.isEstablished {
		s.mu.RUnlock()
		return nil, fmt.Errorf("session not established")
	}
	key := s.sessionKey
	s.mu.RUnlock()

	if len(ciphertext) < 12 {
		return nil, fmt.Errorf("ciphertext too short for nonce")
	}

	// Extract nonce from first 12 bytes
	nonceBytes := ciphertext[:12]
	var nonce uint64
	for i := 0; i < 8 && i < len(nonceBytes); i++ {
		nonce |= uint64(nonceBytes[i]) << (i * 8)
	}

	// Validate nonce for replay protection
	if err := s.ValidateRecvNonce(nonce); err != nil {
		return nil, err
	}

	// Decrypt the actual payload
	payload := ciphertext[12:]
	return AEAD_Decrypt(key, nonceBytes, payload)

}
