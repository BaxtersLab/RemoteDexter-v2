package transport

import (
	"fmt"
	noisepkg "remotedexter/desktop/shared/noise"
	"remotedexter/desktop/shared/protocol"
)

// Transport interface for unified transport abstraction
type Transport interface {
	Connect() error
	Send(data []byte) error
	Receive() ([]byte, error)
	Close() error
	IsAvailable() bool
}

type Selector struct{}

func NewSelector() *Selector {
	return &Selector{}
}

func (s *Selector) SelectTransport() string {
	// Check USB first (highest priority)
	if usb := NewUSBTransport(); usb.IsAvailable() {
		fmt.Println("Transport selected: usb")
		return "usb"
	}
	// Then Wi-Fi Direct
	if wifi := NewWiFiDirectTransport(); wifi.IsAvailable() {
		fmt.Println("Transport selected: wifidirect")
		return "wifidirect"
	}
	// Fall back to Bluetooth
	fmt.Println("Transport selected: bluetooth")
	return "bluetooth"
}

func (s *Selector) SendCommand(req protocol.CommandRequest, sessionKey []byte, nonce *uint64) (protocol.CommandResponse, error) {
	// Get selected transport
	transportType := s.SelectTransport()
	var transport Transport

	switch transportType {
	case "usb":
		transport = NewUSBTransport()
	case "wifidirect":
		transport = NewWiFiDirectTransport()
	default:
		transport = NewBluetoothTransport()
	}

	// Connect to transport
	if err := transport.Connect(); err != nil {
		return protocol.CommandResponse{}, fmt.Errorf("transport connection failed: %v", err)
	}
	defer transport.Close()

	// Encode request
	encoded := protocol.EncodeCommandRequest(req)
	// Encrypt
	nonceBytes := make([]byte, 12)
	for i := 0; i < 12; i++ {
		nonceBytes[i] = byte((*nonce >> (8 * i)) & 0xFF)
	}
	encrypted, err := noisepkg.AEAD_Encrypt(sessionKey, nonceBytes, encoded)
	if err != nil {
		return protocol.CommandResponse{}, fmt.Errorf("encryption failed: %v", err)
	}
	*nonce++

	fmt.Printf("Command sent via %s: %s\n", transportType, req.Type)

	// Send encrypted data
	if err := transport.Send(encrypted); err != nil {
		return protocol.CommandResponse{}, fmt.Errorf("send failed: %v", err)
	}

	// Receive response
	encryptedResp, err := transport.Receive()
	if err != nil {
		return protocol.CommandResponse{}, fmt.Errorf("receive failed: %v", err)
	}

	// Decrypt response
	respDecrypted, err := noisepkg.AEAD_Decrypt(sessionKey, nonceBytes, encryptedResp)
	if err != nil {
		return protocol.CommandResponse{}, fmt.Errorf("decryption failed: %v", err)
	}
	*nonce++

	receivedResp, err := protocol.DecodeCommandResponse(respDecrypted[4:]) // skip frame
	if err != nil {
		return protocol.CommandResponse{}, fmt.Errorf("decode failed: %v", err)
	}

	fmt.Printf("Response received via %s: %s\n", transportType, receivedResp.Status)
	return receivedResp, nil
}

// Close closes all active transport connections
func (s *Selector) Close() error {
	// In a real implementation, this would close any persistent connections
	// For now, just return success
	fmt.Println("Transport selector closed")
	return nil
}
