package ui

import (
	"bufio"
	"fmt"
	"os"
	"remotedexter/desktop/internal/bluetooth"
	"remotedexter/desktop/internal/noise"
	"remotedexter/desktop/internal/streaming"
	"remotedexter/desktop/internal/transport"
	"remotedexter/desktop/shared/protocol"
	"strings"
)

type Console struct {
	discovery  *bluetooth.Discovery
	handshake  *noise.Handshake
	selector   *transport.Selector
	streaming  *streaming.StreamingSession
	sessionKey []byte
	nonce      uint64
	debug      bool
}

func NewConsole() *Console {
	return &Console{
		discovery: bluetooth.NewDiscovery(),
		handshake: noise.NewHandshake(),
		selector:  transport.NewSelector(),
		streaming: streaming.NewStreamingSession(),
	}
}

func (c *Console) Run() {
	scanner := bufio.NewScanner(os.Stdin)
	for {
		c.showMenu()
		if !scanner.Scan() {
			break
		}
		choice := strings.TrimSpace(scanner.Text())
		c.handleChoice(choice)
	}
}

func (c *Console) showMenu() {
	fmt.Println("\nRD Desktop Console")
	fmt.Println("1: Discover devices")
	fmt.Println("2: Bootstrap Android")
	fmt.Println("3: Perform handshake")
	fmt.Println("4: Send ping")
	fmt.Println("5: Start screen streaming")
	fmt.Println("6: Stop screen streaming")
	fmt.Println("7: Toggle mouse mode")
	fmt.Println("8: Toggle touch mode")
	fmt.Println("9: Toggle precision mode")
	fmt.Println("10: Exit")
	fmt.Print("Choice: ")
}

func (c *Console) handleChoice(choice string) {
	switch choice {
	case "1":
		c.discoverDevices()
	case "2":
		c.bootstrapAndroid()
	case "3":
		c.performHandshake()
	case "4":
		c.sendPing()
	case "5":
		c.startStreaming()
	case "6":
		c.stopStreaming()
	case "7":
		c.setInputMode("mouse")
	case "8":
		c.setInputMode("touch")
	case "9":
		c.setInputMode("precision")
	case "10":
		os.Exit(0)
	default:
		fmt.Println("Invalid choice")
	}
}

func (c *Console) discoverDevices() {
	fmt.Println("UI: Discovering devices...")
	devices, err := c.discovery.DiscoverPairedDevices()
	if err != nil {
		fmt.Printf("Discovery failed: %v\n", err)
	} else {
		fmt.Println("Discovery OK")
		for _, dev := range devices {
			fmt.Printf("Device: %s (%s)\n", dev.Name, dev.MAC)
		}
	}
}

func (c *Console) bootstrapAndroid() {
	fmt.Println("UI: Bootstrap initiated")
	// Placeholder for bootstrap
	fmt.Println("Bootstrap OK")
}

func (c *Console) performHandshake() {
	fmt.Println("UI: Handshake initiated")
	key, err := c.handshake.PerformNoiseHandshake()
	if err != nil {
		fmt.Printf("Handshake failed: %v\n", err)
	} else {
		c.sessionKey = key
		fmt.Println("Handshake OK")
	}
}

func (c *Console) sendPing() {
	if c.sessionKey == nil {
		fmt.Println("No session key, perform handshake first")
		return
	}
	fmt.Println("UI: Command sent")
	req := protocol.CommandRequest{Type: "ping", Payload: []byte{}}
	resp, err := c.selector.SendCommand(req, c.sessionKey, &c.nonce)
	if err != nil {
		fmt.Printf("Command failed: %v\n", err)
	} else {
		fmt.Printf("ping → %s\n", string(resp.Payload))
	}
}

func (c *Console) startStreaming() {
	if c.sessionKey == nil {
		fmt.Println("No session key, perform handshake first")
		return
	}

	fmt.Println("UI: Starting screen streaming")
	c.streaming.SetSessionKeys(c.sessionKey, c.nonce)
	if err := c.streaming.StartStreaming(); err != nil {
		fmt.Printf("Streaming start failed: %v\n", err)
	} else {
		fmt.Println("Streaming started successfully")
	}
}

func (c *Console) stopStreaming() {
	fmt.Println("UI: Stopping screen streaming")
	if err := c.streaming.StopStreaming(); err != nil {
		fmt.Printf("Streaming stop failed: %v\n", err)
	} else {
		fmt.Println("Streaming stopped successfully")
	}
}

func (c *Console) setInputMode(mode string) {
	if c.sessionKey == nil {
		fmt.Println("No session key, perform handshake first")
		return
	}

	fmt.Printf("UI: Setting input mode to %s\n", mode)

	req := protocol.CommandRequest{
		Type:    "set_input_mode",
		Payload: []byte(mode),
	}

	resp, err := c.selector.SendCommand(req, c.sessionKey, &c.nonce)
	if err != nil {
		fmt.Printf("Set input mode failed: %v\n", err)
	} else {
		fmt.Printf("Input mode set to %s: %s\n", mode, resp.Status)
	}
}
