package shared

import (
	"fmt"
	"log"
	"os"
	"strings"
)

// LogLevel represents the severity level of log messages
type LogLevel int

const (
	ERROR LogLevel = iota
	WARN
	INFO
	DEBUG
)

// Logger provides secure logging functionality
type Logger struct {
	level  LogLevel
	logger *log.Logger
}

// NewLogger creates a new secure logger
func NewLogger(level LogLevel) *Logger {
	return &Logger{
		level:  level,
		logger: log.New(os.Stderr, "", log.LstdFlags),
	}
}

// SetLevel sets the minimum log level
func (l *Logger) SetLevel(level LogLevel) {
	l.level = level
}

// Error logs an error message
func (l *Logger) Error(format string, args ...interface{}) {
	if l.level >= ERROR {
		l.log("ERROR", format, args...)
	}
}

// Warn logs a warning message
func (l *Logger) Warn(format string, args ...interface{}) {
	if l.level >= WARN {
		l.log("WARN", format, args...)
	}
}

// Info logs an info message
func (l *Logger) Info(format string, args ...interface{}) {
	if l.level >= INFO {
		l.log("INFO", format, args...)
	}
}

// Debug logs a debug message
func (l *Logger) Debug(format string, args ...interface{}) {
	if l.level >= DEBUG {
		l.log("DEBUG", format, args...)
	}
}

// log is the internal logging function that sanitizes output
func (l *Logger) log(level, format string, args ...interface{}) {
	// Sanitize the message to remove sensitive information
	message := fmt.Sprintf(format, args...)
	sanitized := l.sanitizeMessage(message)

	l.logger.Printf("[%s] %s", level, sanitized)
}

// sanitizeMessage removes or redacts sensitive information from log messages
func (l *Logger) sanitizeMessage(message string) string {
	// Redact cryptographic keys (hex strings of typical key lengths)
	message = l.redactHexKeys(message, 32) // 32 bytes = 64 hex chars
	message = l.redactHexKeys(message, 64) // 64 bytes = 128 hex chars

	// Redact base64-like strings that might be keys
	message = l.redactBase64Keys(message)

	// Redact session keys specifically
	message = strings.ReplaceAll(message, "session_key", "[REDACTED]")
	message = strings.ReplaceAll(message, "sessionKey", "[REDACTED]")

	// Redact private keys
	message = strings.ReplaceAll(message, "private_key", "[REDACTED]")
	message = strings.ReplaceAll(message, "privateKey", "[REDACTED]")

	// Redact passwords and tokens
	message = strings.ReplaceAll(message, "password", "[REDACTED]")
	message = strings.ReplaceAll(message, "token", "[REDACTED]")

	return message
}

// redactHexKeys redacts hexadecimal strings of a specific byte length
func (l *Logger) redactHexKeys(message string, byteLength int) string {
	hexLength := byteLength * 2

	// Simple regex replacement (not using regexp for security)
	words := strings.Fields(message)
	for i, word := range words {
		if len(word) == hexLength && l.isHexString(word) {
			words[i] = "[REDACTED_KEY]"
		}
	}

	return strings.Join(words, " ")
}

// isHexString checks if a string contains only hexadecimal characters
func (l *Logger) isHexString(s string) bool {
	for _, r := range s {
		if !((r >= '0' && r <= '9') || (r >= 'a' && r <= 'f') || (r >= 'A' && r <= 'F')) {
			return false
		}
	}
	return true
}

// redactBase64Keys redacts base64-like strings that might be keys
func (l *Logger) redactBase64Keys(message string) string {
	words := strings.Fields(message)
	for i, word := range words {
		// Look for base64-like strings (multiples of 4 chars, valid base64 alphabet)
		if len(word) >= 16 && len(word)%4 == 0 && l.isBase64String(word) {
			words[i] = "[REDACTED_KEY]"
		}
	}
	return strings.Join(words, " ")
}

// isBase64String checks if a string looks like base64
func (l *Logger) isBase64String(s string) bool {
	validChars := "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
	for _, r := range s {
		if !strings.ContainsRune(validChars, r) {
			return false
		}
	}
	return true
}

// SecurePrintf provides a secure alternative to fmt.Printf for logging
func (l *Logger) SecurePrintf(format string, args ...interface{}) {
	message := fmt.Sprintf(format, args...)
	sanitized := l.sanitizeMessage(message)
	l.logger.Print(sanitized)
}

// Event logs a security event with structured information
func (l *Logger) Event(eventType, description string, metadata map[string]interface{}) {
	if l.level >= INFO {
		// Sanitize metadata values
		sanitizedMeta := make(map[string]string)
		for k, v := range metadata {
			sanitizedMeta[k] = l.sanitizeValue(fmt.Sprintf("%v", v))
		}

		l.logger.Printf("[EVENT] %s: %s %v", eventType, description, sanitizedMeta)
	}
}

// sanitizeValue sanitizes individual values for logging
func (l *Logger) sanitizeValue(value string) string {
	if len(value) > 100 {
		return "[TRUNCATED]"
	}
	return l.sanitizeMessage(value)
}

// Global logger instance
var DefaultLogger = NewLogger(INFO)

// SetGlobalLevel sets the global logger level
func SetGlobalLevel(level LogLevel) {
	DefaultLogger.SetLevel(level)
}

// Convenience functions for global logging
func LogError(format string, args ...interface{}) {
	DefaultLogger.Error(format, args...)
}

func LogWarn(format string, args ...interface{}) {
	DefaultLogger.Warn(format, args...)
}

func LogInfo(format string, args ...interface{}) {
	DefaultLogger.Info(format, args...)
}

func LogDebug(format string, args ...interface{}) {
	DefaultLogger.Debug(format, args...)
}

func LogEvent(eventType, description string, metadata map[string]interface{}) {
	DefaultLogger.Event(eventType, description, metadata)
}
