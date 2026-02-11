package api

import "remotedexter/desktop/internal/core/types"

// AgentStatus describes current agent status.
type AgentStatus struct {
	State string `json:"state"`
}

// SessionInfo exposes current session metadata.
type SessionInfo struct {
	Metadata types.SessionMetadata `json:"metadata"`
	NAT      types.NATFingerprint  `json:"nat_fingerprint"`
}

// AgentEventType enumerates agent event types.
type AgentEventType string

const (
	EventBootstrapComplete  AgentEventType = "on_bootstrap_complete"
	EventGloveTap           AgentEventType = "on_glove_tap"
	EventTunnelEstablished  AgentEventType = "on_tunnel_established"
	EventTunnelLost         AgentEventType = "on_tunnel_lost"
	EventRelayFallback      AgentEventType = "on_relay_fallback"
	EventDirectPathRestored AgentEventType = "on_direct_path_restored"
)

// AgentEvent represents a user-visible agent event.
type AgentEvent struct {
	Type AgentEventType    `json:"type"`
	Data map[string]string `json:"data"`
}

// AgentAPI is the external interface layer.
type AgentAPI interface {
	BootstrapViaBluetooth() error
	ImportBootstrapPackage() error
	ExportBootstrapPackage() error
	StartTunnel() error
	StopTunnel() error
	GetStatus() (AgentStatus, error)
	GetSessionInfo() (SessionInfo, error)
	SubscribeEvents() (<-chan AgentEvent, error)
}

