package main

import (
	"fmt"
	"os"
	"path/filepath"
	"remotedexter/desktop/internal/ui"
	"remotedexter/desktop/shared/logger"
	_ "remotedexter/desktop/shared/protocol" // ensure protocol package is built
)

func main() {
	// Initialize secure logger first
	logFile := filepath.Join(os.Getenv("APPDATA"), "RemoteDexter", "rd-desktop.log")
	if err := logger.InitSecureLogger(logFile); err != nil {
		fmt.Printf("Warning: Failed to initialize secure logger: %v\n", err)
		// Continue without logging rather than fail
	}

	logger.Info("RD Desktop Application starting...",
		logger.NewField("version", "1.0.0"),
		logger.NewField("data_dir", filepath.Join(os.Getenv("APPDATA"), "RemoteDexter")))

	// Create data directory if it doesn't exist
	dataDir := filepath.Join(os.Getenv("APPDATA"), "RemoteDexter")
	if err := os.MkdirAll(dataDir, 0755); err != nil {
		logger.Error("Failed to create data directory", logger.NewField("error", err))
		fmt.Printf("Warning: Failed to create data directory: %v\n", err)
		dataDir = "" // Fallback to current directory
	}

	console := ui.NewConsole(dataDir)
	console.Run()

	logger.Info("RD Desktop Application shutting down")
}
