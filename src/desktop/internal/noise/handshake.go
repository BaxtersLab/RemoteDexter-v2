package noise

import (
	noise "command-line-argumentsC:\\RemoteDexter\\src\\shared\\noise\\primitives.go"
	"fmt"
	noisepkg "remotedexter/desktop/shared/noise"
	"remotedexter/desktop/shared/protocol"
)

type Handshake struct{}

func NewHandshake() *Handshake {
	return &Handshake{}
}

func (h *Handshake) PerformNoiseHandshake() ([]byte, error) {
	// Generate ephemeral keypair
	ephemeralPub, ephemeralPriv, err := noisepkg.GenerateKeyPair()
	if err != nil {
		return err
	}

	// Construct NoiseInit
	initMsg := protocol.NoiseInit{
		EphemeralPublicKey: ephemeralPub,
		StaticPublicKey:    []byte{}, // empty for now
		Payload:            []byte{},
	}

	// Frame and "send" (simulate)
	_ = protocol.EncodeNoiseInit(initMsg)
	fmt.Println("NoiseInit sent")

	// Simulate receive NoiseResponse
	// In real, receive from network
	respPub, _, err := noisepkg.GenerateKeyPair()
	if err != nil {
		return err
	}
	respMsg := protocol.NoiseResponse{
		EphemeralPublicKey: respPub,
		Payload:            []byte{},
	}
	respFramed := protocol.EncodeNoiseResponse(respMsg)

	// Decode
	receivedResp, err := protocol.DecodeNoiseResponse(respFramed[4:]) // skip frame
	if err != nil {
		return err
	}
	fmt.Println("NoiseResponse received")

	// Derive shared secret
	shared, err := noisepkg.SharedSecret(ephemeralPriv, receivedResp.EphemeralPublicKey)
	if err != nil {
		return err
	}

	// Run HKDF
	sessionKey, err := noisepkg.HKDF(shared, nil, []byte("session"), 32)
	if err != nil {
		return err
	}

	fmt.Printf("Session keys derived: %x\n", sessionKey[:4])
	return sessionKey, nil
}
