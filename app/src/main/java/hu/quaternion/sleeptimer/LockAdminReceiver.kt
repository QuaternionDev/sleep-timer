package hu.quaternion.sleeptimer

import android.app.admin.DeviceAdminReceiver

/**
 * Device-admin hook used only so the app may call [android.app.admin.DevicePolicyManager.lockNow]
 * when the timer ends. The policy set declared in res/xml/device_admin.xml is force-lock and
 * nothing else, so this grants no other control over the device.
 */
class LockAdminReceiver : DeviceAdminReceiver()
