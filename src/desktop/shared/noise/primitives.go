package noise

import (
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
