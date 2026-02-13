package noise

import (
	"bytes"
	"testing"
)

// TestX25519KeyAgreement tests X25519 key agreement correctness
func TestX25519KeyAgreement(t *testing.T) {
	// Generate two keypairs
	pubA, privA, err := GenerateKeyPair()
	if err != nil {
		t.Fatalf("Failed to generate keypair A: %v", err)
	}

	pubB, privB, err := GenerateKeyPair()
	if err != nil {
		t.Fatalf("Failed to generate keypair B: %v", err)
	}

	// Compute shared secrets
	secretA, err := SharedSecret(privA, pubB)
	if err != nil {
		t.Fatalf("Failed to compute shared secret A: %v", err)
	}

	secretB, err := SharedSecret(privB, pubA)
	if err != nil {
		t.Fatalf("Failed to compute shared secret B: %v", err)
	}

	// Secrets should be identical
	if !bytes.Equal(secretA, secretB) {
		t.Error("Shared secrets do not match")
	}

	// Secrets should be non-zero
	if bytes.Equal(secretA, make([]byte, len(secretA))) {
		t.Error("Shared secret is zero")
	}

	// Test with invalid public key (all zeros)
	invalidPub := make([]byte, 32)
	_, err = SharedSecret(privA, invalidPub)
	if err == nil {
		t.Error("Expected error with invalid public key")
	}
}

// TestHKDFDerivation tests HKDF key derivation consistency
func TestHKDFDerivation(t *testing.T) {
	secret := []byte("test-secret-12345678901234567890")
	salt := []byte("test-salt-1234567890")
	info := []byte("test-info")

	// Derive keys of different lengths
	key16, err := HKDF(secret, salt, info, 16)
	if err != nil {
		t.Fatalf("Failed to derive 16-byte key: %v", err)
	}

	key32, err := HKDF(secret, salt, info, 32)
	if err != nil {
		t.Fatalf("Failed to derive 32-byte key: %v", err)
	}

	key64, err := HKDF(secret, salt, info, 64)
	if err != nil {
		t.Fatalf("Failed to derive 64-byte key: %v", err)
	}

	// Keys should be different lengths
	if len(key16) != 16 || len(key32) != 32 || len(key64) != 64 {
		t.Error("Key lengths incorrect")
	}

	// Keys should be deterministic (same inputs produce same outputs)
	key16Again, err := HKDF(secret, salt, info, 16)
	if err != nil {
		t.Fatalf("Failed to derive 16-byte key again: %v", err)
	}

	if !bytes.Equal(key16, key16Again) {
		t.Error("HKDF is not deterministic")
	}

	// Different info should produce different keys
	info2 := []byte("different-info")
	key16Diff, err := HKDF(secret, salt, info2, 16)
	if err != nil {
		t.Fatalf("Failed to derive key with different info: %v", err)
	}

	if bytes.Equal(key16, key16Diff) {
		t.Error("Different info produced same key")
	}
}

// TestAEADCorrectness tests AEAD encryption/decryption
func TestAEADCorrectness(t *testing.T) {
	key := []byte("12345678901234567890123456789012")
	nonce := []byte("123456789012")
	plaintext := []byte("test message for AEAD")

	// Encrypt
	ciphertext, err := AEAD_Encrypt(key, nonce, plaintext)
	if err != nil {
		t.Fatalf("AEAD encryption failed: %v", err)
	}

	// Ciphertext should be longer than plaintext (includes auth tag)
	if len(ciphertext) <= len(plaintext) {
		t.Error("Ciphertext is not longer than plaintext")
	}

	// Decrypt
	decrypted, err := AEAD_Decrypt(key, nonce, ciphertext)
	if err != nil {
		t.Fatalf("AEAD decryption failed: %v", err)
	}

	// Should recover original plaintext
	if !bytes.Equal(plaintext, decrypted) {
		t.Error("Decryption did not recover original plaintext")
	}
}

// TestAEADNegativeCases tests AEAD with invalid inputs
func TestAEADNegativeCases(t *testing.T) {
	key := []byte("12345678901234567890123456789012")
	nonce := []byte("123456789012")
	plaintext := []byte("test message")

	// Valid encryption
	ciphertext, err := AEAD_Encrypt(key, nonce, plaintext)
	if err != nil {
		t.Fatalf("Valid encryption failed: %v", err)
	}

	// Test with wrong key
	wrongKey := []byte("wrongkey123456789012345678901234")
	_, err = AEAD_Decrypt(wrongKey, nonce, ciphertext)
	if err == nil {
		t.Error("Expected decryption to fail with wrong key")
	}

	// Test with wrong nonce
	wrongNonce := []byte("wrongnonce12345")
	_, err = AEAD_Decrypt(key, wrongNonce, ciphertext)
	if err == nil {
		t.Error("Expected decryption to fail with wrong nonce")
	}

	// Test with truncated ciphertext
	truncated := ciphertext[:len(ciphertext)-1]
	_, err = AEAD_Decrypt(key, nonce, truncated)
	if err == nil {
		t.Error("Expected decryption to fail with truncated ciphertext")
	}

	// Test with modified ciphertext
	modified := make([]byte, len(ciphertext))
	copy(modified, ciphertext)
	modified[len(modified)-1] ^= 0x01 // Flip last bit
	_, err = AEAD_Decrypt(key, nonce, modified)
	if err == nil {
		t.Error("Expected decryption to fail with modified ciphertext")
	}
}

// TestNonceIncrementBehavior tests nonce management
func TestNonceIncrementBehavior(t *testing.T) {
	key := []byte("12345678901234567890123456789012")
	plaintext := []byte("test message")

	// Test sequential nonces
	nonce1 := []byte("123456789012")
	nonce2 := []byte("123456789013")

	ciphertext1, err := AEAD_Encrypt(key, nonce1, plaintext)
	if err != nil {
		t.Fatalf("Encryption with nonce1 failed: %v", err)
	}

	ciphertext2, err := AEAD_Encrypt(key, nonce2, plaintext)
	if err != nil {
		t.Fatalf("Encryption with nonce2 failed: %v", err)
	}

	// Ciphertexts should be different (different nonces)
	if bytes.Equal(ciphertext1, ciphertext2) {
		t.Error("Same plaintext with different nonces produced same ciphertext")
	}

	// Test nonce reuse detection (simulate by trying to decrypt with wrong nonce)
	_, err = AEAD_Decrypt(key, nonce2, ciphertext1)
	if err == nil {
		t.Error("Expected decryption to fail when using wrong nonce")
	}
}

// TestKeyGeneration tests key generation randomness
func TestKeyGeneration(t *testing.T) {
	// Generate multiple keys
	key1, err := GenerateKey()
	if err != nil {
		t.Fatalf("Failed to generate key1: %v", err)
	}

	key2, err := GenerateKey()
	if err != nil {
		t.Fatalf("Failed to generate key2: %v", err)
	}

	// Keys should be different
	if bytes.Equal(key1, key2) {
		t.Error("Generated keys are identical (should be random)")
	}

	// Keys should be correct length (32 bytes for X25519)
	if len(key1) != 32 || len(key2) != 32 {
		t.Error("Generated keys have wrong length")
	}

	// Keys should be non-zero
	zeroKey := make([]byte, 32)
	if bytes.Equal(key1, zeroKey) || bytes.Equal(key2, zeroKey) {
		t.Error("Generated key is zero")
	}
}

// TestKeyPairGeneration tests X25519 keypair generation
func TestKeyPairGeneration(t *testing.T) {
	pub, priv, err := GenerateKeyPair()
	if err != nil {
		t.Fatalf("Failed to generate keypair: %v", err)
	}

	// Check lengths
	if len(pub) != 32 || len(priv) != 32 {
		t.Error("Keypair has wrong length")
	}

	// Private key should be non-zero
	if bytes.Equal(priv, make([]byte, 32)) {
		t.Error("Private key is zero")
	}

	// Public key should be non-zero
	if bytes.Equal(pub, make([]byte, 32)) {
		t.Error("Public key is zero")
	}

	// Test that different calls produce different keypairs
	pub2, priv2, err := GenerateKeyPair()
	if err != nil {
		t.Fatalf("Failed to generate second keypair: %v", err)
	}

	if bytes.Equal(pub, pub2) || bytes.Equal(priv, priv2) {
		t.Error("Generated keypairs are identical")
	}
}

// TestValidateAEAD tests the AEAD validation function
func TestValidateAEAD(t *testing.T) {
	err := ValidateAEAD()
	if err != nil {
		t.Fatalf("AEAD validation failed: %v", err)
	}
}
