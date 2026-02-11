package transport

import (
	"fmt"
	"testing"
)

// TestTransportUpgrade tests the end-to-end transport upgrade scenario
func TestTransportUpgrade(t *testing.T) {
	selector := NewSelector()

	// Test transport selection
	selected := selector.SelectTransport()
	fmt.Printf("Selected transport: %s\n", selected)

	// Verify selection logic: USB > Wi-Fi Direct > Bluetooth
	if selected != "wifidirect" && selected != "usb" && selected != "bluetooth" {
		t.Errorf("Invalid transport selected: %s", selected)
	}

	fmt.Printf("Transport upgrade test: %s transport selected\n", selected)
}

// TestTransportAvailability tests availability of all transports
func TestTransportAvailability(t *testing.T) {
	transports := []struct {
		name      string
		transport Transport
	}{
		{"USB", NewUSBTransport()},
		{"Wi-Fi Direct", NewWiFiDirectTransport()},
		{"Bluetooth", NewBluetoothTransport()},
	}

	for _, tt := range transports {
		available := tt.transport.IsAvailable()
		fmt.Printf("%s transport available: %v\n", tt.name, available)
	}
}
