package input

import (
	"math"
)

// SmoothingConfig holds smoothing parameters
type SmoothingConfig struct {
	VelocityCurve float64 // Curve factor for velocity
	LatencyComp   float64 // Latency compensation factor
	PrecisionMode bool    // Reduced sensitivity mode
	Normalization bool    // Enable coordinate normalization
}

// InputSmoothing applies smoothing algorithms to input events
type InputSmoothing struct {
	config               SmoothingConfig
	lastDX, lastDY       int
	velocityX, velocityY float64
}

func NewInputSmoothing() *InputSmoothing {
	return &InputSmoothing{
		config: SmoothingConfig{
			VelocityCurve: 0.8,
			LatencyComp:   0.1,
			PrecisionMode: false,
			Normalization: true,
		},
	}
}

func (is *InputSmoothing) SetPrecisionMode(enabled bool) {
	is.config.PrecisionMode = enabled
}

// SmoothMouseMove applies smoothing to mouse movement
func (is *InputSmoothing) SmoothMouseMove(rawDX, rawDY int) (int, int) {
	if !is.config.Normalization {
		return rawDX, rawDY
	}

	// Convert to float for calculations
	dx := float64(rawDX)
	dy := float64(rawDY)

	// Apply precision mode (reduce sensitivity)
	if is.config.PrecisionMode {
		dx *= 0.5
		dy *= 0.5
	}

	// Apply velocity curve (similar to RustDesk)
	velocityX := math.Pow(math.Abs(dx), is.config.VelocityCurve) * sign(dx)
	velocityY := math.Pow(math.Abs(dy), is.config.VelocityCurve) * sign(dy)

	// Apply latency compensation
	velocityX += (velocityX - is.velocityX) * is.config.LatencyComp
	velocityY += (velocityY - is.velocityY) * is.config.LatencyComp

	// Update stored velocity
	is.velocityX = velocityX
	is.velocityY = velocityY

	// Convert back to int
	smoothedDX := int(math.Round(velocityX))
	smoothedDY := int(math.Round(velocityY))

	return smoothedDX, smoothedDY
}

// NormalizeCoordinates converts screen coordinates to normalized values
func (is *InputSmoothing) NormalizeCoordinates(x, y, screenWidth, screenHeight int) (float64, float64) {
	if !is.config.Normalization {
		return float64(x), float64(y)
	}

	// Normalize to 0.0-1.0 range
	normX := float64(x) / float64(screenWidth)
	normY := float64(y) / float64(screenHeight)

	// Clamp to valid range
	if normX < 0 {
		normX = 0
	} else if normX > 1 {
		normX = 1
	}
	if normY < 0 {
		normY = 0
	} else if normY > 1 {
		normY = 1
	}

	return normX, normY
}

// DenormalizeCoordinates converts normalized coordinates back to screen pixels
func (is *InputSmoothing) DenormalizeCoordinates(normX, normY float64, screenWidth, screenHeight int) (int, int) {
	x := int(math.Round(normX * float64(screenWidth)))
	y := int(math.Round(normY * float64(screenHeight)))

	// Clamp to screen bounds
	if x < 0 {
		x = 0
	} else if x >= screenWidth {
		x = screenWidth - 1
	}
	if y < 0 {
		y = 0
	} else if y >= screenHeight {
		y = screenHeight - 1
	}

	return x, y
}

func sign(x float64) float64 {
	if x > 0 {
		return 1
	} else if x < 0 {
		return -1
	}
	return 0
}
