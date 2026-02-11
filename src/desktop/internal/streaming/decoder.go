package streaming

import (
	"fmt"
	"image"
	"image/color"
)

// VideoDecoder handles H.264 video decoding
// Note: In production, this would use FFmpeg/libavcodec
// For this implementation, we simulate decoding
type VideoDecoder struct {
	width      int
	height     int
	isDecoding bool
}

func NewVideoDecoder() *VideoDecoder {
	return &VideoDecoder{
		width:  1280,
		height: 720,
	}
}

func (vd *VideoDecoder) Initialize() error {
	vd.isDecoding = true
	fmt.Println("Video decoder initialized (simulated)")
	return nil
}

func (vd *VideoDecoder) DecodeFrame(h264Data []byte) (*image.RGBA, error) {
	if !vd.isDecoding {
		return nil, fmt.Errorf("decoder not initialized")
	}

	// Create a test frame (in real implementation, decode H.264)
	frame := image.NewRGBA(image.Rect(0, 0, vd.width, vd.height))

	// Generate a test pattern
	for y := 0; y < vd.height; y++ {
		for x := 0; x < vd.width; x++ {
			r := uint8((x * 255) / vd.width)
			g := uint8((y * 255) / vd.height)
			b := uint8(128)
			frame.Set(x, y, color.RGBA{r, g, b, 255})
		}
	}

	fmt.Printf("Decoded frame: %d bytes H.264 → %dx%d RGBA\n",
		len(h264Data), vd.width, vd.height)
	return frame, nil
}

func (vd *VideoDecoder) Close() {
	vd.isDecoding = false
	fmt.Println("Video decoder closed")
}
