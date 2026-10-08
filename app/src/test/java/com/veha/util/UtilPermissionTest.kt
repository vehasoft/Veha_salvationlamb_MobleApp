package com.veha.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression tests for `Util.hasPermission()` — the `G13` fail-open defect (task T-025).
 *
 * Before the fix the method read:
 *
 * ```java
 * if (permissionMap == null || permissionMap.isEmpty()) {
 *     return true;          // <-- every one of the 34 gates granted access
 * }
 * ```
 *
 * so a failed `GET /api/v1/permission/users/{userId}` at splash silently promoted every
 * user to full access. These tests pin the fail-CLOSED behaviour.
 *
 * Requires `testOptions { unitTests.returnDefaultValues = true }` because the deny path
 * calls `android.util.Log.w`, which is an unimplemented stub on the JVM.
 */
class UtilPermissionTest {

    @Before
    fun resetState() {
        Util.clearPermissions()
    }

    // ---------------------------------------------------------------- G13: fail closed

    @Test
    fun `denies when permissions were never loaded`() {
        assertFalse(
            "a fresh process must not grant anything before the splash fetch succeeds",
            Util.hasPermission(PermissionType.POST.value, Permission.READ.value)
        )
    }

    @Test
    fun `denies when the fetch failed and the map is still empty`() {
        Util.clearPermissions()
        assertFalse(Util.hasPermission(PermissionType.USER.value, Permission.EDIT.value))
    }

    @Test
    fun `denies after the session is cleared`() {
        Util.setPermissionMap(mutableMapOf(PermissionType.POST.value to "Read"))
        assertTrue(Util.hasPermission(PermissionType.POST.value, Permission.READ.value))

        Util.clearPermissions()
        assertFalse(
            "logout must revoke in-memory permissions",
            Util.hasPermission(PermissionType.POST.value, Permission.READ.value)
        )
    }

    @Test
    fun `an explicitly empty map from the server grants nothing`() {
        Util.setPermissionMap(mutableMapOf())
        assertFalse(Util.hasPermission(PermissionType.POST.value, Permission.READ.value))
    }

    // ------------------------------------------------------- normal granting behaviour

    @Test
    fun `grants a permission that is present in the csv list`() {
        Util.setPermissionMap(mutableMapOf(PermissionType.POST.value to "Read,Edit,Create"))

        assertTrue(Util.hasPermission(PermissionType.POST.value, Permission.READ.value))
        assertTrue(Util.hasPermission(PermissionType.POST.value, Permission.EDIT.value))
        assertTrue(Util.hasPermission(PermissionType.POST.value, Permission.CREATE.value))
    }

    @Test
    fun `denies a permission missing from the csv list`() {
        Util.setPermissionMap(mutableMapOf(PermissionType.POST.value to "Read,Edit"))

        assertFalse(Util.hasPermission(PermissionType.POST.value, Permission.DELETE.value))
    }

    @Test
    fun `the All wildcard grants every permission for that type`() {
        Util.setPermissionMap(mutableMapOf(PermissionType.POST.value to "All"))

        assertTrue(Util.hasPermission(PermissionType.POST.value, Permission.READ.value))
        assertTrue(Util.hasPermission(PermissionType.POST.value, Permission.DELETE.value))
    }

    @Test
    fun `denies an unknown permission type even when other types are granted`() {
        Util.setPermissionMap(mutableMapOf(PermissionType.POST.value to "All"))

        assertFalse(
            "a type absent from the map must not inherit another type's grants",
            Util.hasPermission(PermissionType.ANNOUNCEMENT.value, Permission.READ.value)
        )
    }

    @Test
    fun `a null csv value denies instead of throwing`() {
        val map = HashMap<String, String?>()
        map[PermissionType.POST.value] = null

        @Suppress("UNCHECKED_CAST")
        Util.setPermissionMap(map as MutableMap<String, String>)

        assertFalse(Util.hasPermission(PermissionType.POST.value, Permission.READ.value))
    }

    // ------------------------------------------------------------------- loaded marker

    @Test
    fun `isPermissionsLoaded tracks the real bootstrap state`() {
        assertFalse("not loaded before any fetch", Util.isPermissionsLoaded())

        Util.setPermissionMap(mutableMapOf(PermissionType.POST.value to "Read"))
        assertTrue("loaded after a successful 200", Util.isPermissionsLoaded())

        Util.clearPermissions()
        assertFalse("cleared on logout / failed bootstrap", Util.isPermissionsLoaded())
    }
}
