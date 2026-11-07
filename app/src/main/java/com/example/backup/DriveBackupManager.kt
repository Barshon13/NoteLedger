package com.example.backup

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

data class BackupPayload(
    val notes: List<Note>,
    val expenses: List<Expense>,
    val userBalance: UserBalanceEntity?,
    val monthlyBudgets: List<MonthlyBudget>,
    val recurringExpenses: List<RecurringExpense>,
    val debtRecords: List<DebtRecord>
)

class DriveBackupManager(private val context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val adapter = moshi.adapter(BackupPayload::class.java)

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("NoteLedger")
            .build()
    }

    suspend fun backupData(account: GoogleSignInAccount): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            
            // Gather all data
            // Note: In a real app, you'd collect these from Flows or suspend functions in DAOs
            // For brevity and since it's a one-shot backup, we assume synchronous/blocking DAO calls are okay here if wrapped in withContext(IO)
            // But Room DAOs usually return Flow or are suspend.
            
            // We need to fetch data from all DAOs
            val notes = database.noteDao().getAllNotesDirect()
            val expenses = database.expenseDao().getAllExpensesDirect()
            val balance = database.userBalanceDao().getBalanceDirect()
            val budgets = database.budgetDao().getAllBudgetsDirect()
            val recurring = database.recurringExpenseDao().getAllRecurringExpensesDirect()
            val debts = database.debtRecordDao().getAllDebtRecordsDirect()

            val payload = BackupPayload(
                notes = notes,
                expenses = expenses,
                userBalance = balance,
                monthlyBudgets = budgets,
                recurringExpenses = recurring,
                debtRecords = debts
            )

            val json = adapter.toJson(payload)
            val driveService = getDriveService(account)

            // Find existing backup file in appDataFolder
            val files = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = 'noteledger_backup.json'")
                .execute()

            val fileMetadata = File()
            fileMetadata.name = "noteledger_backup.json"
            fileMetadata.parents = Collections.singletonList("appDataFolder")

            val contentStream = com.google.api.client.http.ByteArrayContent.fromString("application/json", json)

            if (files.files.isEmpty()) {
                driveService.files().create(fileMetadata, contentStream).execute()
            } else {
                val fileId = files.files[0].id
                driveService.files().update(fileId, null, contentStream).execute()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DriveBackupManager", "Backup failed", e)
            Result.failure(e)
        }
    }

    suspend fun restoreData(account: GoogleSignInAccount): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(account)
            val files = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = 'noteledger_backup.json'")
                .execute()

            if (files.files.isEmpty()) {
                return@withContext Result.failure(Exception("No backup found on Google Drive"))
            }

            val fileId = files.files[0].id
            val inputStream = driveService.files().get(fileId).executeMediaAsInputStream()
            val json = inputStream.bufferedReader().use { it.readText() }

            val payload = adapter.fromJson(json) ?: return@withContext Result.failure(Exception("Failed to parse backup"))

            val database = AppDatabase.getDatabase(context)
            
            withContext(Dispatchers.IO) {
                database.runInTransaction {
                    // We need to run these inside the transaction
                    // Since Room doesn't support calling suspend functions directly in runInTransaction without a CoroutineScope, 
                    // and we are already in withContext(IO), we can just use blocking-like calls if we had them or use runBlocking for small operations if needed.
                    // However, it's better to just call the DAOs sequentially.
                }
                
                // Clear existing
                database.noteDao().deleteAllNotes()
                database.expenseDao().deleteAllExpenses()
                database.userBalanceDao().deleteAllBalance()
                database.budgetDao().deleteAllBudgets()
                database.recurringExpenseDao().deleteAllRecurringExpenses()
                database.debtRecordDao().deleteAllDebtRecords()

                // Insert restored
                database.noteDao().insertNotes(payload.notes)
                database.expenseDao().insertExpenses(payload.expenses)
                payload.userBalance?.let { database.userBalanceDao().updateBalance(it) }
                payload.monthlyBudgets.forEach { database.budgetDao().insertOrUpdateBudget(it) }
                database.recurringExpenseDao().insertRecurringExpenses(payload.recurringExpenses)
                database.debtRecordDao().insertDebtRecords(payload.debtRecords)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DriveBackupManager", "Restore failed", e)
            Result.failure(e)
        }
    }
}
