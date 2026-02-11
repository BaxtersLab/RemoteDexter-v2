package bluetooth

type Device struct {
	Name string
	MAC  string
}

type Discovery struct{}

func NewDiscovery() *Discovery {
	return &Discovery{}
}

func (d *Discovery) DiscoverPairedDevices() ([]Device, error) {
	// Placeholder: simulate paired device
	return []Device{{Name: "Test Android Device", MAC: "00:11:22:33:44:55"}}, nil
}
