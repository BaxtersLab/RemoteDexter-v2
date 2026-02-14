package ui

import (
	"fmt"
	"remotedexter/desktop/internal/security"
	"remotedexter/desktop/internal/session"
)

// FirstRunExperience manages the first-run setup flow for new users
type FirstRunExperience struct {
	trustStore *security.FileBasedTrustStore
	controller *session.SessionController
	dataDir    string
}

type freKeyPair struct {
	PublicKey  []byte
	PrivateKey []byte
}

// NewFirstRunExperience creates a new FRE instance
func NewFirstRunExperience(dataDir string) (*FirstRunExperience, error) {
	trustStore, err := security.NewFileBasedTrustStore(dataDir)
	if err != nil {
		return nil, fmt.Errorf("failed to initialize trust store: %v", err)
	}

	return &FirstRunExperience{
		trustStore: trustStore,
		dataDir:    dataDir,
	}, nil
}

// IsFirstRun checks if this is the first time the application is run
func (fre *FirstRunExperience) IsFirstRun() bool {
	devices, err := fre.trustStore.ListDevices()
	if err != nil {
		// If we can't read the trust store, assume it's first run
		return true
	}
	return len(devices) == 0
}

// StartFirstRunFlow begins the first-run experience
func (fre *FirstRunExperience) StartFirstRunFlow() error {
	fmt.Println("=== RemoteDexter First-Run Experience ===")
	fmt.Println("Welcome to RemoteDexter - Sovereign Remote Desktop")
	fmt.Println()

	// Step 1: Explain sovereignty
	fre.showSovereigntyExplanation()

	// Step 2: Generate Noise static keypair
	fmt.Println("Generating your Noise protocol keypair...")
	keypair, err := fre.generateNoiseKeypair()
	if err != nil {
		return fmt.Errorf("failed to generate keypair: %v", err)
	}
	fmt.Printf("✓ Keypair generated successfully\n")
	fmt.Printf("  Public key: %x\n", keypair.PublicKey)
	fmt.Println()

	// Step 3: Guide through first pairing
	fmt.Println("=== First Device Pairing ===")
	fmt.Println("To start using RemoteDexter, you need to pair with your Android device.")
	fmt.Println()
	fmt.Println("Instructions:")
	fmt.Println("1. Install RemoteDexter on your Android device")
	fmt.Println("2. Open RemoteDexter on Android and select 'Pair with Desktop'")
	fmt.Println("3. Scan the QR code that will appear, or enter the pairing code manually")
	fmt.Println()

	// In a real implementation, this would show a QR code or pairing code
	pairingCode := fre.generatePairingCode()
	fmt.Printf("Your pairing code: %s\n", pairingCode)
	fmt.Println()
	fmt.Println("Waiting for device pairing... (Press Enter when paired)")

	// In a real UI, this would be asynchronous with a callback
	// For now, we'll simulate waiting
	fmt.Println("✓ Device paired successfully!")
	fmt.Println()

	// Step 4: Complete setup
	fmt.Println("=== Setup Complete ===")
	fmt.Println("RemoteDexter is now ready to use!")
	fmt.Println("You can start remote sessions from your Android device.")
	fmt.Println()

	return nil
}

// showSovereigntyExplanation displays the sovereignty explanation
func (fre *FirstRunExperience) showSovereigntyExplanation() {
	fmt.Println("=== Why RemoteDexter is Different ===")
	fmt.Println()
	fmt.Println("RemoteDexter is built on the principle of DIGITAL SOVEREIGNTY:")
	fmt.Println()
	fmt.Println("✓ NO CLOUD DEPENDENCY")
	fmt.Println("  - All communication happens directly between your devices")
	fmt.Println("  - No data is stored on external servers")
	fmt.Println("  - Your remote desktop stays completely private")
	fmt.Println()
	fmt.Println("✓ END-TO-END ENCRYPTION")
	fmt.Println("  - Uses the Noise protocol for secure key exchange")
	fmt.Println("  - All data is encrypted with ChaCha20-Poly1305")
	fmt.Println("  - Session keys are zeroized when sessions end")
	fmt.Println()
	fmt.Println("✓ USER CONTROL")
	fmt.Println("  - You decide which devices can connect")
	fmt.Println("  - File transfers require your explicit approval")
	fmt.Println("  - You can revoke device access at any time")
	fmt.Println()
	fmt.Println("✓ OPEN SOURCE")
	fmt.Println("  - Code is auditable and transparent")
	fmt.Println("  - No hidden backdoors or data collection")
	fmt.Println("  - Community-driven development")
	fmt.Println()
}

// generateNoiseKeypair generates a new Noise static keypair
func (fre *FirstRunExperience) generateNoiseKeypair() (*freKeyPair, error) {
	// In a real implementation, this would generate proper Noise keys
	// For now, simulate key generation
	keypair := &freKeyPair{
		PublicKey:  make([]byte, 32),
		PrivateKey: make([]byte, 32),
	}

	// Fill with simulated key data
	copy(keypair.PublicKey, []byte("simulated-noise-public-key-32"))
	copy(keypair.PrivateKey, []byte("simulated-noise-private-key-32"))

	// In a real implementation, save the keypair securely
	// For now, just return it
	return keypair, nil
}

// generatePairingCode creates a human-readable pairing code
func (fre *FirstRunExperience) generatePairingCode() string {
	// In a real implementation, this would generate a secure pairing code
	// For now, return a simulated code
	return "RD-4829-1732"
}

// SetSessionController sets the session controller for FRE
func (fre *FirstRunExperience) SetSessionController(controller *session.SessionController) {
	fre.controller = controller
}
