package transport

import (
	"fmt"
	"remotedexter/desktop/internal/bluetooth"
)

// BluetoothTransport implements Transport interface for Bluetooth connections
type BluetoothTransport struct {
	discovery *bluetooth.Discovery
	connected bool
}

func NewBluetoothTransport() *BluetoothTransport {
	return &BluetoothTransport{
		discovery: bluetooth.NewDiscovery(),
	}
}

func (b *BluetoothTransport) IsAvailable() bool {
	devices, err := b.discovery.DiscoverPairedDevices()
	return err == nil && len(devices) > 0
}

func (b *BluetoothTransport) Connect() error {
	if !b.IsAvailable() {
		return fmt.Errorf("no Bluetooth devices available")
	}

	// Initialize Bluetooth bootstrap
	bluetooth.Bootstrap()
	b.connected = true
	fmt.Println("Bluetooth transport connected")
	return nil
}

func (b *BluetoothTransport) Send(data []byte) error {
	if !b.connected {
		return fmt.Errorf("Bluetooth transport not connected")
	}

	// Placeholder: simulate Bluetooth send
	fmt.Printf("Bluetooth sending %d bytes\n", len(data))
	return nil
}

func (b *BluetoothTransport) Receive() ([]byte, error) {
	if !b.connected {
		return nil, fmt.Errorf("Bluetooth transport not connected")
	}

	// Placeholder: simulate Bluetooth receive
	return []byte("bluetooth_response"), nil
}

func (b *BluetoothTransport) Close() error {
	b.connected = false
	fmt.Println("Bluetooth transport closed")
	return nil
}
