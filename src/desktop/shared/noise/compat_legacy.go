package noise

import "fmt"

// KeyPair is a compatibility struct used by older session tests/callers.
type KeyPair struct {
	Public  []byte
	Private []byte
}

// GenerateKeypair is a backward-compatible wrapper around GenerateKeyPair.
func GenerateKeypair() (KeyPair, error) {
	public, private, err := GenerateKeyPair()
	if err != nil {
		return KeyPair{}, err
	}
	return KeyPair{Public: public, Private: private}, nil
}

// Session is a compatibility Noise session used by legacy tests.
type Session struct {
	local       KeyPair
	remote      []byte
	initiator   bool
	sessionKey  []byte
	established bool
	sendNonce   uint64
	recvNonce   uint64
}

// NewSession creates a compatibility session.
func NewSession(local KeyPair, remotePublic []byte, initiator bool) (*Session, error) {
	return &Session{
		local:     local,
		remote:    remotePublic,
		initiator: initiator,
	}, nil
}

func (s *Session) ensureEstablished() error {
	if s.established {
		return nil
	}
	shared, err := SharedSecret(s.local.Private, s.remote)
	if err != nil {
		return err
	}
	key, err := HKDF(shared, nil, []byte("remotedexter-noise-session"), 32)
	if err != nil {
		return err
	}
	s.sessionKey = key
	s.established = true
	return nil
}

// InitiateHandshake returns a simple compatibility handshake payload.
func (s *Session) InitiateHandshake() ([]byte, error) {
	return append([]byte(nil), s.local.Public...), nil
}

// ReceiveHandshake consumes an incoming handshake payload.
func (s *Session) ReceiveHandshake(_ []byte) error {
	return s.ensureEstablished()
}

// RespondHandshake returns a simple compatibility handshake response payload.
func (s *Session) RespondHandshake() ([]byte, error) {
	if err := s.ensureEstablished(); err != nil {
		return nil, err
	}
	return append([]byte(nil), s.local.Public...), nil
}

// CompleteHandshake finalizes the compatibility handshake.
func (s *Session) CompleteHandshake(_ []byte) error {
	return s.ensureEstablished()
}

// Encrypt encrypts plaintext and prefixes nonce bytes.
func (s *Session) Encrypt(plaintext []byte) ([]byte, error) {
	if err := s.ensureEstablished(); err != nil {
		return nil, err
	}

	nonce := s.sendNonce
	s.sendNonce++

	nonceBytes := make([]byte, 12)
	for i := 0; i < 8; i++ {
		nonceBytes[i] = byte(nonce >> (8 * i))
	}

	ciphertext, err := AEAD_Encrypt(s.sessionKey, nonceBytes, plaintext)
	if err != nil {
		return nil, err
	}

	result := make([]byte, 0, 12+len(ciphertext))
	result = append(result, nonceBytes...)
	result = append(result, ciphertext...)
	return result, nil
}

// Decrypt decrypts payload and enforces monotonic nonce replay protection.
func (s *Session) Decrypt(ciphertext []byte) ([]byte, error) {
	if err := s.ensureEstablished(); err != nil {
		return nil, err
	}
	if len(ciphertext) < 12 {
		return nil, fmt.Errorf("ciphertext too short")
	}

	nonceBytes := ciphertext[:12]
	var nonce uint64
	for i := 0; i < 8; i++ {
		nonce |= uint64(nonceBytes[i]) << (8 * i)
	}

	if nonce < s.recvNonce {
		return nil, fmt.Errorf("replay detected")
	}
	if nonce > s.recvNonce {
		return nil, fmt.Errorf("out-of-order message")
	}
	s.recvNonce++

	return AEAD_Decrypt(s.sessionKey, nonceBytes, ciphertext[12:])
}
