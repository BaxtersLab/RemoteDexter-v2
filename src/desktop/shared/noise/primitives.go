package noise

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/sha256"
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

// HKDF derives keys using HKDF
func HKDF(secret, salt, info []byte, length int) ([]byte, error) {
	hkdf := hkdf.New(sha256.New, secret, salt, info)
	key := make([]byte, length)
	_, err := io.ReadFull(hkdf, key)
	return key, err
}

// AEAD_Encrypt encrypts data using AES-GCM
func AEAD_Encrypt(key, nonce, plaintext []byte) ([]byte, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}

	aesgcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}

	ciphertext := aesgcm.Seal(nil, nonce, plaintext, nil)
	return ciphertext, nil
}

// AEAD_Decrypt decrypts data using AES-GCM
func AEAD_Decrypt(key, nonce, ciphertext []byte) ([]byte, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}

	aesgcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}

	plaintext, err := aesgcm.Open(nil, nonce, ciphertext, nil)
	if err != nil {
		return nil, err
	}

	return plaintext, nil
}
