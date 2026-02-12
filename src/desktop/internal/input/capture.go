package input

import (
	"fmt"
	"remotedexter/desktop/internal/transport"
	noisepkg "remotedexter/desktop/shared/noise"
	"remotedexter/desktop/shared/protocol"
)

// InputCapture handles capturing desktop input events
type InputCapture struct {
	selector    *transport.Selector
	sessionKey  []byte
	nonce       *uint64
	isCapturing bool
}

func NewInputCapture() *InputCapture {
	return &InputCapture{
		selector: transport.NewSelector(),
	}
}

func (ic *InputCapture) StartCapture(sessionKey []byte, nonce *uint64) {
	ic.sessionKey = make([]byte, len(sessionKey))
	copy(ic.sessionKey, sessionKey)
	ic.nonce = nonce
	ic.isCapturing = true
	fmt.Println("Input capture started")
}

func (ic *InputCapture) StopCapture() {
	ic.isCapturing = false
	fmt.Println("Input capture stopped")
}

// SendMouseMove sends mouse movement event
func (ic *InputCapture) SendMouseMove(dx, dy int) error {
	if !ic.isCapturing {
		return nil
	}

	event := protocol.MouseMove{DX: dx, DY: dy}
	encoded := protocol.EncodeMouseMove(event)

	// Encrypt
	nonceBytes := make([]byte, 12)
	for i := 0; i < 12; i++ {
		nonceBytes[i] = byte((*ic.nonce >> (8 * i)) & 0xFF)
	}
	encrypted, err := noisepkg.AEAD_Encrypt(ic.sessionKey, nonceBytes, encoded)
	if err != nil {
		return fmt.Errorf("encryption failed: %v", err)
	}
	*ic.nonce++

	// Send via transport
	req := protocol.CommandRequest{
		Type:    "mouse_move",
		Payload: encrypted,
	}

	resp, err := ic.selector.SendCommand(req, ic.sessionKey, ic.nonce)
	if err != nil {
		return fmt.Errorf("send failed: %v", err)
	}

	if resp.Status != "ok" {
		return fmt.Errorf("mouse move rejected: %s", resp.Status)
	}

	return nil
}

// SendMouseClick sends mouse click event
func (ic *InputCapture) SendMouseClick(button, action int) error {
	if !ic.isCapturing {
		return nil
	}

	event := protocol.MouseClick{Button: button, Action: action}
	encoded := protocol.EncodeMouseClick(event)

	// Encrypt
	nonceBytes := make([]byte, 12)
	for i := 0; i < 12; i++ {
		nonceBytes[i] = byte((*ic.nonce >> (8 * i)) & 0xFF)
	}
	encrypted, err := noisepkg.AEAD_Encrypt(ic.sessionKey, nonceBytes, encoded)
	if err != nil {
		return fmt.Errorf("encryption failed: %v", err)
	}
	*ic.nonce++

	// Send via transport
	req := protocol.CommandRequest{
		Type:    "mouse_click",
		Payload: encrypted,
	}

	resp, err := ic.selector.SendCommand(req, ic.sessionKey, ic.nonce)
	if err != nil {
		return fmt.Errorf("send failed: %v", err)
	}

	if resp.Status != "ok" {
		return fmt.Errorf("mouse click rejected: %s", resp.Status)
	}

	return nil
}

// SendKeyEvent sends keyboard event
func (ic *InputCapture) SendKeyEvent(keyCode, action int) error {
	if !ic.isCapturing {
		return nil
	}

	event := protocol.KeyEvent{KeyCode: keyCode, Action: action}
	encoded := protocol.EncodeKeyEvent(event)

	// Encrypt
	nonceBytes := make([]byte, 12)
	for i := 0; i < 12; i++ {
		nonceBytes[i] = byte((*ic.nonce >> (8 * i)) & 0xFF)
	}
	encrypted, err := noisepkg.AEAD_Encrypt(ic.sessionKey, nonceBytes, encoded)
	if err != nil {
		return fmt.Errorf("encryption failed: %v", err)
	}
	*ic.nonce++

	// Send via transport
	req := protocol.CommandRequest{
		Type:    "key_event",
		Payload: encrypted,
	}

	resp, err := ic.selector.SendCommand(req, ic.sessionKey, ic.nonce)
	if err != nil {
		return fmt.Errorf("send failed: %v", err)
	}

	if resp.Status != "ok" {
		return fmt.Errorf("key event rejected: %s", resp.Status)
	}

	return nil
}

// SendScrollEvent sends scroll wheel event
func (ic *InputCapture) SendScrollEvent(amount int) error {
	if !ic.isCapturing {
		return nil
	}

	event := protocol.ScrollEvent{Amount: amount}
	encoded := protocol.EncodeScrollEvent(event)

	// Encrypt
	nonceBytes := make([]byte, 12)
	for i := 0; i < 12; i++ {
		nonceBytes[i] = byte((*ic.nonce >> (8 * i)) & 0xFF)
	}
	encrypted, err := noisepkg.AEAD_Encrypt(ic.sessionKey, nonceBytes, encoded)
	if err != nil {
		return fmt.Errorf("encryption failed: %v", err)
	}
	*ic.nonce++

	// Send via transport
	req := protocol.CommandRequest{
		Type:    "scroll_event",
		Payload: encrypted,
	}

	resp, err := ic.selector.SendCommand(req, ic.sessionKey, ic.nonce)
	if err != nil {
		return fmt.Errorf("send failed: %v", err)
	}

	if resp.Status != "ok" {
		return fmt.Errorf("scroll event rejected: %s", resp.Status)
	}

	return nil
}

// Close cleans up input capture resources
func (ic *InputCapture) Close() error {
	if ic.isCapturing {
		ic.StopCapture()
	}

	// Zeroize session keys
	if ic.sessionKey != nil {
		for i := range ic.sessionKey {
			ic.sessionKey[i] = 0
		}
		ic.sessionKey = nil
	}

	fmt.Println("Input capture closed")
	return nil
}
