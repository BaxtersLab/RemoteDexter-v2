package session

import (
	"sync"
	"time"
)

// RemoteSessionState represents the current state of a remote session
type RemoteSessionState struct {
	IsConnected      bool
	TransportType    string
	IsStreaming      bool
	IsInputEnabled   bool
	LatencyMs        int
	SessionStartTime time.Time
	LastActivityTime time.Time
	ErrorCount       int
	mu               sync.RWMutex
}

// NewSessionState creates a new session state
func NewSessionState() *RemoteSessionState {
	return &RemoteSessionState{
		IsConnected:      false,
		TransportType:    "",
		IsStreaming:      false,
		IsInputEnabled:   false,
		LatencyMs:        0,
		SessionStartTime: time.Now(),
		LastActivityTime: time.Now(),
		ErrorCount:       0,
	}
}

// UpdateTransport updates the transport type
func (rss *RemoteSessionState) UpdateTransport(transportType string) {
	rss.mu.Lock()
	defer rss.mu.Unlock()
	rss.TransportType = transportType
	rss.LastActivityTime = time.Now()
}

// SetConnected sets the connection state
func (rss *RemoteSessionState) SetConnected(connected bool) {
	rss.mu.Lock()
	defer rss.mu.Unlock()
	rss.IsConnected = connected
	if connected {
		rss.SessionStartTime = time.Now()
	}
	rss.LastActivityTime = time.Now()
}

// SetStreaming sets the streaming state
func (rss *RemoteSessionState) SetStreaming(streaming bool) {
	rss.mu.Lock()
	defer rss.mu.Unlock()
	rss.IsStreaming = streaming
	rss.LastActivityTime = time.Now()
}

// SetInputEnabled sets the input control state
func (rss *RemoteSessionState) SetInputEnabled(enabled bool) {
	rss.mu.Lock()
	defer rss.mu.Unlock()
	rss.IsInputEnabled = enabled
	rss.LastActivityTime = time.Now()
}

// UpdateLatency updates the measured latency
func (rss *RemoteSessionState) UpdateLatency(latencyMs int) {
	rss.mu.Lock()
	defer rss.mu.Unlock()
	rss.LatencyMs = latencyMs
	rss.LastActivityTime = time.Now()
}

// IncrementError increments the error count
func (rss *RemoteSessionState) IncrementError() {
	rss.mu.Lock()
	defer rss.mu.Unlock()
	rss.ErrorCount++
	rss.LastActivityTime = time.Now()
}

// ResetErrors resets the error count
func (rss *RemoteSessionState) ResetErrors() {
	rss.mu.Lock()
	defer rss.mu.Unlock()
	rss.ErrorCount = 0
}

// GetState returns a copy of the current state (thread-safe)
func (rss *RemoteSessionState) GetState() RemoteSessionState {
	rss.mu.RLock()
	defer rss.mu.RUnlock()
	return RemoteSessionState{
		IsConnected:      rss.IsConnected,
		TransportType:    rss.TransportType,
		IsStreaming:      rss.IsStreaming,
		IsInputEnabled:   rss.IsInputEnabled,
		LatencyMs:        rss.LatencyMs,
		SessionStartTime: rss.SessionStartTime,
		LastActivityTime: rss.LastActivityTime,
		ErrorCount:       rss.ErrorCount,
	}
}

// IsHealthy checks if the session is in a healthy state
func (rss *RemoteSessionState) IsHealthy() bool {
	rss.mu.RLock()
	defer rss.mu.RUnlock()

	// Session is unhealthy if:
	// - Not connected
	// - Too many errors (>5)
	// - No activity for more than 30 seconds
	if !rss.IsConnected || rss.ErrorCount > 5 {
		return false
	}

	if time.Since(rss.LastActivityTime) > 30*time.Second {
		return false
	}

	return true
}

// GetSessionDuration returns the session duration
func (rss *RemoteSessionState) GetSessionDuration() time.Duration {
	rss.mu.RLock()
	defer rss.mu.RUnlock()
	if !rss.IsConnected {
		return 0
	}
	return time.Since(rss.SessionStartTime)
}
