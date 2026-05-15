package com.hastakala.shop

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hastakala.shop.model.Product
import com.hastakala.shop.model.ProductVariant
import com.hastakala.shop.repository.ProductRepository
import com.hastakala.shop.viewmodel.InventoryResult
import com.hastakala.shop.viewmodel.InventoryViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
 * Unit tests for InventoryViewModel covering:
 * - Req 3.1: add product with valid data
 * - Req 3.2: update stock
 * - Req 3.3: delete product
 * - Req 3.5: reject empty product name
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var productRepository: ProductRepository
    private lateinit var viewModel: InventoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        productRepository = mockk()
        coEvery { productRepository.getAllProducts() } returns flowOf(emptyList())
        coEvery { productRepository.getAllVariants() } returns flowOf(emptyList())
        viewModel = InventoryViewModel(productRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Req 3.1 — add product
    // -------------------------------------------------------------------------

    @Test
    fun `addProduct with valid data inserts product and variant`() = runTest {
        coEvery { productRepository.insertProduct(any()) } returns Unit
        coEvery { productRepository.insertVariant(any()) } returns Unit

        viewModel.addProduct("Banana Bag", "Red", 10, 50.0)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.operationResult.value
        assertTrue("Expected Success but got $result", result is InventoryResult.Success)
        coVerify(exactly = 1) { productRepository.insertProduct(any()) }
        coVerify(exactly = 1) { productRepository.insertVariant(any()) }
    }

    // -------------------------------------------------------------------------
    // Req 3.5 — reject empty product name
    // -------------------------------------------------------------------------

    @Test
    fun `addProduct with blank name emits Error and does not call repository`() = runTest {
        viewModel.addProduct("   ", "Red", 10, 50.0)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.operationResult.value
        assertTrue("Expected Error but got $result", result is InventoryResult.Error)
        coVerify(exactly = 0) { productRepository.insertProduct(any()) }
    }

    @Test
    fun `addProduct with blank color emits Error`() = runTest {
        viewModel.addProduct("Banana Bag", "  ", 10, 50.0)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.operationResult.value
        assertTrue("Expected Error but got $result", result is InventoryResult.Error)
    }

    @Test
    fun `addProduct with zero unit price emits Error`() = runTest {
        viewModel.addProduct("Banana Bag", "Red", 10, 0.0)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.operationResult.value
        assertTrue("Expected Error but got $result", result is InventoryResult.Error)
    }

    @Test
    fun `addProduct with negative stock emits Error`() = runTest {
        viewModel.addProduct("Banana Bag", "Red", -1, 50.0)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.operationResult.value
        assertTrue("Expected Error but got $result", result is InventoryResult.Error)
    }

    // -------------------------------------------------------------------------
    // Req 3.2 — update stock
    // -------------------------------------------------------------------------

    @Test
    fun `updateStock calls repository with correct variantId and quantity`() = runTest {
        coEvery { productRepository.updateStock("v1", 25) } returns Unit

        viewModel.updateStock("v1", 25)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.operationResult.value
        assertTrue("Expected Success but got $result", result is InventoryResult.Success)
        coVerify(exactly = 1) { productRepository.updateStock("v1", 25) }
    }

    // -------------------------------------------------------------------------
    // Req 3.3 — delete product
    // -------------------------------------------------------------------------

    @Test
    fun `deleteProduct calls repository with correct productId`() = runTest {
        coEvery { productRepository.deleteProduct("p1") } returns Unit

        viewModel.deleteProduct("p1")
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.operationResult.value
        assertTrue("Expected Success but got $result", result is InventoryResult.Success)
        coVerify(exactly = 1) { productRepository.deleteProduct("p1") }
    }

    @Test
    fun `deleteProduct propagates repository exception as Error`() = runTest {
        coEvery { productRepository.deleteProduct(any()) } throws RuntimeException("DB error")

        viewModel.deleteProduct("p1")
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.operationResult.value
        assertTrue("Expected Error but got $result", result is InventoryResult.Error)
    }
}
