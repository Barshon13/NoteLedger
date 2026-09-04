package com.example.data.repository

import com.example.data.local.BudgetDao
import com.example.data.local.DebtRecordDao
import com.example.data.local.ExpenseDao
import com.example.data.local.NoteDao
import com.example.data.local.RecurringExpenseDao
import com.example.data.model.DebtRecord
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.data.model.Note
import com.example.data.model.RecurringExpense
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

class TrackerRepository(
    private val noteDao: NoteDao,
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val recurringExpenseDao: RecurringExpenseDao,
    private val debtRecordDao: DebtRecordDao
) {
    // Notes operations
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> {
        return if (query.isBlank()) {
            noteDao.getAllNotes()
        } else {
            noteDao.searchNotes(query.trim())
        }
    }

    suspend fun getNoteById(id: String): Note? = noteDao.getNoteById(id)

    suspend fun insertNote(note: Note) = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun deleteNoteById(id: String) = noteDao.deleteNoteById(id)

    // Expenses operations
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesByDate(date: String): Flow<List<Expense>> = expenseDao.getExpensesByDate(date)

    fun getExpensesByMonth(yearMonth: String): Flow<List<Expense>> = expenseDao.getExpensesByMonth(yearMonth)

    fun getDailyTotal(date: String): Flow<Double> = expenseDao.getDailyTotal(date)

    fun getMonthlyTotal(yearMonth: String): Flow<Double> = expenseDao.getMonthlyTotal(yearMonth)

    fun getNotesCount(): Flow<Int> = noteDao.getNotesCount()

    fun getExpensesCount(): Flow<Int> = expenseDao.getExpensesCount()

    fun getAllTimeExpenseTotal(): Flow<Double> = expenseDao.getAllTimeTotal()

    suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun deleteExpenseById(id: String) = expenseDao.deleteExpenseById(id)

    // Budget operations
    fun getBudgetForMonth(monthYear: String): Flow<MonthlyBudget?> = budgetDao.getBudgetForMonth(monthYear)

    fun getAllBudgets(): Flow<List<MonthlyBudget>> = budgetDao.getAllBudgets()

    suspend fun saveBudget(budget: MonthlyBudget) = budgetDao.insertOrUpdateBudget(budget)

    suspend fun deleteBudget(monthYear: String) = budgetDao.deleteBudget(monthYear)

    // Recurring Expenses operations
    fun getAllRecurringExpenses(): Flow<List<RecurringExpense>> = recurringExpenseDao.getAllRecurringExpenses()

    fun getActiveRecurringExpenses(): Flow<List<RecurringExpense>> = recurringExpenseDao.getActiveRecurringExpenses()

    suspend fun insertRecurringExpense(expense: RecurringExpense) = recurringExpenseDao.insertRecurringExpense(expense)

    suspend fun updateRecurringExpense(expense: RecurringExpense) = recurringExpenseDao.updateRecurringExpense(expense)

    suspend fun deleteRecurringExpense(expense: RecurringExpense) = recurringExpenseDao.deleteRecurringExpense(expense)

    suspend fun deleteRecurringExpenseById(id: String) = recurringExpenseDao.deleteRecurringExpenseById(id)

    // Debt Records operations
    fun getAllDebtRecords(): Flow<List<DebtRecord>> = debtRecordDao.getAllDebtRecords()

    suspend fun insertDebtRecord(debt: DebtRecord) = debtRecordDao.insertDebtRecord(debt)

    suspend fun updateDebtRecord(debt: DebtRecord) = debtRecordDao.updateDebtRecord(debt)

    suspend fun deleteDebtRecord(debt: DebtRecord) = debtRecordDao.deleteDebtRecord(debt)

    suspend fun deleteDebtRecordById(id: String) = debtRecordDao.deleteDebtRecordById(id)

    // Global clear
    suspend fun clearAllData() {
        noteDao.deleteAllNotes()
        expenseDao.deleteAllExpenses()
        budgetDao.deleteAllBudgets()
        recurringExpenseDao.deleteAllRecurringExpenses()
        debtRecordDao.deleteAllDebtRecords()
    }

    // JSON Export according to PRD specification
    suspend fun exportDataAsJson(): String {
        val notes = noteDao.getAllNotesDirect()
        val expenses = expenseDao.getAllExpensesDirect()
        val budgets = budgetDao.getAllBudgetsDirect()
        val recurring = recurringExpenseDao.getAllRecurringExpensesDirect()
        val debts = debtRecordDao.getAllDebtRecordsDirect()

        val root = JSONObject()
        root.put("version", 2)

        val notesArray = JSONArray()
        for (n in notes) {
            val noteObj = JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
                put("createdAt", n.createdAt)
                put("updatedAt", n.updatedAt)
            }
            notesArray.put(noteObj)
        }
        root.put("notes", notesArray)

        val expensesArray = JSONArray()
        for (e in expenses) {
            val expObj = JSONObject().apply {
                put("id", e.id)
                put("amount", e.amount)
                put("category", e.category)
                put("note", e.note)
                put("date", e.date)
                put("createdAt", e.createdAt)
            }
            expensesArray.put(expObj)
        }
        root.put("expenses", expensesArray)

        val budgetsArray = JSONArray()
        for (b in budgets) {
            val bObj = JSONObject().apply {
                put("monthYear", b.monthYear)
                put("totalBudget", b.totalBudget)
                put("foodBudget", b.foodBudget)
                put("transportBudget", b.transportBudget)
                put("shoppingBudget", b.shoppingBudget)
                put("billsBudget", b.billsBudget)
                put("otherBudget", b.otherBudget)
            }
            budgetsArray.put(bObj)
        }
        root.put("budgets", budgetsArray)

        val recurringArray = JSONArray()
        for (r in recurring) {
            val rObj = JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("amount", r.amount)
                put("category", r.category)
                put("frequency", r.frequency)
                put("nextDueDate", r.nextDueDate)
                put("isActive", r.isActive)
                put("note", r.note)
                put("createdAt", r.createdAt)
            }
            recurringArray.put(rObj)
        }
        root.put("recurringExpenses", recurringArray)

        val debtsArray = JSONArray()
        for (d in debts) {
            val dObj = JSONObject().apply {
                put("id", d.id)
                put("personName", d.personName)
                put("amount", d.amount)
                put("description", d.description)
                put("isOwedToMe", d.isOwedToMe)
                put("isSettled", d.isSettled)
                put("date", d.date)
                put("createdAt", d.createdAt)
            }
            debtsArray.put(dObj)
        }
        root.put("debts", debtsArray)

        return root.toString(2)
    }

    // CSV Export for Expenses
    suspend fun exportExpensesAsCsv(): String {
        val expenses = expenseDao.getAllExpensesDirect()
        val sb = StringBuilder()
        sb.append("Date,Category,Amount,Note,ID,CreatedAt\n")
        for (e in expenses) {
            val date = escapeCsv(e.date)
            val category = escapeCsv(e.category)
            val amount = String.format(Locale.US, "%.2f", e.amount)
            val note = escapeCsv(e.note)
            val id = escapeCsv(e.id)
            val createdAt = escapeCsv(e.createdAt)
            sb.append("$date,$category,$amount,$note,$id,$createdAt\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    // JSON Import with validation
    sealed class ImportResult {
        data class Success(
            val notesImported: Int,
            val expensesImported: Int,
            val budgetsImported: Int,
            val recurringImported: Int,
            val debtsImported: Int
        ) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    suspend fun importDataFromJson(jsonString: String, replaceExisting: Boolean = true): ImportResult {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("notes") && !root.has("expenses") && !root.has("budgets") && !root.has("recurringExpenses") && !root.has("debts")) {
                return ImportResult.Error("Invalid backup file: missing data sections.")
            }

            val importedNotes = mutableListOf<Note>()
            if (root.has("notes")) {
                val notesArray = root.getJSONArray("notes")
                for (i in 0 until notesArray.length()) {
                    val item = notesArray.getJSONObject(i)
                    val id = if (item.has("id") && item.getString("id").isNotBlank()) {
                        item.getString("id")
                    } else {
                        UUID.randomUUID().toString()
                    }
                    val title = item.optString("title", "")
                    val content = item.optString("content", "")
                    val createdAt = item.optString("createdAt", Instant.now().toString())
                    val updatedAt = item.optString("updatedAt", Instant.now().toString())
                    importedNotes.add(
                        Note(
                            id = id,
                            title = title,
                            content = content,
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                    )
                }
            }

            val importedExpenses = mutableListOf<Expense>()
            if (root.has("expenses")) {
                val expensesArray = root.getJSONArray("expenses")
                for (i in 0 until expensesArray.length()) {
                    val item = expensesArray.getJSONObject(i)
                    val id = if (item.has("id") && item.getString("id").isNotBlank()) {
                        item.getString("id")
                    } else {
                        UUID.randomUUID().toString()
                    }
                    val amount = item.optDouble("amount", 0.0)
                    val category = item.optString("category", ExpenseCategory.OTHER.displayName)
                    val note = item.optString("note", "")
                    val date = item.optString("date", LocalDate.now().toString())
                    val createdAt = item.optString("createdAt", Instant.now().toString())
                    importedExpenses.add(
                        Expense(
                            id = id,
                            amount = amount,
                            category = category,
                            note = note,
                            date = date,
                            createdAt = createdAt
                        )
                    )
                }
            }

            val importedBudgets = mutableListOf<MonthlyBudget>()
            if (root.has("budgets")) {
                val bArray = root.getJSONArray("budgets")
                for (i in 0 until bArray.length()) {
                    val item = bArray.getJSONObject(i)
                    val monthYear = item.optString("monthYear", LocalDate.now().toString().substring(0, 7))
                    val total = item.optDouble("totalBudget", 0.0)
                    val food = item.optDouble("foodBudget", 0.0)
                    val transport = item.optDouble("transportBudget", 0.0)
                    val shopping = item.optDouble("shoppingBudget", 0.0)
                    val bills = item.optDouble("billsBudget", 0.0)
                    val other = item.optDouble("otherBudget", 0.0)
                    importedBudgets.add(
                        MonthlyBudget(
                            monthYear = monthYear,
                            totalBudget = total,
                            foodBudget = food,
                            transportBudget = transport,
                            shoppingBudget = shopping,
                            billsBudget = bills,
                            otherBudget = other
                        )
                    )
                }
            }

            val importedRecurring = mutableListOf<RecurringExpense>()
            if (root.has("recurringExpenses")) {
                val rArray = root.getJSONArray("recurringExpenses")
                for (i in 0 until rArray.length()) {
                    val item = rArray.getJSONObject(i)
                    val id = if (item.has("id") && item.getString("id").isNotBlank()) item.getString("id") else UUID.randomUUID().toString()
                    val title = item.optString("title", "")
                    val amount = item.optDouble("amount", 0.0)
                    val category = item.optString("category", ExpenseCategory.BILLS.displayName)
                    val frequency = item.optString("frequency", "Monthly")
                    val nextDueDate = item.optString("nextDueDate", LocalDate.now().toString())
                    val isActive = item.optBoolean("isActive", true)
                    val note = item.optString("note", "")
                    val createdAt = item.optString("createdAt", Instant.now().toString())
                    importedRecurring.add(
                        RecurringExpense(
                            id = id,
                            title = title,
                            amount = amount,
                            category = category,
                            frequency = frequency,
                            nextDueDate = nextDueDate,
                            isActive = isActive,
                            note = note,
                            createdAt = createdAt
                        )
                    )
                }
            }

            val importedDebts = mutableListOf<DebtRecord>()
            if (root.has("debts")) {
                val dArray = root.getJSONArray("debts")
                for (i in 0 until dArray.length()) {
                    val item = dArray.getJSONObject(i)
                    val id = if (item.has("id") && item.getString("id").isNotBlank()) item.getString("id") else UUID.randomUUID().toString()
                    val personName = item.optString("personName", "")
                    val amount = item.optDouble("amount", 0.0)
                    val description = item.optString("description", "")
                    val isOwedToMe = item.optBoolean("isOwedToMe", true)
                    val isSettled = item.optBoolean("isSettled", false)
                    val date = item.optString("date", LocalDate.now().toString())
                    val createdAt = item.optString("createdAt", Instant.now().toString())
                    importedDebts.add(
                        DebtRecord(
                            id = id,
                            personName = personName,
                            amount = amount,
                            description = description,
                            isOwedToMe = isOwedToMe,
                            isSettled = isSettled,
                            date = date,
                            createdAt = createdAt
                        )
                    )
                }
            }

            if (replaceExisting) {
                noteDao.deleteAllNotes()
                expenseDao.deleteAllExpenses()
                budgetDao.deleteAllBudgets()
                recurringExpenseDao.deleteAllRecurringExpenses()
                debtRecordDao.deleteAllDebtRecords()
            }

            if (importedNotes.isNotEmpty()) {
                noteDao.insertNotes(importedNotes)
            }
            if (importedExpenses.isNotEmpty()) {
                expenseDao.insertExpenses(importedExpenses)
            }
            for (b in importedBudgets) {
                budgetDao.insertOrUpdateBudget(b)
            }
            if (importedRecurring.isNotEmpty()) {
                recurringExpenseDao.insertRecurringExpenses(importedRecurring)
            }
            if (importedDebts.isNotEmpty()) {
                debtRecordDao.insertDebtRecords(importedDebts)
            }

            ImportResult.Success(
                notesImported = importedNotes.size,
                expensesImported = importedExpenses.size,
                budgetsImported = importedBudgets.size,
                recurringImported = importedRecurring.size,
                debtsImported = importedDebts.size
            )
        } catch (e: Exception) {
            ImportResult.Error("Failed to parse JSON backup: ${e.localizedMessage ?: "Unknown format error"}")
        }
    }
}
