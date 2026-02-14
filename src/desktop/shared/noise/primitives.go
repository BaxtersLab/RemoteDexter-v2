package noise

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/sha256"
	"errors"
	"io"

	"golang.org/x/crypto/curve25519"
	"golang.org/x/crypto/hkdf"
)

// GenerateKey generates a random key
func GenerateKey() ([]byte, error) {
	key := make([]byte, 32)
	_, err := rand.Read(key)
	return key, err
}

// GenerateKeyPair generates X25519 keypair
func GenerateKeyPair() (public, private []byte, err error) {
	private = make([]byte, 32)
	_, err = rand.Read(private)
	if err != nil {
		return
	}
	public, err = curve25519.X25519(private, curve25519.Basepoint)
	return
}

// SharedSecret computes X25519 shared secret
func SharedSecret(private, public []byte) ([]byte, error) {
	return curve25519.X25519(private, public)
}

// AEAD_Encrypt encrypts plaintext with key and nonce
func AEAD_Encrypt(key, nonce, plaintext []byte) ([]byte, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}
	aesgcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}
	return aesgcm.Seal(nil, nonce, plaintext, nil), nil
}

// AEAD_Decrypt decrypts ciphertext with key and nonce
func AEAD_Decrypt(key, nonce, ciphertext []byte) ([]byte, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}
	aesgcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}
	if len(nonce) != aesgcm.NonceSize() {
		return nil, errors.New("invalid nonce length")
	}
	return aesgcm.Open(nil, nonce, ciphertext, nil)
}

// ValidateAEAD tests encryption/decryption with test vectors
func ValidateAEAD() error {
	key := []byte("12345678901234567890123456789012")
	nonce := []byte("123456789012")
	plaintext := []byte("test message")
	ciphertext, err := AEAD_Encrypt(key, nonce, plaintext)
	if err != nil {
		return err
	}
	decrypted, err := AEAD_Decrypt(key, nonce, ciphertext)
	if err != nil {
		return err
	}
	if string(decrypted) != string(plaintext) {
		return errors.New("AEAD validation failed")
	}
	return nil
}

// HKDF derives keys using HKDF
func HKDF(secret, salt, info []byte, length int) ([]byte, error) {
	hkdf := hkdf.New(sha256.New, secret, salt, info)
	key := make([]byte, length)
	_, err := io.ReadFull(hkdf, key)
	return key, err
}
