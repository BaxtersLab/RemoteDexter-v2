package protocol

// KeyEvent represents a keyboard event
type KeyEvent struct {
	KeyCode int
	Pressed bool
}

// MouseEvent represents a mouse event
type MouseEvent struct {
	X, Y    int
	Button  int
	Pressed bool
}

// PairingRequest for initial pairing
type PairingRequest struct {
	DeviceID  string
	PublicKey []byte
}

// PairingResponse
type PairingResponse struct {
	Accepted   bool
	SessionKey []byte
}

// NoiseInit for handshake initiation
type NoiseInit struct {
	EphemeralPublicKey []byte
	StaticPublicKey    []byte
	Payload            []byte
}

// NoiseResponse for handshake response
type NoiseResponse struct {
	EphemeralPublicKey []byte
	Payload            []byte
}
