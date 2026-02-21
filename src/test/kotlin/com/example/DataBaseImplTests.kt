package com.example

import com.example.data.database.DataBaseFacade
import com.example.data.database.DataBaseImpl
import com.example.data.database.DatabaseFactory
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DataBaseImplTests {

    @Test
    fun `CRUD works`() = runBlocking {
        DatabaseFactory.init()
        val db: DataBaseFacade = DataBaseImpl()

        db.deleteAllTransactions()
        assertEquals(0, db.allTransactions().size)

        val created = db.addTransaction(
            symbol = "AAPL", side = "BUY", quantity = "1.5", notional = "3000"
        )
        assertTrue(created.id > 0)
        assertEquals("AAPL", created.symbol)
        assertEquals("BUY", created.side)
        assertEquals("1.5", created.quantity)
        assertEquals("3000", created.notional)

        val fetched = db.getTransaction(created.id)
        assertNotNull(fetched)
        assertEquals(created.id, fetched.id)

        assertTrue(db.doesTransactionExist(created.id))

        assertEquals(1, db.allTransactions().size)

        assertTrue(db.deleteTransaction(created.id))
        assertFalse(db.doesTransactionExist(created.id))
        assertEquals(0, db.allTransactions().size)
    }

    @Test
    fun `addTransaction accepts blank quantity and notional and persists as zero`() = runBlocking {
        DatabaseFactory.init()
        val db: DataBaseFacade = DataBaseImpl()

        db.deleteAllTransactions()
        val created = db.addTransaction(symbol = "AAPL", side = "SELL", quantity = "", notional = "")
        assertEquals("", created.quantity)
        assertEquals("", created.notional)

        val fetched = db.getTransaction(created.id)
        assertNotNull(fetched)
        assertEquals("0.00", fetched.quantity)
        assertEquals("0.00", fetched.notional)
    }

    @Test
    fun `addTransaction accepts null quantity`() = runBlocking {
        DatabaseFactory.init()
        val db: DataBaseFacade = DataBaseImpl()

        db.deleteAllTransactions()
        val created = db.addTransaction(symbol = "AAPL", side = "SELL", quantity = "", notional = "")
        assertEquals("", created.quantity)
        assertEquals("", created.notional)
    }

    @Test
    fun `addTransaction trims whitespace`() = runBlocking {
        DatabaseFactory.init()
        val db: DataBaseFacade = DataBaseImpl()

        db.deleteAllTransactions()
        val created = db.addTransaction(
            symbol = "AAPL",
            side = "BUY",
            quantity = "  2.50  ",
            notional = "   1000   "
        )

        val fetched = db.getTransaction(created.id)
        assertNotNull(fetched)
        assertEquals("2.50", fetched.quantity)
        assertEquals("1000.00", fetched.notional)
    }

    @Test
    fun `getTransaction returns null for missing id`() = runBlocking {
        DatabaseFactory.init()
        val db = DataBaseImpl()

        db.deleteAllTransactions()
        assertEquals(null, db.getTransaction(999_999))
    }

    @Test
    fun `deleteTransaction returns false for missing id`() = runBlocking {
        DatabaseFactory.init()
        val db: DataBaseFacade = DataBaseImpl()

        db.deleteAllTransactions()
        assertFalse(db.deleteTransaction(999_999))
    }

    @Test
    fun `addTransaction accepts comma decimal and persists normalized`() = runBlocking {
        DatabaseFactory.init()
        val db: DataBaseFacade = DataBaseImpl()

        db.deleteAllTransactions()
        val created = db.addTransaction(
            symbol = "AAPL",
            side = "BUY",
            quantity = "1,5",
            notional = "3000,25"
        )

        val fetched = db.getTransaction(created.id)
        assertNotNull(fetched)
        assertEquals("1.50", fetched.quantity)
        assertEquals("3000.25", fetched.notional)
    }

    @Test
    fun `allTransactions returns rows ordered by id`() = runBlocking {
        DatabaseFactory.init()
        val db: DataBaseFacade = DataBaseImpl()

        db.deleteAllTransactions()
        val a = db.addTransaction(symbol = "AAPL", side = "BUY", quantity = "1", notional = "10")
        val b = db.addTransaction(symbol = "MSFT", side = "SELL", quantity = "2", notional = "20")
        val c = db.addTransaction(symbol = "TSLA", side = "BUY", quantity = "3", notional = "30")

        val all = db.allTransactions()
        assertEquals(3, all.size)
        assertEquals(listOf(a.id, b.id, c.id), all.map { it.id })
    }
}

