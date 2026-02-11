package protocol

// KeyEvent represents a keyboard event
type KeyEvent struct {
	KeyCode int
	Action  int
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

// CommandRequest for sending commands
type CommandRequest struct {
	Type    string
	Payload []byte
}

// CommandResponse for command responses
type CommandResponse struct {
	Status  string
	Payload []byte
}

// Input event types
type TouchEvent struct {
	X      int
	Y      int
	Action int
}

type MouseMove struct {
	DX int
	DY int
}

type MouseClick struct {
	Button int
	Action int
}

type ScrollEvent struct {
	Amount int
}

// Action constants
const (
	ACTION_DOWN = 0
	ACTION_UP   = 1
	ACTION_MOVE = 2
)

// Button constants
const (
	BUTTON_LEFT   = 0
	BUTTON_RIGHT  = 1
	BUTTON_MIDDLE = 2
)
