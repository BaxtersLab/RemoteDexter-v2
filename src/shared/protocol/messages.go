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

// CommandRequest for remote commands
type CommandRequest struct {
	Type    string
	Payload []byte
}

// CommandResponse for command replies
type CommandResponse struct {
	Status  string
	Payload []byte
}

// TrustedDevice represents a trusted device entry
type TrustedDevice struct {
	DeviceName string `json:"device_name"`
	DeviceID   string `json:"device_id"`
	PublicKey  []byte `json:"public_key"`
	AddedAt    string `json:"added_at"`
	LastSeen   string `json:"last_seen"`
}

// RevokeDevice command payload
type RevokeDevice struct {
	DeviceID string `json:"device_id"`
}

// TerminateSession command payload
type TerminateSession struct {
	Reason string `json:"reason"`
}

// FileOffer represents a file transfer offer
type FileOffer struct {
	FileID   string `json:"file_id"`
	FileName string `json:"file_name"`
	Size     int64  `json:"size"`
	MimeType string `json:"mime_type"`
}

// FileAccept represents acceptance of a file transfer
type FileAccept struct {
	FileID string `json:"file_id"`
}

// FileReject represents rejection of a file transfer
type FileReject struct {
	FileID string `json:"file_id"`
	Reason string `json:"reason"`
}

// FileChunk represents a chunk of file data
type FileChunk struct {
	FileID string `json:"file_id"`
	Offset int64  `json:"offset"`
	Data   []byte `json:"data"`
	EOF    bool   `json:"eof"`
}
