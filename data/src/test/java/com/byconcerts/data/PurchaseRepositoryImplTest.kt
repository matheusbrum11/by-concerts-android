package com.byconcerts.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.byconcerts.core.common.DispatcherProvider
import com.byconcerts.data.local.db.AppDatabase
import com.byconcerts.data.repository.PurchaseRepositoryImpl
import com.byconcerts.domain.model.PaymentCode
import com.byconcerts.domain.model.PaymentInfo
import com.byconcerts.domain.model.Purchase
import com.byconcerts.domain.model.PurchaseStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PurchaseRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PurchaseRepositoryImpl

    private val testDispatchers = object : DispatcherProvider {
        override val main = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
    }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = PurchaseRepositoryImpl(db.purchaseDao(), testDispatchers)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `chave de idempotencia duplicada nao cria segunda compra`() = runTest {
        val first = repository.createPendingIfAbsent(pending(id = "p1", key = "same-key"))
        val second = repository.createPendingIfAbsent(pending(id = "p2", key = "same-key"))

        assertThat(first.id).isEqualTo("p1")
        assertThat(second.id).isEqualTo("p1")
        assertThat(repository.getById("p2")).isNull()
    }

    @Test
    fun `update persiste status APPROVED e dados de pagamento`() = runTest {
        repository.createPendingIfAbsent(pending(id = "p1", key = "k1"))

        val approved = repository.getById("p1")!!.copy(
            status = PurchaseStatus.APPROVED,
            payment = PaymentInfo("A1", "N1", "VISA", "**** 9", 10000),
        )
        repository.update(approved)

        val reloaded = repository.getById("p1")!!
        assertThat(reloaded.status).isEqualTo(PurchaseStatus.APPROVED)
        assertThat(reloaded.payment?.authCode).isEqualTo("A1")
        assertThat(reloaded.payment?.cieloCode).isEqualTo("N1")
    }

    private fun pending(id: String, key: String): Purchase = Purchase(
        id = id,
        idempotencyKey = key,
        eventId = "evt-1",
        eventTitle = "Show",
        quantity = 2,
        unitPriceInCents = 5000,
        totalInCents = 10000,
        status = PurchaseStatus.PENDING,
        paymentCode = PaymentCode.CREDITO_AVISTA,
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )
}
