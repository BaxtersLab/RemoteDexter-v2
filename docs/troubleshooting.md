# Troubleshooting

## Bluetooth Pairing Issues
- Ensure Bluetooth is enabled on both devices
- Check Windows Bluetooth settings for device visibility
- Restart Bluetooth services if pairing fails

## APK Install Issues
- Enable "Install unknown apps" in Android settings
- Check APK integrity with SHA-256 checksum
- Clear cache if install fails

## Handshake Mismatch Cases
- Verify Noise primitives are correctly implemented
- Check for nonce reuse or replay attacks
- Ensure X25519 keys are valid

## Command Failures
- Confirm session keys are identical
- Check AEAD encryption/decryption
- Verify framing is correct