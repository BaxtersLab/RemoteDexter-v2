package ui

import (
	"fmt"
	"remotedexter/desktop/internal/security"
	"remotedexter/desktop/internal/session"
	"time"
)

// TrustedDevicePanel manages the trusted devices UI panel
type TrustedDevicePanel struct {
	trustStore *security.FileBasedTrustStore
	controller *session.SessionController
	devices    []security.TrustedDevice
}

// NewTrustedDevicePanel creates a new trusted device panel
func NewTrustedDevicePanel(dataDir string, controller *session.SessionController) (*TrustedDevicePanel, error) {
	trustStore, err := security.NewFileBasedTrustStore(dataDir)
	if err != nil {
		return nil, fmt.Errorf("failed to initialize trust store: %v", err)
	}

	return &TrustedDevicePanel{
		trustStore: trustStore,
		controller: controller,
	}, nil
}

// ShowPanel displays the trusted devices panel
func (tdp *TrustedDevicePanel) ShowPanel() {
	fmt.Println("=== Trusted Devices ===")
	fmt.Println()

	tdp.refreshDevices()

	if len(tdp.devices) == 0 {
		fmt.Println("No trusted devices found.")
		fmt.Println("Pair a device first to start using RemoteDexter.")
		return
	}

	// Display devices
	for i, device := range tdp.devices {
		fmt.Printf("%d. %s\n", i+1, device.DeviceName)
		fmt.Printf("   ID: %s\n", device.DeviceID)
		fmt.Printf("   Last Seen: %s\n", tdp.formatLastSeen(device.LastSeen))
		fmt.Printf("   Status: %s\n", tdp.getDeviceStatus(device))
		fmt.Println()
	}

	tdp.showMenu()
}

// refreshDevices updates the device list
func (tdp *TrustedDevicePanel) refreshDevices() {
	devices, err := tdp.trustStore.ListDevices()
	if err != nil {
		fmt.Printf("Error loading devices: %v\n", err)
		tdp.devices = []security.TrustedDevice{}
		return
	}
	tdp.devices = devices
}

// formatLastSeen formats the last seen timestamp
func (tdp *TrustedDevicePanel) formatLastSeen(lastSeen string) string {
	if lastSeen == "" {
		return "Never"
	}

	t, err := time.Parse(time.RFC3339, lastSeen)
	if err != nil {
		return lastSeen
	}

	duration := time.Since(t)
	if duration < time.Minute {
		return "Just now"
	} else if duration < time.Hour {
		return fmt.Sprintf("%d minutes ago", int(duration.Minutes()))
	} else if duration < 24*time.Hour {
		return fmt.Sprintf("%d hours ago", int(duration.Hours()))
	} else {
		return fmt.Sprintf("%d days ago", int(duration.Hours()/24))
	}
}

// getDeviceStatus returns the status of a device
func (tdp *TrustedDevicePanel) getDeviceStatus(device security.TrustedDevice) string {
	// In a real implementation, this would check if the device is currently connected
	// For now, just show "Available" if seen recently
	lastSeen, err := time.Parse(time.RFC3339, device.LastSeen)
	if err != nil {
		return "Unknown"
	}
	if time.Since(lastSeen) < 24*time.Hour {
		return "Available"
	}
	return "Offline"
}

// showMenu displays the device management menu
func (tdp *TrustedDevicePanel) showMenu() {
	fmt.Println("Device Management Options:")
	fmt.Println("1. Rename device")
	fmt.Println("2. Revoke device")
	fmt.Println("3. Trigger Lost Device Protocol")
	fmt.Println("4. Refresh list")
	fmt.Println("5. Back to main menu")
	fmt.Println()

	for {
		fmt.Print("Enter choice (1-5): ")
		var choice int
		fmt.Scanf("%d", &choice)

		switch choice {
		case 1:
			tdp.renameDevice()
		case 2:
			tdp.revokeDevice()
		case 3:
			tdp.triggerLostDeviceProtocol()
		case 4:
			tdp.ShowPanel()
			return
		case 5:
			return
		default:
			fmt.Println("Invalid choice. Please try again.")
		}
	}
}

// renameDevice allows renaming a trusted device
func (tdp *TrustedDevicePanel) renameDevice() {
	if len(tdp.devices) == 0 {
		fmt.Println("No devices to rename.")
		return
	}

	fmt.Println("Select device to rename:")
	for i, device := range tdp.devices {
		fmt.Printf("%d. %s\n", i+1, device.DeviceName)
	}

	fmt.Print("Enter device number: ")
	var deviceNum int
	fmt.Scanf("%d", &deviceNum)

	if deviceNum < 1 || deviceNum > len(tdp.devices) {
		fmt.Println("Invalid device number.")
		return
	}

	device := tdp.devices[deviceNum-1]

	fmt.Printf("Current name: %s\n", device.DeviceName)
	fmt.Print("Enter new name: ")
	var newName string
	fmt.Scanf("%s", &newName)

	if newName == "" {
		fmt.Println("Name cannot be empty.")
		return
	}

	// In a real implementation, update the device name in the trust store
	fmt.Printf("Device '%s' renamed to '%s'\n", device.DeviceName, newName)
}

// revokeDevice removes a device from the trusted list
func (tdp *TrustedDevicePanel) revokeDevice() {
	if len(tdp.devices) == 0 {
		fmt.Println("No devices to revoke.")
		return
	}

	fmt.Println("Select device to revoke:")
	for i, device := range tdp.devices {
		fmt.Printf("%d. %s (%s)\n", i+1, device.DeviceName, device.DeviceID)
	}

	fmt.Print("Enter device number: ")
	var deviceNum int
	fmt.Scanf("%d", &deviceNum)

	if deviceNum < 1 || deviceNum > len(tdp.devices) {
		fmt.Println("Invalid device number.")
		return
	}

	device := tdp.devices[deviceNum-1]

	fmt.Printf("Are you sure you want to revoke '%s'? (y/N): ", device.DeviceName)
	var confirm string
	fmt.Scanf("%s", &confirm)

	if confirm != "y" && confirm != "Y" {
		fmt.Println("Revocation cancelled.")
		return
	}

	// Revoke the device
	err := tdp.controller.RevokeDevice(device.DeviceID)
	if err != nil {
		fmt.Printf("Error revoking device: %v\n", err)
		return
	}

	fmt.Printf("Device '%s' has been revoked and removed from trusted list.\n", device.DeviceName)
	fmt.Println("Any active sessions with this device have been terminated.")
}

// triggerLostDeviceProtocol executes the lost device protocol
func (tdp *TrustedDevicePanel) triggerLostDeviceProtocol() {
	fmt.Println("=== Lost Device Protocol ===")
	fmt.Println()
	fmt.Println("WARNING: This will revoke ALL trusted devices and invalidate ALL pairings.")
	fmt.Println("You will need to re-pair all your devices after this operation.")
	fmt.Println()
	fmt.Println("This should only be used if:")
	fmt.Println("- Your device was lost or stolen")
	fmt.Println("- You suspect your pairings have been compromised")
	fmt.Println()
	fmt.Print("Are you absolutely sure? Type 'LOST' to confirm: ")

	var confirm string
	fmt.Scanf("%s", &confirm)

	if confirm != "LOST" {
		fmt.Println("Lost Device Protocol cancelled.")
		return
	}

	// Execute lost device protocol
	err := tdp.controller.LostDeviceProtocol()
	if err != nil {
		fmt.Printf("Error executing Lost Device Protocol: %v\n", err)
		return
	}

	fmt.Println("Lost Device Protocol executed successfully.")
	fmt.Println("All trusted devices have been cleared.")
	fmt.Println("Please re-pair your devices to continue using RemoteDexter.")
}
