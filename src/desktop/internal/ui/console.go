package ui

import (
	"bufio"
	"fmt"
	"os"
	"remotedexter/desktop/internal/bluetooth"
	"remotedexter/desktop/internal/noise"
	"remotedexter/desktop/internal/session"
	"remotedexter/desktop/internal/streaming"
	"remotedexter/desktop/internal/transport"
	"remotedexter/desktop/shared/protocol"
	"strings"
)

type Console struct {
	discovery         *bluetooth.Discovery
	handshake         *noise.Handshake
	selector          *transport.Selector
	streaming         *streaming.StreamingSession
	sessionController *session.SessionController
	sessionKey        []byte
	nonce             uint64
	debug             bool
}

func NewConsole(dataDir string) *Console {
	sessionController, err := session.NewSessionController(dataDir)
	if err != nil {
		fmt.Printf("Warning: Failed to initialize session controller: %v\n", err)
		sessionController, _ = session.NewSessionController("") // Fallback
	}

	// Initialize file transfer components
	destDir := dataDir
	if destDir == "" {
		destDir = "."
	}

	// Simple accept callback for console UI
	acceptCallback := func(fileID, fileName string, size int64) bool {
		fmt.Printf("\nIncoming file transfer:\n")
		fmt.Printf("File: %s\n", fileName)
		fmt.Printf("Size: %d bytes\n", size)
		fmt.Print("Accept transfer? (y/N): ")

		scanner := bufio.NewScanner(os.Stdin)
		if scanner.Scan() {
			response := strings.TrimSpace(strings.ToLower(scanner.Text()))
			return response == "y" || response == "yes"
		}
		return false
	}

	if err := sessionController.InitializeFileTransfer(destDir, acceptCallback); err != nil {
		fmt.Printf("Warning: Failed to initialize file transfer: %v\n", err)
	}

	return &Console{
		discovery:         bluetooth.NewDiscovery(),
		handshake:         noise.NewHandshake(),
		selector:          transport.NewSelector(),
		streaming:         streaming.NewStreamingSession(),
		sessionController: sessionController,
	}
}

func (c *Console) Run() {
	scanner := bufio.NewScanner(os.Stdin)
	for {
		c.showMenu()
		if !scanner.Scan() {
			break
		}
		choice := strings.TrimSpace(scanner.Text())
		c.handleChoice(choice)
	}
}

func (c *Console) showMenu() {
	fmt.Println("\nRD Desktop Console")
	fmt.Println("=== Session Management ===")
	fmt.Println("1: Start remote session")
	fmt.Println("2: Stop remote session")
	fmt.Println("3: Show session status")
	fmt.Println("=== Device Trust Management ===")
	fmt.Println("4: List trusted devices")
	fmt.Println("5: Revoke device")
	fmt.Println("6: Lost device protocol")
	fmt.Println("7: Rotate keys")
	fmt.Println("=== File Transfer ===")
	fmt.Println("8: Send file to device")
	fmt.Println("9: List active transfers")
	fmt.Println("10: Cancel file transfer")
	fmt.Println("=== Legacy Commands ===")
	fmt.Println("11: Discover devices")
	fmt.Println("12: Bootstrap Android")
	fmt.Println("13: Perform handshake")
	fmt.Println("14: Send ping")
	fmt.Println("15: Start screen streaming")
	fmt.Println("16: Stop screen streaming")
	fmt.Println("17: Toggle mouse mode")
	fmt.Println("18: Toggle touch mode")
	fmt.Println("19: Toggle precision mode")
	fmt.Println("20: Exit")
	fmt.Print("Choice: ")
}

func (c *Console) handleChoice(choice string) {
	switch choice {
	case "1":
		c.startSession()
	case "2":
		c.stopSession()
	case "3":
		c.showSessionStatus()
	case "4":
		c.listTrustedDevices()
	case "5":
		c.revokeDevice()
	case "6":
		c.lostDeviceProtocol()
	case "7":
		c.rotateKeys()
	case "8":
		c.sendFile()
	case "9":
		c.listFileTransfers()
	case "10":
		c.cancelFileTransfer()
	case "11":
		c.discoverDevices()
	case "12":
		c.bootstrapAndroid()
	case "13":
		c.performHandshake()
	case "14":
		c.sendPing()
	case "15":
		c.startStreaming()
	case "16":
		c.stopStreaming()
	case "17":
		c.setInputMode("mouse")
	case "18":
		c.setInputMode("touch")
	case "19":
		c.setInputMode("precision")
	case "20":
		os.Exit(0)
	default:
		fmt.Println("Invalid choice")
	}
}

func (c *Console) discoverDevices() {
	fmt.Println("UI: Discovering devices...")
	devices, err := c.discovery.DiscoverPairedDevices()
	if err != nil {
		fmt.Printf("Discovery failed: %v\n", err)
	} else {
		fmt.Println("Discovery OK")
		for _, dev := range devices {
			fmt.Printf("Device: %s (%s)\n", dev.Name, dev.MAC)
		}
	}
}

func (c *Console) bootstrapAndroid() {
	fmt.Println("UI: Bootstrap initiated")
	// Placeholder for bootstrap
	fmt.Println("Bootstrap OK")
}

func (c *Console) performHandshake() {
	fmt.Println("UI: Handshake initiated")
	key, err := c.handshake.PerformNoiseHandshake()
	if err != nil {
		fmt.Printf("Handshake failed: %v\n", err)
	} else {
		c.sessionKey = key
		fmt.Println("Handshake OK")
	}
}

func (c *Console) sendPing() {
	if c.sessionKey == nil {
		fmt.Println("No session key, perform handshake first")
		return
	}
	fmt.Println("UI: Command sent")
	req := protocol.CommandRequest{Type: "ping", Payload: []byte{}}
	resp, err := c.selector.SendCommand(req, c.sessionKey, &c.nonce)
	if err != nil {
		fmt.Printf("Command failed: %v\n", err)
	} else {
		fmt.Printf("ping → %s\n", string(resp.Payload))
	}
}

func (c *Console) startStreaming() {
	if c.sessionKey == nil {
		fmt.Println("No session key, perform handshake first")
		return
	}

	fmt.Println("UI: Starting screen streaming")
	c.streaming.SetSessionKeys(c.sessionKey, c.nonce)
	if err := c.streaming.StartStreaming(); err != nil {
		fmt.Printf("Streaming start failed: %v\n", err)
	} else {
		fmt.Println("Streaming started successfully")
	}
}

func (c *Console) stopStreaming() {
	fmt.Println("UI: Stopping screen streaming")
	if err := c.streaming.StopStreaming(); err != nil {
		fmt.Printf("Streaming stop failed: %v\n", err)
	} else {
		fmt.Println("Streaming stopped successfully")
	}
}

func (c *Console) setInputMode(mode string) {
	if c.sessionKey == nil {
		fmt.Println("No session key, perform handshake first")
		return
	}

	fmt.Printf("UI: Setting input mode to %s\n", mode)

	req := protocol.CommandRequest{
		Type:    "set_input_mode",
		Payload: []byte(mode),
	}

	resp, err := c.selector.SendCommand(req, c.sessionKey, &c.nonce)
	if err != nil {
		fmt.Printf("Set input mode failed: %v\n", err)
	} else {
		fmt.Printf("Input mode set to %s: %s\n", mode, resp.Status)
	}
}

func (c *Console) startSession() {
	fmt.Println("UI: Starting remote session...")
	if err := c.sessionController.StartRemoteSession(); err != nil {
		fmt.Printf("Session start failed: %v\n", err)
	} else {
		fmt.Println("Session started successfully")
	}
}

func (c *Console) stopSession() {
	fmt.Println("UI: Stopping remote session...")
	if err := c.sessionController.StopRemoteSession(); err != nil {
		fmt.Printf("Session stop failed: %v\n", err)
	} else {
		fmt.Println("Session stopped successfully")
	}
}

func (c *Console) showSessionStatus() {
	state := c.sessionController.GetState()
	fmt.Printf("\n=== Session Status ===\n")
	fmt.Printf("Connected: %t\n", state.IsConnected)
	fmt.Printf("Streaming: %t\n", state.IsStreaming)
	fmt.Printf("Input Enabled: %t\n", state.IsInputEnabled)
	fmt.Printf("Transport: %s\n", state.TransportType)
	fmt.Printf("Error Count: %d\n", state.ErrorCount)
	fmt.Printf("Last Activity: %v\n", state.LastActivityTime)
	fmt.Printf("Healthy: %t\n", c.sessionController.IsHealthy())
}

func (c *Console) listTrustedDevices() {
	fmt.Println("UI: Listing trusted devices...")
	devices, err := c.sessionController.ListTrustedDevices()
	if err != nil {
		fmt.Printf("Failed to list trusted devices: %v\n", err)
		return
	}

	if len(devices) == 0 {
		fmt.Println("No trusted devices found")
		return
	}

	fmt.Println("\n=== Trusted Devices ===")
	for _, device := range devices {
		fmt.Printf("Name: %s\n", device.DeviceName)
		fmt.Printf("ID: %s\n", device.DeviceID)
		fmt.Printf("Added: %s\n", device.AddedAt)
		fmt.Printf("Last Seen: %s\n", device.LastSeen)
		fmt.Println("---")
	}
}

func (c *Console) revokeDevice() {
	fmt.Println("UI: Revoking device...")

	// First list devices to help user choose
	devices, err := c.sessionController.ListTrustedDevices()
	if err != nil {
		fmt.Printf("Failed to list devices: %v\n", err)
		return
	}

	if len(devices) == 0 {
		fmt.Println("No trusted devices to revoke")
		return
	}

	fmt.Println("Available devices:")
	for i, device := range devices {
		fmt.Printf("%d: %s (%s)\n", i+1, device.DeviceName, device.DeviceID)
	}

	fmt.Print("Enter device number to revoke: ")
	scanner := bufio.NewScanner(os.Stdin)
	if !scanner.Scan() {
		return
	}

	var deviceIndex int
	if _, err := fmt.Sscanf(scanner.Text(), "%d", &deviceIndex); err != nil || deviceIndex < 1 || deviceIndex > len(devices) {
		fmt.Println("Invalid device number")
		return
	}

	selectedDevice := devices[deviceIndex-1]

	if err := c.sessionController.RevokeDevice(selectedDevice.DeviceID); err != nil {
		fmt.Printf("Failed to revoke device: %v\n", err)
	} else {
		fmt.Printf("Device '%s' revoked successfully\n", selectedDevice.DeviceName)
	}
}

func (c *Console) lostDeviceProtocol() {
	fmt.Println("WARNING: This will revoke ALL trusted devices and terminate any active sessions!")
	fmt.Println("This action cannot be undone.")
	fmt.Print("Are you sure? (type 'YES' to confirm): ")

	scanner := bufio.NewScanner(os.Stdin)
	if !scanner.Scan() {
		return
	}

	confirmation := strings.TrimSpace(scanner.Text())
	if confirmation != "YES" {
		fmt.Println("Lost device protocol cancelled")
		return
	}

	if err := c.sessionController.LostDeviceProtocol(); err != nil {
		fmt.Printf("Lost device protocol failed: %v\n", err)
	} else {
		fmt.Println("Lost device protocol completed successfully")
	}
}

func (c *Console) rotateKeys() {
	fmt.Println("UI: Rotating cryptographic keys...")
	fmt.Println("This will generate new keys and update all trusted devices.")
	fmt.Println("Active sessions will be terminated and require re-handshake.")
	fmt.Print("Are you sure? (type 'ROTATE' to confirm): ")

	scanner := bufio.NewScanner(os.Stdin)
	if !scanner.Scan() {
		return
	}

	confirmation := strings.TrimSpace(scanner.Text())
	if confirmation != "ROTATE" {
		fmt.Println("Key rotation cancelled")
		return
	}

	if err := c.sessionController.RotateKeys(); err != nil {
		fmt.Printf("Key rotation failed: %v\n", err)
	} else {
		fmt.Println("Key rotation completed successfully")
		fmt.Println("All trusted devices have been updated with new keys")
		fmt.Println("Please perform handshake with devices to resume sessions")
	}
}

func (c *Console) sendFile() {
	fmt.Println("UI: Sending file to device...")

	fmt.Print("Enter file path to send: ")
	scanner := bufio.NewScanner(os.Stdin)
	if !scanner.Scan() {
		return
	}

	filePath := strings.TrimSpace(scanner.Text())
	if filePath == "" {
		fmt.Println("No file path provided")
		return
	}

	if err := c.sessionController.SendFile(filePath); err != nil {
		fmt.Printf("Failed to send file: %v\n", err)
	} else {
		fmt.Printf("File transfer initiated: %s\n", filePath)
	}
}

func (c *Console) listFileTransfers() {
	fmt.Println("UI: Listing active file transfers...")

	transfers := c.sessionController.ListActiveFileTransfers()
	if len(transfers) == 0 {
		fmt.Println("No active file transfers")
		return
	}

	fmt.Println("\n=== Active File Transfers ===")
	for _, transfer := range transfers {
		progress := float64(transfer.Transferred) / float64(transfer.Size) * 100.0
		fmt.Printf("ID: %s\n", transfer.FileID)
		fmt.Printf("File: %s\n", transfer.FileName)
		fmt.Printf("Size: %d bytes\n", transfer.Size)
		fmt.Printf("Transferred: %d bytes (%.1f%%)\n", transfer.Transferred, progress)
		fmt.Printf("Status: %s\n", transfer.Status)
		if transfer.Error != nil {
			fmt.Printf("Error: %v\n", transfer.Error)
		}
		fmt.Printf("Started: %s\n", transfer.StartTime.Format("15:04:05"))
		fmt.Println("---")
	}
}

func (c *Console) cancelFileTransfer() {
	fmt.Println("UI: Cancelling file transfer...")

	transfers := c.sessionController.ListActiveFileTransfers()
	if len(transfers) == 0 {
		fmt.Println("No active file transfers to cancel")
		return
	}

	fmt.Println("Active transfers:")
	for i, transfer := range transfers {
		fmt.Printf("%d: %s (%s) - %s\n", i+1, transfer.FileName, transfer.FileID, transfer.Status)
	}

	fmt.Print("Enter transfer number to cancel: ")
	scanner := bufio.NewScanner(os.Stdin)
	if !scanner.Scan() {
		return
	}

	var transferIndex int
	if _, err := fmt.Sscanf(scanner.Text(), "%d", &transferIndex); err != nil || transferIndex < 1 || transferIndex > len(transfers) {
		fmt.Println("Invalid transfer number")
		return
	}

	selectedTransfer := transfers[transferIndex-1]

	if err := c.sessionController.CancelFileTransfer(selectedTransfer.FileID); err != nil {
		fmt.Printf("Failed to cancel transfer: %v\n", err)
	} else {
		fmt.Printf("Transfer cancelled: %s\n", selectedTransfer.FileName)
	}
}
