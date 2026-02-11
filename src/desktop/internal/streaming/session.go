package streaming

import (
	"fmt"
	"remotedexter/desktop/internal/transport"
	"remotedexter/desktop/shared/protocol"
)

type StreamingSession struct {
	selector    *transport.Selector
	decoder     *VideoDecoder
	renderer    *Renderer
	sessionKey  []byte
	nonce       uint64
	isStreaming bool
}

func NewStreamingSession() *StreamingSession {
	return &StreamingSession{
		selector: transport.NewSelector(),
		decoder:  NewVideoDecoder(),
		renderer: NewRenderer(1280, 720),
	}
}

func (ss *StreamingSession) StartStreaming() error {
	if ss.isStreaming {
		return fmt.Errorf("streaming already active")
	}

	// Initialize decoder
	if err := ss.decoder.Initialize(); err != nil {
		return fmt.Errorf("failed to initialize decoder: %v", err)
	}

	// Initialize renderer
	if err := ss.renderer.Initialize(); err != nil {
		return fmt.Errorf("failed to initialize renderer: %v", err)
	}

	// Send start streaming command
	req := protocol.CommandRequest{
		Type:    "start_streaming",
		Payload: []byte{},
	}

	resp, err := ss.selector.SendCommand(req, ss.sessionKey, &ss.nonce)
	if err != nil {
		return fmt.Errorf("failed to start streaming: %v", err)
	}

	if resp.Status != "ok" {
		return fmt.Errorf("streaming start rejected: %s", resp.Status)
	}

	ss.isStreaming = true
	fmt.Println("Streaming session started")

	// Start receive loop
	go ss.receiveLoop()

	return nil
}

func (ss *StreamingSession) receiveLoop() {
	for ss.isStreaming {
		// In a real implementation, this would receive frames from the transport
		// For now, simulate receiving H.264 data
		h264Data := []byte{} // Would be received from transport

		if len(h264Data) > 0 {
			// Decode frame
			frame, err := ss.decoder.DecodeFrame(h264Data)
			if err != nil {
				fmt.Printf("Decode error: %v\n", err)
				continue
			}

			// Render frame
			ss.renderer.RenderFrame(frame)

			// Handle events
			if !ss.renderer.HandleEvents() {
				ss.StopStreaming()
				break
			}
		}
	}
}

func (ss *StreamingSession) StopStreaming() error {
	if !ss.isStreaming {
		return nil
	}

	ss.isStreaming = false

	// Send stop streaming command
	req := protocol.CommandRequest{
		Type:    "stop_streaming",
		Payload: []byte{},
	}

	resp, err := ss.selector.SendCommand(req, ss.sessionKey, &ss.nonce)
	if err != nil {
		fmt.Printf("Warning: failed to stop streaming: %v\n", err)
	} else if resp.Status != "ok" {
		fmt.Printf("Warning: streaming stop rejected: %s\n", resp.Status)
	}

	// Close decoder and renderer
	ss.decoder.Close()
	ss.renderer.Close()

	fmt.Println("Streaming session stopped")
	return nil
}

func (ss *StreamingSession) SetSessionKeys(key []byte, nonce uint64) {
	ss.sessionKey = make([]byte, len(key))
	copy(ss.sessionKey, key)
	ss.nonce = nonce
}
