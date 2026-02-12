package com.remotedexter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TrustSettingsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var lostDeviceButton: Button
    private lateinit var deviceAdapter: DeviceAdapter

    // Mock data - in real app, this would come from a repository
    private val trustedDevices = mutableListOf<TrustedDevice>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Create layout programmatically
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val titleText = TextView(requireContext()).apply {
            text = "Trusted Devices"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }

        recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
        }

        emptyText = TextView(requireContext()).apply {
            text = "No trusted devices found.\n\nPair a device first to start using RemoteDexter."
            textSize = 16f
            setPadding(0, 32, 0, 32)
            visibility = View.GONE
        }

        lostDeviceButton = Button(requireContext()).apply {
            text = "Lost Device Protocol"
            setOnClickListener { showLostDeviceDialog() }
            setPadding(32, 16, 32, 16)
        }

        layout.addView(titleText)
        layout.addView(recyclerView)
        layout.addView(emptyText)
        layout.addView(lostDeviceButton)

        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deviceAdapter = DeviceAdapter(trustedDevices) { device, action ->
            when (action) {
                "rename" -> renameDevice(device)
                "revoke" -> revokeDevice(device)
            }
        }
        recyclerView.adapter = deviceAdapter

        loadTrustedDevices()
    }

    private fun loadTrustedDevices() {
        // Mock loading - in real app, load from secure storage
        trustedDevices.clear()
        // Add some mock devices for demonstration
        trustedDevices.add(TrustedDevice("1", "Pixel 7", "Online", "2 minutes ago"))
        trustedDevices.add(TrustedDevice("2", "Galaxy S23", "Offline", "3 days ago"))

        if (trustedDevices.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
            deviceAdapter.notifyDataSetChanged()
        }
    }

    private fun renameDevice(device: TrustedDevice) {
        // Show rename dialog
        val editText = android.widget.EditText(requireContext()).apply {
            setText(device.name)
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Rename Device")
            .setView(editText)
            .setPositiveButton("Rename") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    device.name = newName
                    deviceAdapter.notifyDataSetChanged()
                    Toast.makeText(requireContext(), "Device renamed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun revokeDevice(device: TrustedDevice) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Revoke Device")
            .setMessage("Are you sure you want to revoke '${device.name}'? This will remove it from your trusted devices and terminate any active sessions.")
            .setPositiveButton("Revoke") { _, _ ->
                trustedDevices.remove(device)
                deviceAdapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Device revoked", Toast.LENGTH_SHORT).show()

                if (trustedDevices.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyText.visibility = View.VISIBLE
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLostDeviceDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Lost Device Protocol")
            .setMessage("""
                WARNING: This will revoke ALL trusted devices and invalidate ALL pairings.

                You will need to re-pair all your devices after this operation.

                This should only be used if:
                • Your device was lost or stolen
                • You suspect your pairings have been compromised

                Type 'LOST' to confirm:
            """.trimIndent())
            .setView(android.widget.EditText(requireContext()).apply {
                hint = "Type 'LOST' to confirm"
            })
            .setPositiveButton("Execute") { dialog, _ ->
                val input = (dialog as android.app.AlertDialog).findViewById<android.widget.EditText>(android.R.id.edit)
                if (input?.text.toString() == "LOST") {
                    executeLostDeviceProtocol()
                } else {
                    Toast.makeText(requireContext(), "Confirmation failed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeLostDeviceProtocol() {
        // Clear all trusted devices
        trustedDevices.clear()
        deviceAdapter.notifyDataSetChanged()

        recyclerView.visibility = View.GONE
        emptyText.visibility = View.VISIBLE

        Toast.makeText(requireContext(), "Lost Device Protocol executed. All pairings cleared.", Toast.LENGTH_LONG).show()
    }
}

// Data class for trusted device
data class TrustedDevice(
    val id: String,
    var name: String,
    val status: String,
    val lastSeen: String
)

// RecyclerView adapter for devices
class DeviceAdapter(
    private val devices: List<TrustedDevice>,
    private val onAction: (TrustedDevice, String) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val layout = android.widget.LinearLayout(parent.context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        return DeviceViewHolder(layout)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device, onAction)
    }

    override fun getItemCount() = devices.size

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layout = itemView as android.widget.LinearLayout

        fun bind(device: TrustedDevice, onAction: (TrustedDevice, String) -> Unit) {
            layout.removeAllViews()

            val nameText = TextView(layout.context).apply {
                text = device.name
                textSize = 18f
                setPadding(0, 0, 0, 8)
            }

            val statusText = TextView(layout.context).apply {
                text = "Status: ${device.status} • Last seen: ${device.lastSeen}"
                textSize = 14f
                setPadding(0, 0, 0, 16)
            }

            val buttonLayout = android.widget.LinearLayout(layout.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
            }

            val renameButton = Button(layout.context).apply {
                text = "Rename"
                setOnClickListener { onAction(device, "rename") }
                layoutParams = android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val revokeButton = Button(layout.context).apply {
                text = "Revoke"
                setOnClickListener { onAction(device, "revoke") }
                layoutParams = android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            buttonLayout.addView(renameButton)
            buttonLayout.addView(revokeButton)

            layout.addView(nameText)
            layout.addView(statusText)
            layout.addView(buttonLayout)
        }
    }
}