package protocol

import (
	"fmt"
	"regexp"
	"strings"
)

// ValidationError represents a command validation error
type ValidationError struct {
	Field   string
	Value   interface{}
	Reason  string
}

func (ve ValidationError) Error() string {
	return fmt.Sprintf("validation error in field '%s': %s (value: %v)", ve.Field, ve.Reason, ve.Value)
}

// CommandValidator validates and sanitizes commands
type CommandValidator struct {
	maxPayloadSize int
	maxFrameRate   int
	maxResolution  struct{ width, height int }
}

// NewCommandValidator creates a new command validator
func NewCommandValidator() *CommandValidator {
	return &CommandValidator{
		maxPayloadSize: 64 * 1024, // 64KB max payload
		maxFrameRate:   60,        // 60 FPS max
		maxResolution: struct{ width, height int }{
			width:  4096, // 4K width
			height: 2160, // 4K height
		},
	}
}

// ValidateCommand validates a command request
func (cv *CommandValidator) ValidateCommand(req *CommandRequest) error {
	// Validate command type
	if err := cv.validateCommandType(req.Type); err != nil {
		return err
	}

	// Validate payload size
	if len(req.Payload) > cv.maxPayloadSize {
		return ValidationError{
			Field:  "payload",
			Value:  len(req.Payload),
			Reason: fmt.Sprintf("payload size exceeds maximum (%d bytes)", cv.maxPayloadSize),
		}
	}

	// Type-specific validation
	switch req.Type {
	case "ping":
		return cv.validatePingCommand(req.Payload)
	case "set_input_mode":
		return cv.validateInputModeCommand(req.Payload)
	case "mouse_move", "mouse_click", "mouse_scroll":
		return cv.validateMouseCommand(req.Type, req.Payload)
	case "key_press", "key_release":
		return cv.validateKeyboardCommand(req.Type, req.Payload)
	case "revoke_device":
		return cv.validateRevokeDeviceCommand(req.Payload)
	case "terminate_session":
		return cv.validateTerminateSessionCommand(req.Payload)
	default:
		// Allow unknown commands but log them
		return nil
	}
}

// validateCommandType validates the command type string
func (cv *CommandValidator) validateCommandType(cmdType string) error {
	if cmdType == "" {
		return ValidationError{
			Field:  "type",
			Value:  cmdType,
			Reason: "command type cannot be empty",
		}
	}

	// Allow alphanumeric, underscore, and hyphen
	validType := regexp.MustCompile(`^[a-zA-Z0-9_-]+$`)
	if !validType.MatchString(cmdType) {
		return ValidationError{
			Field:  "type",
			Value:  cmdType,
			Reason: "command type contains invalid characters",
		}
	}

	if len(cmdType) > 50 {
		return ValidationError{
			Field:  "type",
			Value:  cmdType,
			Reason: "command type too long",
		}
	}

	return nil
}

// validatePingCommand validates ping commands
func (cv *CommandValidator) validatePingCommand(payload []byte) error {
	// Ping commands should have empty or minimal payload
	if len(payload) > 100 {
		return ValidationError{
			Field:  "payload",
			Value:  len(payload),
			Reason: "ping payload too large",
		}
	}
	return nil
}

// validateInputModeCommand validates input mode commands
func (cv *CommandValidator) validateInputModeCommand(payload []byte) error {
	mode := string(payload)
	validModes := []string{"mouse", "touch", "precision"}

	for _, validMode := range validModes {
		if mode == validMode {
			return nil
		}
	}

	return ValidationError{
		Field:  "payload",
		Value:  mode,
		Reason: "invalid input mode",
	}
}

// validateMouseCommand validates mouse-related commands
func (cv *CommandValidator) validateMouseCommand(cmdType string, payload []byte) error {
	switch cmdType {
	case "mouse_move":
		if len(payload) != 8 { // 2 int32 coordinates
			return ValidationError{
				Field:  "payload",
				Value:  len(payload),
				Reason: "mouse_move requires 8 bytes (2 int32 coordinates)",
			}
		}
		// Could add coordinate range validation here

	case "mouse_click":
		if len(payload) != 5 { // 1 byte button, 4 bytes (int32) state
			return ValidationError{
				Field:  "payload",
				Value:  len(payload),
				Reason: "mouse_click requires 5 bytes (button + state)",
			}
		}
		button := payload[0]
		if button > 2 { // Left, right, middle only
			return ValidationError{
				Field:  "button",
				Value:  button,
				Reason: "invalid mouse button",
			}
		}

	case "mouse_scroll":
		if len(payload) != 4 { // 1 int32 amount
			return ValidationError{
				Field:  "payload",
				Value:  len(payload),
				Reason: "mouse_scroll requires 4 bytes (int32 amount)",
			}
		}
	}

	return nil
}

// validateKeyboardCommand validates keyboard commands
func (cv *CommandValidator) validateKeyboardCommand(cmdType string, payload []byte) error {
	if len(payload) != 4 { // 1 int32 keycode
		return ValidationError{
			Field:  "payload",
			Value:  len(payload),
			Reason: "keyboard commands require 4 bytes (int32 keycode)",
		}
	}

	// Basic keycode range validation (0-255 for common keys, but allow higher for special keys)
	keycode := int(payload[0]) | int(payload[1])<<8 | int(payload[2])<<16 | int(payload[3])<<24
	if keycode < 0 || keycode > 0x10FFFF { // Valid Unicode range plus some extras
		return ValidationError{
			Field:  "keycode",
			Value:  keycode,
			Reason: "keycode out of valid range",
		}
	}

	return nil
}

// validateRevokeDeviceCommand validates device revocation commands
func (cv *CommandValidator) validateRevokeDeviceCommand(payload []byte) error {
	payloadStr := string(payload)

	// Should be a valid device ID (hex string)
	if len(payloadStr) != 64 { // SHA256 hex length
		return ValidationError{
			Field:  "device_id",
			Value:  len(payloadStr),
			Reason: "device ID must be 64 hex characters",
		}
	}

	// Check if it's valid hex
	validHex := regexp.MustCompile(`^[a-fA-F0-9]{64}$`)
	if !validHex.MatchString(payloadStr) {
		return ValidationError{
			Field:  "device_id",
			Value:  payloadStr,
			Reason: "device ID must be valid hexadecimal",
		}
	}

	return nil
}

// validateTerminateSessionCommand validates session termination commands
func (cv *CommandValidator) validateTerminateSessionCommand(payload []byte) error {
	reason := string(payload)

	// Reason should not be too long
	if len(reason) > 200 {
		return ValidationError{
			Field:  "reason",
			Value:  len(reason),
			Reason: "termination reason too long",
		}
	}

	// Basic sanitization - no control characters
	for _, r := range reason {
		if r < 32 && r != 9 && r != 10 && r != 13 { // Allow tab, LF, CR
			return ValidationError{
				Field:  "reason",
				Value:  reason,
				Reason: "termination reason contains invalid characters",
			}
		}
	}

	return nil
}

// SanitizeCommand sanitizes a command for safe processing
func (cv *CommandValidator) SanitizeCommand(req *CommandRequest) *CommandRequest {
	sanitized := &CommandRequest{
		Type:    strings.TrimSpace(req.Type),
		Payload: make([]byte, len(req.Payload)),
	}
	copy(sanitized.Payload, req.Payload)

	// Type normalization
	sanitized.Type = strings.ToLower(sanitized.Type)

	return sanitized
}

// IsAbusiveCommand checks if a command shows signs of abuse
func (cv *CommandValidator) IsAbusiveCommand(req *CommandRequest, recentCommands []*CommandRequest) bool {
	// Check for command flooding (more than 100 commands in last second would be abusive)
	if len(recentCommands) > 100 {
		return true
	}

	// Check for oversized payloads in rapid succession
	oversizedCount := 0
	for _, cmd := range recentCommands {
		if len(cmd.Payload) > cv.maxPayloadSize/2 { // Half of max is still large
			oversizedCount++
		}
	}
	if oversizedCount > 5 {
		return true
	}

	return false
}</content>
<parameter name="filePath">C:\RemoteDexter\src\shared\protocol\validator.go