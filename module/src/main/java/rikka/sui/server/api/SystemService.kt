package rikka.sui.server.api

import android.app.ActivityManagerNative
import android.app.IActivityManager
import android.app.IProcessObserver
import android.app.IUidObserver
import android.content.pm.*
import android.os.IUserManager
import android.os.RemoteException
import android.permission.IPermissionManager
import androidx.annotation.RequiresApi
import com.android.internal.app.IAppOpsService
import rikka.sui.server.ServerConstants.LOGGER
import rikka.sui.util.BuildUtils
import java.util.*

object SystemService {

    private val activityManagerBinder by lazy {
        SystemServiceBinder<IActivityManager>("activity") {
            if (BuildUtils.atLeast26()) {
                IActivityManager.Stub.asInterface(it)
            } else {
                ActivityManagerNative.asInterface(it)
            }
        }
    }

    private val packageManagerBinder by lazy {
        SystemServiceBinder<IPackageManager>("package") {
            IPackageManager.Stub.asInterface(it)
        }
    }

    private val userManagerBinder by lazy {
        SystemServiceBinder<IUserManager>("user") {
            IUserManager.Stub.asInterface(it)
        }

    }

    private val appOpsServiceBinder by lazy {
        SystemServiceBinder<IAppOpsService>("appops") {
            IAppOpsService.Stub.asInterface(it)
        }
    }

    private val launcherAppsBinder by lazy {
        SystemServiceBinder<ILauncherApps>("launcherapps") {
            ILauncherApps.Stub.asInterface(it)
        }
    }

    @delegate:RequiresApi(30)
    private val permissionManagerBinder by lazy {
        SystemServiceBinder<IPermissionManager>("permissionmgr") {
            IPermissionManager.Stub.asInterface(it)
        }
    }

    val activityManager get() = activityManagerBinder.service
    val packageManager get() = packageManagerBinder.service
    val userManager get() = userManagerBinder.service
    val appOpsService get() = appOpsServiceBinder.service
    val launcherApps get() = launcherAppsBinder.service
    val permissionManager get() = permissionManagerBinder.service

    @JvmStatic
    @Throws(RemoteException::class)
    fun checkPermission(permission: String?, pid: Int, uid: Int): Int {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        return am.checkPermission(permission, pid, uid)
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getPackageInfo(packageName: String?, flags: Int, userId: Int): PackageInfo? {
        val pm = packageManager ?: throw RemoteException("can't get IPackageManger")
        return pm.getPackageInfo(packageName, flags, userId)
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getApplicationInfo(packageName: String?, flags: Int, userId: Int): ApplicationInfo? {
        val pm = packageManager ?: throw RemoteException("can't get IPackageManger")
        return pm.getApplicationInfo(packageName, flags, userId)
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun checkPermission(permName: String?, uid: Int): Int {
        return if (BuildUtils.atLeast30()) {
            val permmgr = permissionManager ?: throw RemoteException("can't get IPermission")
            permmgr.checkUidPermission(permName, uid)
        } else {
            val pm = packageManager ?: throw RemoteException("can't get IPackageManger")
            pm.checkUidPermission(permName, uid)
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun registerProcessObserver(processObserver: IProcessObserver?) {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        am.registerProcessObserver(processObserver)
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun registerUidObserver(observer: IUidObserver?, which: Int, cutpoint: Int, callingPackage: String?) {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        am.registerUidObserver(observer, which, cutpoint, callingPackage)
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getPackagesForUid(uid: Int): Array<String?>? {
        val pm = packageManager ?: throw RemoteException("can't get IPackageManger")
        return pm.getPackagesForUid(uid)
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getInstalledApplications(flags: Int, userId: Int): ParceledListSlice<ApplicationInfo>? {
        val pm = packageManager ?: throw RemoteException("can't get IPackageManager")
        @Suppress("UNCHECKED_CAST")
        return pm.getInstalledApplications(flags, userId) as ParceledListSlice<ApplicationInfo>?
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getInstalledPackages(flags: Int, userId: Int): ParceledListSlice<PackageInfo>? {
        val pm = packageManager ?: throw RemoteException("can't get IPackageManager")
        @Suppress("UNCHECKED_CAST")
        return pm.getInstalledPackages(flags, userId) as ParceledListSlice<PackageInfo>?
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun getUsers(excludePartial: Boolean, excludeDying: Boolean, excludePreCreated: Boolean): List<UserInfo> {
        val um = userManager ?: throw RemoteException("can't get IUserManger")
        return if (BuildUtils.atLeast30()) {
            um.getUsers(excludePartial, excludeDying, excludePreCreated)
        } else {
            try {
                um.getUsers(excludeDying)
            } catch (e: NoSuchMethodError) {
                um.getUsers(excludePartial, excludeDying, excludePreCreated)
            }
        }
    }

    @JvmStatic
    fun getInstalledPackagesNoThrow(flags: Int, userId: Int): List<PackageInfo> {
        return try {
            getInstalledPackages(flags, userId)?.list ?: emptyList()
        } catch (tr: Throwable) {
            LOGGER.w(tr, "getInstalledPackages failed: flags=%d, user=%d", flags, userId)
            emptyList()
        }
    }

    @JvmStatic
    fun getInstalledApplicationsNoThrow(flags: Int, userId: Int): List<ApplicationInfo> {
        return try {
            getInstalledApplications(flags, userId)?.list ?: emptyList()
        } catch (tr: Throwable) {
            LOGGER.w(tr, "getInstalledApplications failed: flags=%d, user=%d", flags, userId)
            emptyList()
        }
    }

    @JvmStatic
    fun getPackageInfoNoThrow(packageName: String?, flags: Int, userId: Int): PackageInfo? {
        return try {
            getPackageInfo(packageName, flags, userId)
        } catch (tr: Throwable) {
            LOGGER.w(tr, "getPackageInfo failed: packageName=%s, flags=%d, user=%d", packageName, flags, userId)
            null
        }
    }

    @JvmStatic
    fun getApplicationInfoNoThrow(packageName: String?, flags: Int, userId: Int): ApplicationInfo? {
        return try {
            getApplicationInfo(packageName, flags, userId)
        } catch (tr: Throwable) {
            LOGGER.w(tr, "getApplicationInfo failed: packageName=%s, flags=%d, user=%d", packageName, flags, userId)
            null
        }
    }

    @JvmStatic
    fun getUserIdsNoThrow(): List<Int> {
        val users = ArrayList<Int>()
        try {
            for (ui in getUsers(excludePartial = true, excludeDying = true, excludePreCreated = true)) {
                users.add(ui.id)
            }
        } catch (tr: Throwable) {
            users.clear()
            users.add(0)
        }
        return users
    }

    @JvmStatic
    fun getPackagesForUidNoThrow(uid: Int): List<String> {
        val packages = ArrayList<String>()
        try {
            packages.addAll(getPackagesForUid(uid)?.filterNotNull().orEmpty())
        } catch (tr: Throwable) {
        }
        return packages
    }

    @JvmStatic
    fun forceStopPackageNoThrow(packageName: String?, userId: Int) {
        val am = activityManager ?: throw RemoteException("can't get IActivityManager")
        try {
            am.forceStopPackage(packageName, userId)
        } catch (e: Exception) {
        }
    }

    @JvmStatic
    @Throws(RemoteException::class)
    fun addOnAppsChangedListener(callingPackage: String?, listener: IOnAppsChangedListener?) {
        val la = launcherApps ?: throw RemoteException("can't get ILauncherApps")
        if (BuildUtils.atLeast24()) {
            la.addOnAppsChangedListener(callingPackage, listener)
        } else {
            la.addOnAppsChangedListener(listener)
        }
    }
}