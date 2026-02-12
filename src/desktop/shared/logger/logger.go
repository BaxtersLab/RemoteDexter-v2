package logger

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"os"
	"regexp"
	"strings"
	"sync"
	"time"
)

// LogLevel represents the severity level of a log entry
type LogLevel int

const (
	LevelDebug LogLevel = iota
	LevelInfo
	LevelWarn
	LevelError
	LevelFatal
)

// String returns the string representation of a log level
func (l LogLevel) String() string {
	switch l {
	case LevelDebug:
		return "DEBUG"
	case LevelInfo:
		return "INFO"
	case LevelWarn:
		return "WARN"
	case LevelError:
		return "ERROR"
	case LevelFatal:
		return "FATAL"
	default:
		return "UNKNOWN"
	}
}

// Field represents a structured logging field
type Field struct {
	Key   string
	Value interface{}
}

// NewField creates a new logging field
func NewField(key string, value interface{}) Field {
	return Field{Key: key, Value: value}
}

// SecureLogger provides secure logging with sensitive data redaction
type SecureLogger struct {
	logger *log.Logger
	file   *os.File
	level  LogLevel
	mu     sync.RWMutex

	// Sensitive data patterns to redact
	sensitivePatterns []*regexp.Regexp
}

// Global logger instance
var globalLogger *SecureLogger
var globalMu sync.RWMutex

// InitSecureLogger initializes the global secure logger
func InitSecureLogger(logFile string) error {
	globalMu.Lock()
	defer globalMu.Unlock()

	if globalLogger != nil {
		return fmt.Errorf("logger already initialized")
	}

	logger, err := NewSecureLogger(logFile, LevelInfo)
	if err != nil {
		return err
	}

	globalLogger = logger
	return nil
}

// NewSecureLogger creates a new secure logger instance
func NewSecureLogger(logFile string, level LogLevel) (*SecureLogger, error) {
	file, err := os.OpenFile(logFile, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0600)
	if err != nil {
		return nil, fmt.Errorf("failed to open log file: %v", err)
	}

	logger := &SecureLogger{
		logger: log.New(file, "", 0), // We'll format timestamps ourselves
		file:   file,
		level:  level,
		sensitivePatterns: []*regexp.Regexp{
			regexp.MustCompile(`(?i)password["\s]*:[\s"]*[^"\s]+`),      // password: value
			regexp.MustCompile(`(?i)token["\s]*:[\s"]*[^"\s]+`),         // token: value
			regexp.MustCompile(`(?i)key["\s]*:[\s"]*[^"\s]+`),           // key: value
			regexp.MustCompile(`(?i)secret["\s]*:[\s"]*[^"\s]+`),        // secret: value
			regexp.MustCompile(`(?i)private["\s]*:[\s"]*[^"\s]+`),       // private: value
			regexp.MustCompile(`(?i)session["\s]*:[\s"]*[^"\s]+`),       // session: value
			regexp.MustCompile(`(?i)auth["\s]*:[\s"]*[^"\s]+`),          // auth: value
			regexp.MustCompile(`(?i)credential["\s]*:[\s"]*[^"\s]+`),    // credential: value
			regexp.MustCompile(`(?i)apikey["\s]*:[\s"]*[^"\s]+`),        // apikey: value
			regexp.MustCompile(`(?i)apikey["\s]*:[\s"]*[^"\s]+`),        // apikey: value
			regexp.MustCompile(`(?i)x-api-key["\s]*:[\s"]*[^"\s]+`),     // x-api-key: value
			regexp.MustCompile(`(?i)authorization["\s]*:[\s"]*[^"\s]+`), // authorization: value
			regexp.MustCompile(`(?i)bearer[\s]+[^\s]+`),                 // bearer token
			regexp.MustCompile(`(?i)basic[\s]+[^\s]+`),                  // basic auth
		},
	}

	return logger, nil
}

// Close closes the logger and its file
func (sl *SecureLogger) Close() error {
	sl.mu.Lock()
	defer sl.mu.Unlock()

	if sl.file != nil {
		return sl.file.Close()
	}
	return nil
}

// SetLevel sets the minimum log level
func (sl *SecureLogger) SetLevel(level LogLevel) {
	sl.mu.Lock()
	defer sl.mu.Unlock()
	sl.level = level
}

// log writes a log entry with the specified level
func (sl *SecureLogger) log(level LogLevel, message string, fields ...Field) {
	sl.mu.RLock()
	if level < sl.level {
		sl.mu.RUnlock()
		return
	}
	sl.mu.RUnlock()

	sl.mu.Lock()
	defer sl.mu.Unlock()

	// Create log entry
	entry := map[string]interface{}{
		"timestamp": time.Now().UTC().Format(time.RFC3339),
		"level":     level.String(),
		"message":   sl.redactSensitiveData(message),
	}

	// Add fields
	if len(fields) > 0 {
		entryFields := make(map[string]interface{})
		for _, field := range fields {
			key := sl.redactSensitiveData(field.Key)
			value := sl.redactSensitiveData(fmt.Sprintf("%v", field.Value))
			entryFields[key] = value
		}
		entry["fields"] = entryFields
	}

	// Serialize to JSON
	jsonData, err := json.Marshal(entry)
	if err != nil {
		// Fallback to plain text if JSON fails
		fmt.Fprintf(os.Stderr, "Logger error: %v\n", err)
		sl.logger.Printf("[%s] %s %s", level.String(), time.Now().Format(time.RFC3339), message)
		return
	}

	sl.logger.Println(string(jsonData))
}

// redactSensitiveData removes or masks sensitive information from strings
func (sl *SecureLogger) redactSensitiveData(input string) string {
	if input == "" {
		return input
	}

	result := input

	// Apply all sensitive data patterns
	for _, pattern := range sl.sensitivePatterns {
		result = pattern.ReplaceAllStringFunc(result, func(match string) string {
			// Replace the value part with [REDACTED]
			parts := strings.SplitN(match, ":", 2)
			if len(parts) == 2 {
				return strings.TrimSpace(parts[0]) + ": [REDACTED]"
			}
			// For patterns without colon, replace the entire match
			return "[REDACTED]"
		})
	}

	return result
}

// Debug logs a debug message
func (sl *SecureLogger) Debug(message string, fields ...Field) {
	sl.log(LevelDebug, message, fields...)
}

// Info logs an info message
func (sl *SecureLogger) Info(message string, fields ...Field) {
	sl.log(LevelInfo, message, fields...)
}

// Warn logs a warning message
func (sl *SecureLogger) Warn(message string, fields ...Field) {
	sl.log(LevelWarn, message, fields...)
}

// Error logs an error message
func (sl *SecureLogger) Error(message string, fields ...Field) {
	sl.log(LevelError, message, fields...)
}

// Fatal logs a fatal message and exits
func (sl *SecureLogger) Fatal(message string, fields ...Field) {
	sl.log(LevelFatal, message, fields...)
	os.Exit(1)
}

// Global logging functions

// Debug logs a debug message using the global logger
func Debug(message string, fields ...Field) {
	globalMu.RLock()
	if globalLogger != nil {
		globalLogger.Debug(message, fields...)
	}
	globalMu.RUnlock()
}

// Info logs an info message using the global logger
func Info(message string, fields ...Field) {
	globalMu.RLock()
	if globalLogger != nil {
		globalLogger.Info(message, fields...)
	}
	globalMu.RUnlock()
}

// Warn logs a warning message using the global logger
func Warn(message string, fields ...Field) {
	globalMu.RLock()
	if globalLogger != nil {
		globalLogger.Warn(message, fields...)
	}
	globalMu.RUnlock()
}

// Error logs an error message using the global logger
func Error(message string, fields ...Field) {
	globalMu.RLock()
	if globalLogger != nil {
		globalLogger.Error(message, fields...)
	}
	globalMu.RUnlock()
}

// Fatal logs a fatal message using the global logger and exits
func Fatal(message string, fields ...Field) {
	globalMu.RLock()
	if globalLogger != nil {
		globalLogger.Fatal(message, fields...)
	}
	globalMu.RUnlock()
	os.Exit(1)
}

// GetLogger returns the global logger instance
func GetLogger() *SecureLogger {
	globalMu.RLock()
	defer globalMu.RUnlock()
	return globalLogger
}

// SetOutput sets the output destination for the logger
func (sl *SecureLogger) SetOutput(w io.Writer) {
	sl.mu.Lock()
	defer sl.mu.Unlock()
	sl.logger.SetOutput(w)
}
