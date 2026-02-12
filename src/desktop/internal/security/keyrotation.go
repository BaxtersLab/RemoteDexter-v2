package security

import (
	protocol "command-line-argumentsC:\\RemoteDexter\\src\\shared\\protocol\\messages.go"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"sync"
	"time"
)

// TrustedDevice represents a trusted device entry
type TrustedDevice struct {
	DeviceName string `json:"device_name"`
	DeviceID   string `json:"device_id"`
	PublicKey  []byte `json:"public_key"`
	AddedAt    string `json:"added_at"`
	LastSeen   string `json:"last_seen"`
}

// TrustedDeviceStore manages the persistent storage of trusted devices
type TrustedDeviceStore interface {
	AddDevice(deviceName string, publicKey []byte) error
	RemoveDevice(deviceID string) error
	IsTrusted(publicKey []byte) bool
	ListDevices() ([]TrustedDevice, error)
	GetDevice(deviceID string) (*TrustedDevice, error)
	UpdateLastSeen(deviceID string) error
}

// FileBasedTrustStore implements TrustedDeviceStore using JSON file storage
type FileBasedTrustStore struct {
	filePath string
	devices  map[string]TrustedDevice
	mu       sync.RWMutex
}

// NewFileBasedTrustStore creates a new file-based trust store
func NewFileBasedTrustStore(dataDir string) (*FileBasedTrustStore, error) {
	filePath := filepath.Join(dataDir, "trusted_devices.json")

	store := &FileBasedTrustStore{
		filePath: filePath,
		devices:  make(map[string]TrustedDevice),
	}

	// Load existing devices
	if err := store.loadDevices(); err != nil {
		// If file doesn't exist, start with empty store
		if !os.IsNotExist(err) {
			return nil, fmt.Errorf("failed to load trusted devices: %v", err)
		}
	}

	return store, nil
}

// generateDeviceID creates a unique device ID from the public key
func generateDeviceID(publicKey []byte) string {
	hash := sha256.Sum256(publicKey)
	return hex.EncodeToString(hash[:])
}

// AddDevice adds a new trusted device
func (ts *FileBasedTrustStore) AddDevice(deviceName string, publicKey []byte) error {
	ts.mu.Lock()
	defer ts.mu.Unlock()

	deviceID := generateDeviceID(publicKey)

	// Check if device already exists
	if _, exists := ts.devices[deviceID]; exists {
		return fmt.Errorf("device already trusted")
	}

	device := TrustedDevice{
		DeviceName: deviceName,
		DeviceID:   deviceID,
		PublicKey:  publicKey,
		AddedAt:    time.Now().Format(time.RFC3339),
		LastSeen:   time.Now().Format(time.RFC3339),
	}

	ts.devices[deviceID] = device

	return ts.saveDevices()
}

// RemoveDevice removes a trusted device
func (ts *FileBasedTrustStore) RemoveDevice(deviceID string) error {
	ts.mu.Lock()
	defer ts.mu.Unlock()

	if _, exists := ts.devices[deviceID]; !exists {
		return fmt.Errorf("device not found")
	}

	delete(ts.devices, deviceID)

	return ts.saveDevices()
}

// IsTrusted checks if a device with the given public key is trusted
func (ts *FileBasedTrustStore) IsTrusted(publicKey []byte) bool {
	ts.mu.RLock()
	defer ts.mu.RUnlock()

	deviceID := generateDeviceID(publicKey)
	_, exists := ts.devices[deviceID]
	return exists
}

// ListDevices returns all trusted devices
func (ts *FileBasedTrustStore) ListDevices() ([]TrustedDevice, error) {
	ts.mu.RLock()
	defer ts.mu.RUnlock()

	devices := make([]TrustedDevice, 0, len(ts.devices))
	for _, device := range ts.devices {
		devices = append(devices, device)
	}

	return devices, nil
}

// GetDevice returns a specific trusted device by ID
func (ts *FileBasedTrustStore) GetDevice(deviceID string) (*TrustedDevice, error) {
	ts.mu.RLock()
	defer ts.mu.RUnlock()

	device, exists := ts.devices[deviceID]
	if !exists {
		return nil, fmt.Errorf("device not found")
	}

	return &device, nil
}

// UpdateLastSeen updates the last seen timestamp for a device
func (ts *FileBasedTrustStore) UpdateLastSeen(deviceID string) error {
	ts.mu.Lock()
	defer ts.mu.Unlock()

	device, exists := ts.devices[deviceID]
	if !exists {
		return fmt.Errorf("device not found")
	}

	device.LastSeen = time.Now().Format(time.RFC3339)
	ts.devices[deviceID] = device

	return ts.saveDevices()
}

// loadDevices loads trusted devices from the JSON file
func (ts *FileBasedTrustStore) loadDevices() error {
	data, err := os.ReadFile(ts.filePath)
	if err != nil {
		return err
	}

	var devices []TrustedDevice
	if err := json.Unmarshal(data, &devices); err != nil {
		return err
	}

	// Convert to map for efficient lookups
	for _, device := range devices {
		ts.devices[device.DeviceID] = device
	}

	return nil
}

// saveDevices saves trusted devices to the JSON file
func (ts *FileBasedTrustStore) saveDevices() error {
	devices := make([]TrustedDevice, 0, len(ts.devices))
	for _, device := range ts.devices {
		devices = append(devices, device)
	}

	data, err := json.MarshalIndent(devices, "", "  ")
	if err != nil {
		return err
	}

	// Ensure directory exists
	if err := os.MkdirAll(filepath.Dir(ts.filePath), 0755); err != nil {
		return err
	}

	return os.WriteFile(ts.filePath, data, 0644)
}

// ClearAllDevices removes all trusted devices (used for lost device protocol)
func (ts *FileBasedTrustStore) ClearAllDevices() error {
	ts.mu.Lock()
	defer ts.mu.Unlock()

	ts.devices = make(map[string]protocol.TrustedDevice)

	return ts.saveDevices()
}

// RateLimiter implements token bucket rate limiting
type RateLimiter struct {
	tokens     float64
	maxTokens  float64
	refillRate float64 // tokens per second
	lastRefill time.Time
	mu         sync.Mutex
}

// NewRateLimiter creates a new rate limiter
func NewRateLimiter(maxTokens, refillRate float64) *RateLimiter {
	return &RateLimiter{
		tokens:     maxTokens,
		maxTokens:  maxTokens,
		refillRate: refillRate,
		lastRefill: time.Now(),
	}
}

// Allow checks if an action is allowed and consumes a token
func (rl *RateLimiter) Allow() bool {
	rl.mu.Lock()
	defer rl.mu.Unlock()

	now := time.Now()
	elapsed := now.Sub(rl.lastRefill).Seconds()
	rl.tokens += elapsed * rl.refillRate
	if rl.tokens > rl.maxTokens {
		rl.tokens = rl.maxTokens
	}
	rl.lastRefill = now

	if rl.tokens >= 1.0 {
		rl.tokens -= 1.0
		return true
	}
	return false
}
