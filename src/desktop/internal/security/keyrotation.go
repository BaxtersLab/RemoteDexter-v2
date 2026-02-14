package security

import (
	"crypto/rand"
	"fmt"
)

// KeyRotator performs key rotation of trusted devices.
type KeyRotator struct {
	store TrustedDeviceStore
}

// NewKeyRotator constructs a KeyRotator.
func NewKeyRotator(store TrustedDeviceStore) *KeyRotator {
	return &KeyRotator{store: store}
}

// RotateKeys rotates the public keys for all trusted devices.
// It prefers updating in-place when the underlying store is a
// *FileBasedTrustStore; otherwise it falls back to remove+add.
func (kr *KeyRotator) RotateKeys() error {
	devices, err := kr.store.ListDevices()
	if err != nil {
		return err
	}

	if fs, ok := kr.store.(*FileBasedTrustStore); ok {
		fs.mu.Lock()
		defer fs.mu.Unlock()
		for _, d := range devices {
			newKey := make([]byte, 32)
			if _, err := rand.Read(newKey); err != nil {
				return fmt.Errorf("failed to generate new key: %v", err)
			}
			d.PublicKey = newKey
			fs.devices[d.DeviceID] = d
		}
		return fs.saveDevices()
	}

	for _, d := range devices {
		newKey := make([]byte, 32)
		if _, err := rand.Read(newKey); err != nil {
			return fmt.Errorf("failed to generate new key: %v", err)
		}
		if err := kr.store.RemoveDevice(d.DeviceID); err != nil {
			return fmt.Errorf("failed to remove device %s: %v", d.DeviceID, err)
		}
		if err := kr.store.AddDevice(d.DeviceName, newKey); err != nil {
			return fmt.Errorf("failed to add device %s: %v", d.DeviceName, err)
		}
	}

	return nil
}
