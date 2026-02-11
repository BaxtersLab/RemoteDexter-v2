package api

import (
	"errors"

	"remotedexter/desktop/bootstrap/bluetooth"
	bootstrappackage "remotedexter/desktop/bootstrap/package"
	"remotedexter/desktop/tunnel"
)

// DefaultAgentAPI provides a simple in-memory implementation.
type DefaultAgentAPI struct {
	bootstrapper bluetooth.BluetoothBootstrapper
	importer     bootstrappackage.BootstrapImporter
	exporter     bootstrappackage.BootstrapExporter
	tunnel       tunnel.TunnelEngine
	events       chan AgentEvent
	status       AgentStatus
	sessionInfo  SessionInfo
}

func (a *DefaultAgentAPI) BootstrapViaBluetooth() error {
	if a.bootstrapper == nil {
		return errors.New("bootstrap: missing bootstrapper")
	}
	a.status = AgentStatus{State: "BOOTSTRAPPING"}
	return nil
}

func (a *DefaultAgentAPI) ImportBootstrapPackage() error {
	if a.importer == nil {
		return errors.New("import: missing importer")
	}
	a.status = AgentStatus{State: "BOOTSTRAPPED"}
	return nil
}

func (a *DefaultAgentAPI) ExportBootstrapPackage() error {
	if a.exporter == nil {
		return errors.New("export: missing exporter")
	}
	return nil
}

func (a *DefaultAgentAPI) StartTunnel() error {
	if a.tunnel == nil {
		return errors.New("start: missing tunnel engine")
	}
	a.status = AgentStatus{State: "CONNECTING"}
	return nil
}

func (a *DefaultAgentAPI) StopTunnel() error {
	if a.tunnel == nil {
		return errors.New("stop: missing tunnel engine")
	}
	a.status = AgentStatus{State: "STOPPED"}
	return nil
}

func (a *DefaultAgentAPI) GetStatus() (AgentStatus, error) {
	return a.status, nil
}

func (a *DefaultAgentAPI) GetSessionInfo() (SessionInfo, error) {
	return a.sessionInfo, nil
}

func (a *DefaultAgentAPI) SubscribeEvents() (<-chan AgentEvent, error) {
	return a.events, nil
}

