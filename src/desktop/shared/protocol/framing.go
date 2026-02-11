package protocol

import (
	"encoding/binary"
	"errors"
	"io"
)

// FrameMessage adds length prefix to message
func FrameMessage(data []byte) []byte {
	length := uint32(len(data))
	buf := make([]byte, 4+len(data))
	binary.BigEndian.PutUint32(buf, length)
	copy(buf[4:], data)
	return buf
}

// ReadFrame reads a framed message
func ReadFrame(r io.Reader) ([]byte, error) {
	var length uint32
	if err := binary.Read(r, binary.BigEndian, &length); err != nil {
		return nil, err
	}
	data := make([]byte, length)
	if _, err := io.ReadFull(r, data); err != nil {
		return nil, err
	}
	return data, nil
}

// EncodeNoiseInit encodes NoiseInit to bytes
func EncodeNoiseInit(init NoiseInit) []byte {
	// Simple encoding: length-prefixed fields
	buf := make([]byte, 0)
	buf = append(buf, byte(len(init.EphemeralPublicKey)))
	buf = append(buf, init.EphemeralPublicKey...)
	buf = append(buf, byte(len(init.StaticPublicKey)))
	buf = append(buf, init.StaticPublicKey...)
	buf = append(buf, byte(len(init.Payload)))
	buf = append(buf, init.Payload...)
	return FrameMessage(buf)
}

// DecodeNoiseInit decodes bytes to NoiseInit
func DecodeNoiseInit(data []byte) (NoiseInit, error) {
	if len(data) < 3 {
		return NoiseInit{}, errors.New("data too short")
	}
	ephemeralLen := int(data[0])
	if len(data) < 1+ephemeralLen+1 {
		return NoiseInit{}, errors.New("invalid data")
	}
	ephemeral := data[1 : 1+ephemeralLen]
	staticLen := int(data[1+ephemeralLen])
	if len(data) < 1+ephemeralLen+1+staticLen+1 {
		return NoiseInit{}, errors.New("invalid data")
	}
	static := data[1+ephemeralLen+1 : 1+ephemeralLen+1+staticLen]
	payloadLen := int(data[1+ephemeralLen+1+staticLen])
	payload := data[1+ephemeralLen+1+staticLen+1:]
	if len(payload) != payloadLen {
		return NoiseInit{}, errors.New("payload length mismatch")
	}
	return NoiseInit{
		EphemeralPublicKey: ephemeral,
		StaticPublicKey:    static,
		Payload:            payload,
	}, nil
}

// EncodeNoiseResponse encodes NoiseResponse
func EncodeNoiseResponse(resp NoiseResponse) []byte {
	buf := make([]byte, 0)
	buf = append(buf, byte(len(resp.EphemeralPublicKey)))
	buf = append(buf, resp.EphemeralPublicKey...)
	buf = append(buf, byte(len(resp.Payload)))
	buf = append(buf, resp.Payload...)
	return FrameMessage(buf)
}

// DecodeNoiseResponse decodes bytes to NoiseResponse
func DecodeNoiseResponse(data []byte) (NoiseResponse, error) {
	if len(data) < 2 {
		return NoiseResponse{}, errors.New("data too short")
	}
	ephemeralLen := int(data[0])
	if len(data) < 1+ephemeralLen+1 {
		return NoiseResponse{}, errors.New("invalid data")
	}
	ephemeral := data[1 : 1+ephemeralLen]
	payloadLen := int(data[1+ephemeralLen])
	payload := data[1+ephemeralLen+1:]
	if len(payload) != payloadLen {
		return NoiseResponse{}, errors.New("payload length mismatch")
	}
	return NoiseResponse{
		EphemeralPublicKey: ephemeral,
		Payload:            payload,
	}, nil
}
