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

// EncodeCommandRequest encodes CommandRequest to bytes
func EncodeCommandRequest(req CommandRequest) []byte {
	buf := make([]byte, 0)
	buf = append(buf, byte(len(req.Type)))
	buf = append(buf, []byte(req.Type)...)
	buf = append(buf, byte(len(req.Payload)))
	buf = append(buf, req.Payload...)
	return FrameMessage(buf)
}

// DecodeCommandRequest decodes bytes to CommandRequest
func DecodeCommandRequest(data []byte) (CommandRequest, error) {
	if len(data) < 2 {
		return CommandRequest{}, errors.New("data too short")
	}
	typeLen := int(data[0])
	if len(data) < 1+typeLen+1 {
		return CommandRequest{}, errors.New("data too short for type")
	}
	cmdType := string(data[1 : 1+typeLen])
	payloadLen := int(data[1+typeLen])
	payload := data[1+typeLen+1:]
	if len(payload) != payloadLen {
		return CommandRequest{}, errors.New("payload length mismatch")
	}
	return CommandRequest{
		Type:    cmdType,
		Payload: payload,
	}, nil
}

// EncodeCommandResponse encodes CommandResponse to bytes
func EncodeCommandResponse(resp CommandResponse) []byte {
	buf := make([]byte, 0)
	buf = append(buf, byte(len(resp.Status)))
	buf = append(buf, []byte(resp.Status)...)
	buf = append(buf, byte(len(resp.Payload)))
	buf = append(buf, resp.Payload...)
	return FrameMessage(buf)
}

// DecodeCommandResponse decodes bytes to CommandResponse
func DecodeCommandResponse(data []byte) (CommandResponse, error) {
	if len(data) < 2 {
		return CommandResponse{}, errors.New("data too short")
	}
	statusLen := int(data[0])
	if len(data) < 1+statusLen+1 {
		return CommandResponse{}, errors.New("data too short for status")
	}
	status := string(data[1 : 1+statusLen])
	payloadLen := int(data[1+statusLen])
	payload := data[1+statusLen+1:]
	if len(payload) != payloadLen {
		return CommandResponse{}, errors.New("payload length mismatch")
	}
	return CommandResponse{
		Status:  status,
		Payload: payload,
	}, nil
}

// EncodeTouchEvent encodes TouchEvent to bytes
func EncodeTouchEvent(event TouchEvent) []byte {
	buf := make([]byte, 0)
	buf = append(buf, byte(event.X>>8), byte(event.X&0xFF))
	buf = append(buf, byte(event.Y>>8), byte(event.Y&0xFF))
	buf = append(buf, byte(event.Action))
	return FrameMessage(buf)
}

// DecodeTouchEvent decodes bytes to TouchEvent
func DecodeTouchEvent(data []byte) (TouchEvent, error) {
	if len(data) < 5 {
		return TouchEvent{}, errors.New("data too short")
	}
	x := int(data[0])<<8 | int(data[1])
	y := int(data[2])<<8 | int(data[3])
	action := int(data[4])
	return TouchEvent{X: x, Y: y, Action: action}, nil
}

// EncodeMouseMove encodes MouseMove to bytes
func EncodeMouseMove(event MouseMove) []byte {
	buf := make([]byte, 0)
	buf = append(buf, byte(event.DX>>8), byte(event.DX&0xFF))
	buf = append(buf, byte(event.DY>>8), byte(event.DY&0xFF))
	return FrameMessage(buf)
}

// DecodeMouseMove decodes bytes to MouseMove
func DecodeMouseMove(data []byte) (MouseMove, error) {
	if len(data) < 4 {
		return MouseMove{}, errors.New("data too short")
	}
	dx := int(data[0])<<8 | int(data[1])
	dy := int(data[2])<<8 | int(data[3])
	return MouseMove{DX: dx, DY: dy}, nil
}

// EncodeMouseClick encodes MouseClick to bytes
func EncodeMouseClick(event MouseClick) []byte {
	buf := make([]byte, 0)
	buf = append(buf, byte(event.Button))
	buf = append(buf, byte(event.Action))
	return FrameMessage(buf)
}

// DecodeMouseClick decodes bytes to MouseClick
func DecodeMouseClick(data []byte) (MouseClick, error) {
	if len(data) < 2 {
		return MouseClick{}, errors.New("data too short")
	}
	return MouseClick{Button: int(data[0]), Action: int(data[1])}, nil
}

// EncodeKeyEvent encodes KeyEvent to bytes
func EncodeKeyEvent(event KeyEvent) []byte {
	buf := make([]byte, 0)
	buf = append(buf, byte(event.KeyCode>>8), byte(event.KeyCode&0xFF))
	buf = append(buf, byte(event.Action))
	return FrameMessage(buf)
}

// DecodeKeyEvent decodes bytes to KeyEvent
func DecodeKeyEvent(data []byte) (KeyEvent, error) {
	if len(data) < 3 {
		return KeyEvent{}, errors.New("data too short")
	}
	keyCode := int(data[0])<<8 | int(data[1])
	action := int(data[2])
	return KeyEvent{KeyCode: keyCode, Action: action}, nil
}

// EncodeScrollEvent encodes ScrollEvent to bytes
func EncodeScrollEvent(event ScrollEvent) []byte {
	buf := make([]byte, 0)
	buf = append(buf, byte(event.Amount>>8), byte(event.Amount&0xFF))
	return FrameMessage(buf)
}

// DecodeScrollEvent decodes bytes to ScrollEvent
func DecodeScrollEvent(data []byte) (ScrollEvent, error) {
	if len(data) < 2 {
		return ScrollEvent{}, errors.New("data too short")
	}
	amount := int(data[0])<<8 | int(data[1])
	return ScrollEvent{Amount: amount}, nil
}
