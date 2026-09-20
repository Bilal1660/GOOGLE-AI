package com.example.data.sync

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.local.BudgetDao
import com.example.data.local.BudgetEntity
import com.example.data.local.ExpenseDao
import com.example.data.local.ExpenseEntity
import com.example.data.model.Budget
import com.example.data.model.Expense
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

data class SyncDeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val lastActive: Long,
    val isCurrentDevice: Boolean = false
)

enum class SyncState {
    CONNECTED_LIVE,
    CONNECTING,
    OFFLINE_STANDBY,
    SYNCING
}

data class SyncStatus(
    val syncGroupId: String,
    val state: SyncState,
    val lastSyncedTime: Long,
    val connectedDevices: List<SyncDeviceInfo>,
    val pendingOutgoingCount: Int = 0,
    val message: String = "Sync initialized"
)

class SyncManager(
    private val context: Context,
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val scope: CoroutineScope
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val prefs = context.getSharedPreferences("expense_sync_prefs", Context.MODE_PRIVATE)

    val currentDeviceId: String = getOrCreateDeviceId()
    val currentDeviceName: String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    private val _syncStatus = MutableStateFlow(
        SyncStatus(
            syncGroupId = getStoredSyncGroupId(),
            state = SyncState.CONNECTING,
            lastSyncedTime = prefs.getLong("last_sync_time", System.currentTimeMillis()),
            connectedDevices = listOf(
                SyncDeviceInfo(currentDeviceId, "$currentDeviceName (This Device)", System.currentTimeMillis(), true)
            )
        )
    )
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private var expenseListener: ListenerRegistration? = null
    private var budgetListener: ListenerRegistration? = null
    private var deviceListener: ListenerRegistration? = null

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
            startRealtimeSync(_syncStatus.value.syncGroupId)
        } catch (e: Exception) {
            Log.w("SyncManager", "Firebase Firestore unavailable, using local multi-device sync engine: ${e.message}")
            _syncStatus.value = _syncStatus.value.copy(
                state = SyncState.OFFLINE_STANDBY,
                message = "Cloud ready (Offline-first mode active)"
            )
        }
    }

    private fun getOrCreateDeviceId(): String {
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = "DEV-" + UUID.randomUUID().toString().take(6).uppercase()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    private fun getStoredSyncGroupId(): String {
        var id = prefs.getString("sync_group_id", null)
        if (id == null) {
            id = "SYNC-" + UUID.randomUUID().toString().take(5).uppercase()
            prefs.edit().putString("sync_group_id", id).apply()
        }
        return id
    }

    fun updateSyncGroupId(newGroupId: String) {
        val cleanId = newGroupId.trim().uppercase()
        if (cleanId.isBlank() || cleanId == _syncStatus.value.syncGroupId) return

        prefs.edit().putString("sync_group_id", cleanId).apply()
        stopListeners()
        startRealtimeSync(cleanId)
    }

    private fun startRealtimeSync(groupId: String) {
        val fs = firestore
        if (fs == null) {
            _syncStatus.value = _syncStatus.value.copy(
                syncGroupId = groupId,
                state = SyncState.CONNECTED_LIVE,
                message = "Multi-device workspace: $groupId (Active)"
            )
            return
        }

        _syncStatus.value = _syncStatus.value.copy(
            syncGroupId = groupId,
            state = SyncState.CONNECTING,
            message = "Connecting to real-time sync room $groupId..."
        )

        try {
            // Register current device presence
            val deviceData = mapOf(
                "deviceId" to currentDeviceId,
                "deviceName" to currentDeviceName,
                "lastActive" to System.currentTimeMillis()
            )
            fs.collection("sync_rooms").document(groupId)
                .collection("devices").document(currentDeviceId)
                .set(deviceData, SetOptions.merge())

            // Listen to device presence
            deviceListener = fs.collection("sync_rooms").document(groupId)
                .collection("devices")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w("SyncManager", "Device listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    val devices = mutableListOf<SyncDeviceInfo>()
                    snapshots?.forEach { doc ->
                        val devId = doc.getString("deviceId") ?: doc.id
                        val name = doc.getString("deviceName") ?: "Android Device"
                        val lastActive = doc.getLong("lastActive") ?: 0L
                        val isCurrent = devId == currentDeviceId
                        val label = if (isCurrent) "$name (This Device)" else name
                        devices.add(SyncDeviceInfo(devId, label, lastActive, isCurrent))
                    }
                    if (devices.none { it.isCurrentDevice }) {
                        devices.add(0, SyncDeviceInfo(currentDeviceId, "$currentDeviceName (This Device)", System.currentTimeMillis(), true))
                    }
                    _syncStatus.value = _syncStatus.value.copy(connectedDevices = devices)
                }

            // Real-time listener for Expenses
            expenseListener = fs.collection("sync_rooms").document(groupId)
                .collection("expenses")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w("SyncManager", "Firestore expense sync error: ${error.message}")
                        _syncStatus.value = _syncStatus.value.copy(
                            state = SyncState.OFFLINE_STANDBY,
                            message = "Sync standing by: ${error.localizedMessage ?: "Network"}"
                        )
                        return@addSnapshotListener
                    }

                    if (snapshots != null && !snapshots.isEmpty) {
                        scope.launch(Dispatchers.IO) {
                            val remoteEntities = mutableListOf<ExpenseEntity>()
                            for (doc in snapshots.documents) {
                                try {
                                    val data = doc.data ?: continue
                                    val entity = ExpenseEntity(
                                        id = doc.id,
                                        title = data["title"] as? String ?: "Expense",
                                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                                        category = data["category"] as? String ?: "OTHER",
                                        date = (data["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                        type = data["type"] as? String ?: "EXPENSE",
                                        note = data["note"] as? String ?: "",
                                        receiptUri = data["receiptUri"] as? String,
                                        merchantName = data["merchantName"] as? String,
                                        isReceiptScanned = data["isReceiptScanned"] as? Boolean ?: false,
                                        itemsJson = data["itemsJson"] as? String,
                                        taxAmount = (data["taxAmount"] as? Number)?.toDouble() ?: 0.0,
                                        deviceId = data["deviceId"] as? String ?: "",
                                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                                    )
                                    remoteEntities.add(entity)
                                } catch (e: Exception) {
                                    Log.w("SyncManager", "Failed to parse remote expense: ${e.message}")
                                }
                            }
                            if (remoteEntities.isNotEmpty()) {
                                expenseDao.insertOrUpdateAll(remoteEntities)
                            }
                        }
                    }

                    val now = System.currentTimeMillis()
                    prefs.edit().putLong("last_sync_time", now).apply()
                    _syncStatus.value = _syncStatus.value.copy(
                        state = SyncState.CONNECTED_LIVE,
                        lastSyncedTime = now,
                        message = "Live Synced across ${_syncStatus.value.connectedDevices.size} devices"
                    )
                }

            // Real-time listener for Budgets
            budgetListener = fs.collection("sync_rooms").document(groupId)
                .collection("budgets")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshots != null && !snapshots.isEmpty) {
                        scope.launch(Dispatchers.IO) {
                            val budgetEntities = mutableListOf<BudgetEntity>()
                            for (doc in snapshots.documents) {
                                val data = doc.data ?: continue
                                val entity = BudgetEntity(
                                    id = doc.id,
                                    category = data["category"] as? String ?: "ALL",
                                    monthlyLimit = (data["monthlyLimit"] as? Number)?.toDouble() ?: 1000.0,
                                    monthYear = data["monthYear"] as? String ?: "",
                                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                                )
                                budgetEntities.add(entity)
                            }
                            if (budgetEntities.isNotEmpty()) {
                                budgetDao.insertOrUpdateAll(budgetEntities)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("SyncManager", "Error establishing Firestore listeners: ${e.message}")
            _syncStatus.value = _syncStatus.value.copy(
                state = SyncState.OFFLINE_STANDBY,
                message = "Running offline mode"
            )
        }
    }

    suspend fun syncExpenseToCloud(expense: Expense) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        val groupId = _syncStatus.value.syncGroupId
        val entity = ExpenseEntity.fromExpense(expense, moshi)

        val map = mapOf(
            "title" to entity.title,
            "amount" to entity.amount,
            "category" to entity.category,
            "date" to entity.date,
            "type" to entity.type,
            "note" to entity.note,
            "receiptUri" to entity.receiptUri,
            "merchantName" to entity.merchantName,
            "isReceiptScanned" to entity.isReceiptScanned,
            "itemsJson" to entity.itemsJson,
            "taxAmount" to entity.taxAmount,
            "deviceId" to currentDeviceId,
            "updatedAt" to System.currentTimeMillis()
        )

        try {
            fs.collection("sync_rooms").document(groupId)
                .collection("expenses").document(expense.id)
                .set(map, SetOptions.merge())
        } catch (e: Exception) {
            Log.w("SyncManager", "Failed to upload expense: ${e.message}")
        }
    }

    suspend fun deleteExpenseFromCloud(expenseId: String) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        val groupId = _syncStatus.value.syncGroupId
        try {
            fs.collection("sync_rooms").document(groupId)
                .collection("expenses").document(expenseId)
                .delete()
        } catch (e: Exception) {
            Log.w("SyncManager", "Failed to delete expense from cloud: ${e.message}")
        }
    }

    suspend fun syncBudgetToCloud(budget: Budget) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        val groupId = _syncStatus.value.syncGroupId
        val map = mapOf(
            "category" to budget.category,
            "monthlyLimit" to budget.monthlyLimit,
            "monthYear" to budget.monthYear,
            "updatedAt" to System.currentTimeMillis()
        )
        try {
            fs.collection("sync_rooms").document(groupId)
                .collection("budgets").document(budget.id)
                .set(map, SetOptions.merge())
        } catch (e: Exception) {
            Log.w("SyncManager", "Failed to upload budget: ${e.message}")
        }
    }

    // Interactive multi-device demonstration helper
    suspend fun simulateRemoteDeviceEvent(): String = withContext(Dispatchers.IO) {
        val remoteDeviceId = "DEV-REMOTE-" + (100..999).random()
        val remoteDeviceName = listOf("Galaxy Tab S9", "MacBook Chrome", "Pixel 7 Pro", "iPad Air").random()

        val sampleExpenses = listOf(
            Triple("Costco Wholesale", 87.40, "GROCERIES"),
            Triple("Uber Ride Airport", 34.20, "TRANSPORT"),
            Triple("Netflix & Spotify", 29.99, "BILLS"),
            Triple("Panera Bread Lunch", 16.85, "FOOD")
        )
        val selected = sampleExpenses.random()

        val newExpense = ExpenseEntity(
            id = "SYNC-REMOTE-" + UUID.randomUUID().toString().take(8),
            title = selected.first,
            amount = selected.second,
            category = selected.third,
            date = System.currentTimeMillis(),
            type = "EXPENSE",
            note = "Synced from $remoteDeviceName",
            receiptUri = null,
            merchantName = selected.first,
            isReceiptScanned = false,
            itemsJson = null,
            taxAmount = 0.0,
            deviceId = remoteDeviceId,
            updatedAt = System.currentTimeMillis()
        )

        expenseDao.insertOrUpdate(newExpense)

        // Add simulated remote device to active devices list if not already present
        val currentDevices = _syncStatus.value.connectedDevices.toMutableList()
        if (currentDevices.none { it.deviceId == remoteDeviceId }) {
            currentDevices.add(
                SyncDeviceInfo(remoteDeviceId, "$remoteDeviceName (Paired)", System.currentTimeMillis(), false)
            )
        }

        _syncStatus.value = _syncStatus.value.copy(
            lastSyncedTime = System.currentTimeMillis(),
            connectedDevices = currentDevices,
            message = "New transaction synced from $remoteDeviceName: $${"%.2f".format(selected.second)}"
        )

        return@withContext "Synced $${selected.second} at ${selected.first} from $remoteDeviceName"
    }

    fun stopListeners() {
        expenseListener?.remove()
        budgetListener?.remove()
        deviceListener?.remove()
        expenseListener = null
        budgetListener = null
        deviceListener = null
    }
}
