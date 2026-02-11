package transport

import (
	"fmt"
	noisepkg "remotedexter/desktop/shared/noise"
	"remotedexter/desktop/shared/protocol"
)

type Selector struct{}

func NewSelector() *Selector {
	return &Selector{}
}

func (s *Selector) SelectTransport() string {
	return "bluetooth"
}

func (s *Selector) SendCommand(req protocol.CommandRequest, sessionKey []byte, nonce *uint64) (protocol.CommandResponse, error) {
	// Encode request
	encoded := protocol.EncodeCommandRequest(req)
	// Encrypt
	nonceBytes := make([]byte, 12)
	for i := 0; i < 12; i++ {
		nonceBytes[i] = byte((*nonce >> (8 * i)) & 0xFF)
	}
	encrypted, err := noisepkg.AEAD_Encrypt(sessionKey, nonceBytes, encoded)
	if err != nil {
		return protocol.CommandResponse{}, fmt.Errorf("encryption failed: %v", err)
	}
	*nonce++

	fmt.Printf("Command sent: %s\n", req.Type)

	// Simulate send and receive with retry
	var resp protocol.CommandResponse
	var respErr error
	for retries := 0; retries < 3; retries++ {
		// Simulate response
		resp = protocol.CommandResponse{Status: "ok", Payload: []byte("pong")}
		respEncoded := protocol.EncodeCommandResponse(resp)
		// Decrypt
		respDecrypted, err := noisepkg.AEAD_Decrypt(sessionKey, nonceBytes, respEncoded)
		if err != nil {
			respErr = fmt.Errorf("decryption failed: %v", err)
			continue
		}
		*nonce++

		receivedResp, err := protocol.DecodeCommandResponse(respDecrypted[4:]) // skip frame
		if err != nil {
			respErr = fmt.Errorf("decode failed: %v", err)
			continue
		}

		fmt.Printf("Response received: %s\n", receivedResp.Status)
		return receivedResp, nil
	}
	return protocol.CommandResponse{}, respErr
}
