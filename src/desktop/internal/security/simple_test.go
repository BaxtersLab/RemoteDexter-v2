package security

import "testing"

func TestSimple(t *testing.T) {
	// Simple test to check if testing framework works
	if 1+1 != 2 {
		t.Error("Math is broken")
	}
}
