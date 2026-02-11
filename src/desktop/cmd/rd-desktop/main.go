package main

import (
	"fmt"
	"remotedexter/desktop/internal/ui"
)

func main() {
	fmt.Println("RD Desktop Application starting...")
	console := ui.NewConsole()
	console.Run()
}
