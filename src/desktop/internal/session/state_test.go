package session

import (
	"testing"
	"time"
)

func TestNewSessionState(t *testing.T) {
	state := NewSessionState()

	if state.IsConnected {
		t.Error("Expected IsConnected to be false initially")
	}
	if state.TransportType != "" {
		t.Error("Expected TransportType to be empty initially")
	}
	if state.IsStreaming {
		t.Error("Expected IsStreaming to be false initially")
	}
	if state.IsInputEnabled {
		t.Error("Expected IsInputEnabled to be false initially")
	}
	if state.ErrorCount != 0 {
		t.Error("Expected ErrorCount to be 0 initially")
	}
}

func TestSessionStateOperations(t *testing.T) {
	state := NewSessionState()

	// Test transport update
	state.UpdateTransport("bluetooth")
	if state.TransportType != "bluetooth" {
		t.Errorf("Expected TransportType to be 'bluetooth', got '%s'", state.TransportType)
	}

	// Test connection state
	state.SetConnected(true)
	if !state.IsConnected {
		t.Error("Expected IsConnected to be true")
	}

	// Test streaming state
	state.SetStreaming(true)
	if !state.IsStreaming {
		t.Error("Expected IsStreaming to be true")
	}

	// Test input state
	state.SetInputEnabled(true)
	if !state.IsInputEnabled {
		t.Error("Expected IsInputEnabled to be true")
	}

	// Test error increment
	state.IncrementError()
	if state.ErrorCount != 1 {
		t.Errorf("Expected ErrorCount to be 1, got %d", state.ErrorCount)
	}

	// Test error reset
	state.ResetErrors()
	if state.ErrorCount != 0 {
		t.Errorf("Expected ErrorCount to be 0 after reset, got %d", state.ErrorCount)
	}
}

func TestSessionStateHealth(t *testing.T) {
	state := NewSessionState()

	// Initially not healthy (not connected)
	if state.IsHealthy() {
		t.Error("Expected session to be unhealthy when not connected")
	}

	// Make connected but with errors
	state.SetConnected(true)
	state.IncrementError()
	state.IncrementError()
	state.IncrementError()
	state.IncrementError()
	state.IncrementError()
	state.IncrementError() // 6 errors

	if state.IsHealthy() {
		t.Error("Expected session to be unhealthy with too many errors")
	}

	// Reset errors and test healthy state
	state.ResetErrors()
	if !state.IsHealthy() {
		t.Error("Expected session to be healthy when connected and no errors")
	}

	// Test inactivity timeout (simulate old activity time)
	state.LastActivityTime = time.Now().Add(-31 * time.Second)
	if state.IsHealthy() {
		t.Error("Expected session to be unhealthy when inactive for too long")
	}
}

func TestSessionControllerCreation(t *testing.T) {
	controller, err := NewSessionController(t.TempDir())
	if err != nil {
		t.Fatalf("Expected SessionController creation to succeed: %v", err)
	}

	if controller == nil {
		t.Error("Expected SessionController to be created")
	}

	if controller.isActive {
		t.Error("Expected controller to not be active initially")
	}
}

func TestSessionControllerState(t *testing.T) {
	controller, err := NewSessionController(t.TempDir())
	if err != nil {
		t.Fatalf("Expected SessionController creation to succeed: %v", err)
	}

	// Test initial state
	state := controller.GetState()
	if state.IsConnected {
		t.Error("Expected initial state to not be connected")
	}

	if !controller.IsHealthy() {
		t.Error("Expected controller to be healthy initially")
	}
}
