package transport

import (
	"fmt"
	"net"
	"time"
)

// WiFiDirectTransport implements Transport interface for Wi-Fi Direct connections
type WiFiDirectTransport struct {
	conn      net.Conn
	listener  net.Listener
	connected bool
}

func NewWiFiDirectTransport() *WiFiDirectTransport {
	return &WiFiDirectTransport{}
}

func (w *WiFiDirectTransport) IsAvailable() bool {
	// Check if Wi-Fi Direct is supported and available
	// This is a simplified check - in reality would check Wi-Fi Direct capabilities
	return true // Assume available for now
}

func (w *WiFiDirectTransport) Connect() error {
	if w.connected {
		return nil
	}

	// Listen on a port for Wi-Fi Direct connections
	listener, err := net.Listen("tcp", ":0") // Let OS assign port
	if err != nil {
		return fmt.Errorf("failed to start Wi-Fi Direct listener: %v", err)
	}
	w.listener = listener

	// In a real implementation, this would:
	// 1. Discover Wi-Fi Direct peers
	// 2. Establish P2P connection
	// 3. Connect to the peer

	// For now, simulate connection
	go func() {
		conn, err := listener.Accept()
		if err != nil {
			fmt.Printf("Wi-Fi Direct accept error: %v\n", err)
			return
		}
		w.conn = conn
		w.connected = true
		fmt.Println("Wi-Fi Direct connection established")
	}()

	// Wait a bit for connection (in real implementation, this would be event-driven)
	time.Sleep(100 * time.Millisecond)

	return nil
}

func (w *WiFiDirectTransport) Send(data []byte) error {
	if !w.connected || w.conn == nil {
		return fmt.Errorf("Wi-Fi Direct transport not connected")
	}

	_, err := w.conn.Write(data)
	return err
}

func (w *WiFiDirectTransport) Receive() ([]byte, error) {
	if !w.connected || w.conn == nil {
		return nil, fmt.Errorf("Wi-Fi Direct transport not connected")
	}

	buffer := make([]byte, 1024)
	n, err := w.conn.Read(buffer)
	if err != nil {
		return nil, err
	}

	return buffer[:n], nil
}

func (w *WiFiDirectTransport) Close() error {
	w.connected = false
	if w.conn != nil {
		w.conn.Close()
	}
	if w.listener != nil {
		w.listener.Close()
	}
	fmt.Println("Wi-Fi Direct transport closed")
	return nil
}
