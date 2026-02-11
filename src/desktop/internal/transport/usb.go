package transport

import (
	"fmt"
	"os/exec"
	"strings"
)

// USBTransport implements Transport interface for USB connections
type USBTransport struct {
	connected bool
}

func NewUSBTransport() *USBTransport {
	return &USBTransport{}
}

func (u *USBTransport) IsAvailable() bool {
	// Check if Android device is connected via USB
	cmd := exec.Command("adb", "devices")
	output, err := cmd.Output()
	if err != nil {
		return false
	}

	// Look for devices in output (excluding header)
	lines := strings.Split(string(output), "\n")
	for _, line := range lines {
		if strings.TrimSpace(line) != "" && !strings.Contains(line, "List of devices attached") {
			parts := strings.Fields(line)
			if len(parts) >= 2 && parts[1] == "device" {
				return true
			}
		}
	}
	return false
}

func (u *USBTransport) Connect() error {
	if !u.IsAvailable() {
		return fmt.Errorf("no USB device available")
	}

	// Enable USB debugging and connect
	cmd := exec.Command("adb", "tcpip", "5555")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("failed to enable USB debugging: %v", err)
	}

	u.connected = true
	fmt.Println("USB transport connected")
	return nil
}

func (u *USBTransport) Send(data []byte) error {
	if !u.connected {
		return fmt.Errorf("USB transport not connected")
	}

	// Use adb to send data (placeholder - would need custom protocol)
	cmd := exec.Command("adb", "shell", "echo", string(data))
	return cmd.Run()
}

func (u *USBTransport) Receive() ([]byte, error) {
	if !u.connected {
		return nil, fmt.Errorf("USB transport not connected")
	}

	// Placeholder receive implementation
	return []byte("usb_response"), nil
}

func (u *USBTransport) Close() error {
	u.connected = false
	fmt.Println("USB transport closed")
	return nil
}
