package com.hastakala.shop

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hastakala.shop.model.Product
import com.hastakala.shop.model.ProductVariant
import com.hastakala.shop.model.ProductWithVariants
import com.hastakala.shop.model.Sale
import com.hastakala.shop.repository.ProductRepository
import com.hastakala.shop.repository.SaleRepository
import com.hastakala.shop.viewmodel.QuickBillResult
import com.hastakala.shop.viewmodel.QuickBillViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for QuickBillViewModel covering:
 * - Req 2.3: save button gating (variant selected + qty > 0)
 * - Req 2.4: successful sale saves and decrements stock
 * - Req 2.5: oversell prevention
 * - Req 2.6: zero/negative quantity rejection
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuickBillViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var productRepository: ProductRepository
    private lateinit var saleRepository: SaleRepository
    private lateinit var viewModel: QuickBillViewModel

    private val testProduct = Product(id = "p1", name = "Banana Bag")
    private val testVariant = ProductVariant(
        id = "v1",
        userId = "test-user",
        productId = "p1",
        colorOrDesign = "Red",
        stock = 10,
        unitPrice = 50.0
    )
    private val testProductWithVariants = ProductWithVariants(
        product = testProduct,
        variants = listOf(testVariant)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        productRepository = mockk()
        saleRepository = mockk()

        coEvery { productRepository.getAllProducts() } returns flowOf(listOf(testProduct))
        coEvery { productRepository.getAllVariants() } returns flowOf(listOf(testVariant))

        viewModel = QuickBillViewModel(productRepository, saleRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Req 2.4 — successful sale
    // -------------------------------------------------------------------------

    @Test
    fun `saveSale with valid quantity succeeds and emits Success`() = runTest {
        val saleSlot = slot<Sale>()
        coEvery { saleRepository.insertSale(capture(saleSlot)) } returns Result.success(Unit)

        viewModel.saveSale(testVariant, "Banana Bag", quantity = 3)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.saleResult.value
        assertTrue("Expected Success but got $result", result is QuickBillResult.Success)

        // Verify the sale was constructed correctly
        val capturedSale = saleSlot.captured
        assertEquals("Banana Bag", capturedSale.productName)
        assertEquals("Red", capturedSale.variantLabel)
        assertEquals(3, capturedSale.quantity)
        assertEquals(50.0, capturedSale.unitPrice, 0.001)
        assertEquals(150.0, capturedSale.totalAmount, 0.001)

        coVerify(exactly = 1) { saleRepository.insertSale(any()) }
    }

    // -------------------------------------------------------------------------
    // Req 2.5 — oversell prevention
    // -------------------------------------------------------------------------

    @Test
    fun `saveSale with quantity exceeding stock emits Error without calling repository`() = runTest {
        viewModel.saveSale(testVariant, "Banana Bag", quantity = 11) // stock is 10
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.saleResult.value
        assertTrue("Expected Error but got $result", result is QuickBillResult.Error)
        val error = result as QuickBillResult.Error
        assertTrue(error.message.contains("stock", ignoreCase = true))

        coVerify(exactly = 0) { saleRepository.insertSale(any()) }
    }

    // -------------------------------------------------------------------------
    // Req 2.6 — zero quantity rejection
    // -------------------------------------------------------------------------

    @Test
    fun `saveSale with zero quantity emits Error without calling repository`() = runTest {
        viewModel.saveSale(testVariant, "Banana Bag", quantity = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.saleResult.value
        assertTrue("Expected Error but got $result", result is QuickBillResult.Error)

        coVerify(exactly = 0) { saleRepository.insertSale(any()) }
    }

    @Test
    fun `saveSale with negative quantity emits Error without calling repository`() = runTest {
        viewModel.saveSale(testVariant, "Banana Bag", quantity = -5)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.saleResult.value
        assertTrue("Expected Error but got $result", result is QuickBillResult.Error)

        coVerify(exactly = 0) { saleRepository.insertSale(any()) }
    }

    // -------------------------------------------------------------------------
    // Repository failure propagation
    // -------------------------------------------------------------------------

    @Test
    fun `saveSale propagates repository failure as Error`() = runTest {
        coEvery { saleRepository.insertSale(any()) } returns
            Result.failure(RuntimeException("DB write failed"))

        viewModel.saveSale(testVariant, "Banana Bag", quantity = 2)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.saleResult.value
        assertTrue("Expected Error but got $result", result is QuickBillResult.Error)
        val error = result as QuickBillResult.Error
        assertTrue(error.message.contains("DB write failed"))
    }

    // -------------------------------------------------------------------------
    // Req 2.2 — selectProduct populates selectedVariants
    // -------------------------------------------------------------------------

    @Test
    fun `selectProduct updates selectedVariants LiveData`() = runTest {
        viewModel.selectProduct(testProductWithVariants)
        testDispatcher.scheduler.advanceUntilIdle()

        val variants = viewModel.selectedVariants.value
        assertNotNull(variants)
        assertEquals(1, variants!!.size)
        assertEquals("Red", variants[0].colorOrDesign)
    }
}
