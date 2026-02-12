package security

import (
	"os"
	"testing"
)

func TestFileBasedTrustStore(t *testing.T) {
	// Create a temporary directory for testing
	tempDir, err := os.MkdirTemp("", "truststore_test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	store, err := NewFileBasedTrustStore(tempDir)
	if err != nil {
		t.Fatalf("Failed to create trust store: %v", err)
	}

	// Test adding a device
	testKey := []byte("test-public-key-12345")
	err = store.AddDevice("Test Device", testKey)
	if err != nil {
		t.Fatalf("Failed to add device: %v", err)
	}

	// Test checking if device is trusted
	if !store.IsTrusted(testKey) {
		t.Error("Device should be trusted after adding")
	}

	// Test listing devices
	devices, err := store.ListDevices()
	if err != nil {
		t.Fatalf("Failed to list devices: %v", err)
	}

	if len(devices) != 1 {
		t.Errorf("Expected 1 device, got %d", len(devices))
	}

	if devices[0].DeviceName != "Test Device" {
		t.Errorf("Expected device name 'Test Device', got '%s'", devices[0].DeviceName)
	}

	// Test getting device by ID
	device, err := store.GetDevice(devices[0].DeviceID)
	if err != nil {
		t.Fatalf("Failed to get device: %v", err)
	}

	if device.DeviceName != "Test Device" {
		t.Errorf("Retrieved device name mismatch")
	}

	// Test removing device
	err = store.RemoveDevice(devices[0].DeviceID)
	if err != nil {
		t.Fatalf("Failed to remove device: %v", err)
	}

	// Check that device is no longer trusted
	if store.IsTrusted(testKey) {
		t.Error("Device should not be trusted after removal")
	}

	// Check that list is empty
	devices, err = store.ListDevices()
	if err != nil {
		t.Fatalf("Failed to list devices after removal: %v", err)
	}

	if len(devices) != 0 {
		t.Errorf("Expected 0 devices after removal, got %d", len(devices))
	}
}

func TestDeviceIDGeneration(t *testing.T) {
	key1 := []byte("key1")
	key2 := []byte("key2")

	id1 := generateDeviceID(key1)
	id2 := generateDeviceID(key2)

	if id1 == id2 {
		t.Error("Different keys should generate different IDs")
	}

	if len(id1) != 64 { // SHA256 hex encoded
		t.Errorf("Expected ID length 64, got %d", len(id1))
	}

	// Same key should generate same ID
	id1Again := generateDeviceID(key1)
	if id1 != id1Again {
		t.Error("Same key should generate same ID")
	}
}

func TestTrustStorePersistence(t *testing.T) {
	// Create a temporary directory for testing
	tempDir, err := os.MkdirTemp("", "truststore_persist_test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	// Create first store instance and add device
	store1, err := NewFileBasedTrustStore(tempDir)
	if err != nil {
		t.Fatalf("Failed to create first store: %v", err)
	}

	testKey := []byte("persistent-test-key")
	err = store1.AddDevice("Persistent Device", testKey)
	if err != nil {
		t.Fatalf("Failed to add device to first store: %v", err)
	}

	// Create second store instance (should load from file)
	store2, err := NewFileBasedTrustStore(tempDir)
	if err != nil {
		t.Fatalf("Failed to create second store: %v", err)
	}

	// Check that device persists
	if !store2.IsTrusted(testKey) {
		t.Error("Device should persist across store instances")
	}

	devices, err := store2.ListDevices()
	if err != nil {
		t.Fatalf("Failed to list devices in second store: %v", err)
	}

	if len(devices) != 1 {
		t.Errorf("Expected 1 persisted device, got %d", len(devices))
	}
}
